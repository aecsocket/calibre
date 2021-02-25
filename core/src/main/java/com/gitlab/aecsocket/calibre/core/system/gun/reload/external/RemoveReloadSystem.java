package com.gitlab.aecsocket.calibre.core.system.gun.reload.external;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.CapacityComponentContainerSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentContainerSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.reload.internal.InternalReloadSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentAccessorSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class RemoveReloadSystem extends AbstractSystem implements ExternalReloadSystem {
    public static final String ID = "remove_reload";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("unload_delay", NumberDescriptorStat.of(0L))
            .init("unload_after", NumberDescriptorStat.of(0L))
            .get();

    protected transient ComponentContainerSystem container;
    protected transient ComponentAccessorSystem accessor;

    /**
     * Used for registration + deserialization.
     */
    public RemoveReloadSystem() { super(0); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public RemoveReloadSystem(RemoveReloadSystem o) {
        super(o);
    }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    public ComponentContainerSystem container() { return container; }
    public ComponentAccessorSystem collector() { return accessor; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(ComponentContainerSystem.class);
        require(ComponentAccessorSystem.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        container = require(ComponentContainerSystem.class);
        accessor = require(ComponentAccessorSystem.class);
    }

    protected ComponentAccessorSystem.Result collectAmmo(ItemUser user) {
        return accessor.collectComponent(user,
                component -> parent.parent() != null && parent.parent().isCompatible(component) && component.system(ComponentContainerSystem.class) != null,
                Comparator.comparingInt(a -> a.system(ComponentContainerSystem.class).amount()));
    }

    @Override
    public <I extends Item> void reload(GunSystem.Events.ExternalReload<I> event) {
        if (
                (container instanceof CapacityComponentContainerSystem && ((CapacityComponentContainerSystem) container).remaining() <= 0)
                || collectAmmo(event.user()) == null
        ) {
            event.result(ItemEvents.Result.FAILURE);
            return;
        }
        SchedulerSystem scheduler = event.system().scheduler();
        scheduler.delay(tree().<NumberDescriptor.Long>stat("unload_delay").apply());
        scheduler.<RemoveReloadSystem, I>schedule(this, tree().<NumberDescriptor.Long>stat("unload_after").apply(),
                (self, equip, ctx) -> self.removeAmmo(equip, ctx, event));
        update(event);
        event.result(ItemEvents.Result.SUCCESS);
    }

    protected <I extends Item> void removeAmmo(ItemEvents.Equipped<I> equip, SchedulerSystem.TreeContext ctx, GunSystem.Events.ExternalReload<I> event) {
        ItemUser user = equip.user();
        ItemSlot<I> slot = equip.slot();

        CalibreSlot parentSlot = parent.parent();
        GunSystem gun = ctx.system(event.system());
        InternalReloadSystem handler = gun.parent().system(InternalReloadSystem.class);
        if (handler != null) {
            gun.internalReload(new GunSystem.Events.InternalReload<>(
                    equip.component(), user, slot, gun,
                    handler, parentSlot
            ));
        }

        parentSlot.set(null);
        update(user, slot, event);
        accessor.addComponent(user, parent.buildTree());
    }

    @Override public abstract RemoveReloadSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
