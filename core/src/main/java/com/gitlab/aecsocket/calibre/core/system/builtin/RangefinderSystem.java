package com.gitlab.aecsocket.calibre.core.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.core.world.user.SenderUser;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.loop.MinecraftSyncLoop;
import net.kyori.adventure.text.Component;

import java.util.Objects;

public abstract class RangefinderSystem extends AbstractSystem {
    public static final String ID = "rangefinder";
    public static final int LISTENER_PRIORITY = 0;
    @FromMaster protected double maxDistance = -1;
    @FromMaster protected double precision;
    @FromMaster protected String format = "%.1f";

    /**
     * Used for registration + deserialization.
     */
    public RangefinderSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public RangefinderSystem(RangefinderSystem o) {
        super(o);
        maxDistance = o.maxDistance;
        precision = o.precision;
        format = o.format;
    }

    @Override public String id() { return ID; }

    public double maxDistance() { return maxDistance; }
    public void maxDistance(double maxDistance) { this.maxDistance = maxDistance; }

    public double precision() { return precision; }
    public void precision(double accuracy) { this.precision = accuracy; }

    public String format() { return format; }
    public void format(String format) { this.format = format; }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);

        EventDispatcher events = tree.events();
        int priority = listenerPriority();
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, priority);
    }

    protected void onEvent(ItemEvents.Equipped<?> event) {
        if (!(event.tickContext().loop() instanceof MinecraftSyncLoop))
            return;

        ItemUser user = event.user();
        if (user instanceof SenderUser) {
            Double distance = getDistance(user);
            if (distance != null) {
                distance = Math.floor(distance / precision) * precision;
            }
            ((SenderUser) user).showTitle(
                    Component.empty(),
                    distance == null
                            ? gen(user.locale(), "system." + ID + ".no_range")
                            : gen(user.locale(), "system." + ID + ".range", "range", String.format(format, distance)),
                    0, 250, 0
            );
        }
    }

    protected abstract Double getDistance(ItemUser user);

    @Override public abstract RangefinderSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangefinderSystem that = (RangefinderSystem) o;
        return Double.compare(that.maxDistance, maxDistance) == 0 && Double.compare(that.precision, precision) == 0 && Objects.equals(format, that.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxDistance, precision, format);
    }
}
