package me.aecsocket.calibre.system.gun.reload.external;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.system.builtin.CapacityComponentContainerSystem;
import me.aecsocket.calibre.system.builtin.ComponentAccessorSystem;
import me.aecsocket.calibre.system.builtin.SchedulerSystem;
import me.aecsocket.calibre.system.gun.GunSystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.slot.ItemSlot;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SingleChamberReloadSystem extends AbstractSystem implements ExternalReloadSystem {
    public static final String ID = "single_chamber_reload";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("reload_delay", NumberDescriptorStat.of(0L))
            .init("reload_after", NumberDescriptorStat.of(0L))

            .init("reload_single_delay", NumberDescriptorStat.of(0L))
            .init("reload_single_after", NumberDescriptorStat.of(0L))
            .get();

    protected transient CapacityComponentContainerSystem container;
    protected transient ComponentAccessorSystem accessor;

    /**
     * Used for registration + deserialization.
     */
    public SingleChamberReloadSystem() { super(0); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public SingleChamberReloadSystem(SingleChamberReloadSystem o) {
        super(o);
    }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    public CapacityComponentContainerSystem container() { return container; }
    public ComponentAccessorSystem collector() { return accessor; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(CapacityComponentContainerSystem.class);
        require(ComponentAccessorSystem.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        container = require(CapacityComponentContainerSystem.class);
        accessor = require(ComponentAccessorSystem.class);
    }

    protected <I extends Item> void reloadSingle(ItemUser user, ItemSlot<I> slot, TickContext tickContext, SchedulerSystem scheduler, GunSystem.Events.ExternalReload<I> event) {
        int remaining = container.remaining();
        if (remaining <= 0) {
            finishReload(user, slot, tickContext, event);
            return;
        }
        long singleAfter = tree().<NumberDescriptor.Long>stat("reload_single_after").apply();
        int amount = singleAfter == 0
                ? remaining
                : singleAfter < tickContext.delta() ? (int) (tickContext.delta() / singleAfter) : 1;
        for (int i = 0; i < amount; i++) {
            ComponentAccessorSystem.Result result = collectChamber(user);
            if (result == null)
                finishReload(user, slot, tickContext, event);
            else {
                container.push(result.component());
                result.removeItem();
                scheduler.delay(tree().<NumberDescriptor.Long>stat("reload_single_delay").apply());
                scheduler.<SingleChamberReloadSystem, I>schedule(this, singleAfter,
                        (self, equip, ctx) -> self.reloadSingle(equip.user(), equip.slot(), equip.tickContext(), ctx.get(event.system().scheduler()), event));
                update(user, slot, event);
            }
        }
    }

    protected ComponentAccessorSystem.Result collectChamber(ItemUser user) {
        return accessor.collectComponent(user, component ->
                GunSystem.getProjectile(component).c() != null && container.accepts(component), null);
    }

    protected <I extends Item> void finishReload(ItemUser user, ItemSlot<I> slot, TickContext tickContext, GunSystem.Events.ExternalReload<I> event) {}

    @Override
    public <I extends Item> void reload(GunSystem.Events.ExternalReload<I> event) {
        if (container.remaining() <= 0 || collectChamber(event.user()) == null) {
            event.result(ItemEvents.Result.FAILURE);
            return;
        }
        SchedulerSystem scheduler = event.system().scheduler();
        scheduler.delay(tree().<NumberDescriptor.Long>stat("reload_delay").apply());
        scheduler.<SingleChamberReloadSystem, I>schedule(this, tree().<NumberDescriptor.Long>stat("reload_after").apply(),
                (self, equip, ctx) -> self.reloadSingle(equip.user(), equip.slot(), equip.tickContext(), ctx.get(scheduler), event));
        update(event);
        event.result(ItemEvents.Result.SUCCESS);
    }

    @Override public abstract SingleChamberReloadSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
