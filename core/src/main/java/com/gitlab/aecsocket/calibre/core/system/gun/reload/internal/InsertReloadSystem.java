package com.gitlab.aecsocket.calibre.core.system.gun.reload.internal;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentContainerSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
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

public abstract class InsertReloadSystem extends AbstractSystem implements InternalReloadSystem {
    public static final String ID = "insert_reload";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("load_delay", NumberDescriptorStat.of(0L))
            .init("load_after", NumberDescriptorStat.of(0L))
            .get();

    protected transient ComponentAccessorSystem accessor;

    /**
     * Used for registration + deserialization.
     */
    public InsertReloadSystem() { super(0); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public InsertReloadSystem(InsertReloadSystem o) {
        super(o);
    }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    public ComponentAccessorSystem collector() { return accessor; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(ComponentAccessorSystem.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        accessor = require(ComponentAccessorSystem.class);
    }

    protected ComponentAccessorSystem.Result collectAmmo(ItemUser user, CalibreSlot slot) {
        return accessor.collectComponent(user,
                component -> slot.isCompatible(component) && component.system(ComponentContainerSystem.class) != null,
                Comparator.comparingInt(component -> -component.system(ComponentContainerSystem.class).amount()));
    }

    @Override
    public <I extends Item> void reload(GunSystem.Events.InternalReload<I> event) {
        CalibreSlot toFill = event.ammoSlot();
        if (collectAmmo(event.user(), toFill) == null) {
            event.result(ItemEvents.Result.FAILURE);
            return;
        }
        SchedulerSystem scheduler = event.system().scheduler();
        scheduler.delay(tree().<NumberDescriptor.Long>stat("load_delay").apply());
        scheduler.<InsertReloadSystem, I>schedule(this, tree().<NumberDescriptor.Long>stat("load_after").apply(),
                (self, equip, ctx) -> self.insertAmmo(equip, ctx, event));
        update(event);
        event.result(ItemEvents.Result.SUCCESS);
    }

    protected <I extends Item> void insertAmmo(ItemEvents.Equipped<I> equip, SchedulerSystem.TreeContext ctx, GunSystem.Events.InternalReload<I> event) {
        ItemUser user = equip.user();
        ItemSlot<I> slot = equip.slot();

        CalibreSlot toFill = ctx.slot(event.ammoSlot());
        if (toFill == null)
            return;
        ComponentAccessorSystem.Result result = collectAmmo(user, toFill);
        if (result == null)
            return;

        toFill.set(result.component());
        update(user, slot, event);
        result.removeItem();
    }

    @Override public abstract InsertReloadSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}