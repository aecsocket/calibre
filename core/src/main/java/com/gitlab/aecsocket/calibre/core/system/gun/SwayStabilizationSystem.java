package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.core.world.user.StabilizableUser;
import com.gitlab.aecsocket.unifiedframework.core.scheduler.TaskContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SwayStabilizationSystem extends AbstractSystem implements SwayStabilization {
    public static final String ID = "sway_stabilization";
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("sway_stabilization_cost", NumberDescriptorStat.of(1000d))
            .get();

    /**
     * Used for registration + deserialization.
     */
    public SwayStabilizationSystem() { super(0); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public SwayStabilizationSystem(SwayStabilizationSystem o) {
        super(o);
    }

    @Override public String id() { return ID; }

    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }

    @Override
    public boolean stabilizes(TaskContext ctx, ItemUser raw) {
        if (raw instanceof StabilizableUser) {
            StabilizableUser user = (StabilizableUser) raw;
            if (user.stabilize(ctx) && user.stamina() > 0) {
                user.reduceStamina(tree().<NumberDescriptor.Double>stat("sway_stabilization_cost").apply() * (ctx.delta() / 1000d));
                return true;
            }
        }
        return false;
    }

    @Override public abstract SwayStabilizationSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
