package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.world.user.StabilizableUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SwayStabilizationSystem extends AbstractSystem implements SwayStabilization {
    public static final String ID = "sway_stabilization";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
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

    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public boolean stabilizes(TickContext tickContext, ItemUser raw) {
        if (raw instanceof StabilizableUser) {
            StabilizableUser user = (StabilizableUser) raw;
            if (user.stabilize(tickContext) && user.stamina() > 0) {
                user.reduceStamina(tree().<NumberDescriptor.Double>stat("sway_stabilization_cost").apply() * (tickContext.delta() / 1000d));
                return true;
            }
        }
        return false;
    }

    public abstract SwayStabilizationSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
