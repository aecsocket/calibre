package com.gitlab.aecsocket.calibre.core.system.gun.reload.external;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.CapacityComponentContainerSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentAccessorSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SingleChamberReloadSystem extends AbstractSystem implements ExternalReloadSystem {
    public static final String ID = "single_chamber_reload";
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
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
    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }

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

    protected <I extends Item> boolean reloadSingle(ItemUser user, ItemSlot<I> slot, TickContext tickContext, SchedulerSystem scheduler, GunSystem.Events.ExternalReload<I> event) {
        int remaining = container.remaining();
        if (remaining <= 0) {
            finishReload(user, slot, tickContext, event);
            return true;
        }
        long singleAfter = tree().<NumberDescriptor.Long>stat("reload_single_after").apply();
        int amount = singleAfter == 0
                ? remaining
                : singleAfter < tickContext.delta() ? (int) (tickContext.delta() / singleAfter) : 1;
        for (int i = 0; i < amount; i++) {
            ComponentAccessorSystem.Result result = collectChamber(user);
            if (result == null) {
                finishReload(user, slot, tickContext, event);
                return true;
            } else {
                container.push(result.component());
                result.removeItem();
                scheduler.delay(tree().<NumberDescriptor.Long>stat("reload_single_delay").apply());
                scheduler.<SingleChamberReloadSystem, I>schedule(this, singleAfter,
                        (self, equip, ctx) -> self.reloadSingle(equip.user(), equip.slot(), equip.tickContext(), ctx.system(event.system().scheduler()), event));
                update(user, slot, event);
                return false;
            }
        }
        return true;
    }

    protected ComponentAccessorSystem.Result collectChamber(ItemUser user) {
        return accessor.collectComponent(user, component -> GunSystem.getProjectile(component).c() != null && container.accepts(component), null);
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
                (self, equip, ctx) -> self.reloadSingle(equip.user(), equip.slot(), equip.tickContext(), ctx.system(scheduler), event));
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
