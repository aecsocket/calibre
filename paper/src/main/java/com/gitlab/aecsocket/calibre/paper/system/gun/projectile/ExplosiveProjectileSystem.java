package com.gitlab.aecsocket.calibre.paper.system.gun.projectile;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.paper.util.Explosion;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExplosiveProjectileSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "explosive_projectile";
    public static final int LISTENER_PRIORITY = 0;
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("explosion_power", NumberDescriptorStat.of(0d))
            .init("explosion_damage", NumberDescriptorStat.of(0d))
            .init("explosion_dropoff", NumberDescriptorStat.of(0d))
            .init("explosion_range", NumberDescriptorStat.of(0d))

            .init("arm_distance", NumberDescriptorStat.of(0d))
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public ExplosiveProjectileSystem(CalibrePlugin plugin) {
        super(LISTENER_PRIORITY);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    private ExplosiveProjectileSystem() {
        super(LISTENER_PRIORITY);
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public ExplosiveProjectileSystem(ExplosiveProjectileSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(PaperProjectileSystem.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);

        EventDispatcher events = tree.events();
        int priority = listenerPriority();
        events.registerListener(PaperProjectileSystem.Events.Collide.class, this::onEvent, priority);
    }

    protected void onEvent(PaperProjectileSystem.Events.Collide event) {
        if (!event.local())
            return;
        PaperProjectileSystem.PaperProjectile projectile = event.projectile();
        StatMap stats = projectile.stats;
        if (projectile.travelled() >= stats.<NumberDescriptor.Double>val("arm_distance").apply()) {
            Vector3D position = projectile.position();
            new Explosion(plugin)
                    .power(stats.<NumberDescriptor.Double>val("explosion_power").apply())
                    .damage(stats.<NumberDescriptor.Double>val("explosion_damage").apply())
                    .dropoff(stats.<NumberDescriptor.Double>val("explosion_dropoff").apply())
                    .range(stats.<NumberDescriptor.Double>val("explosion_range").apply())
                    .spawn(VectorUtils.toBukkit(position).toLocation(projectile.world()), projectile.source());
        }
        event.taskContext().cancel();
    }

    @Override public ExplosiveProjectileSystem copy() { return new ExplosiveProjectileSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}