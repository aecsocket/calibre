package com.gitlab.aecsocket.calibre.paper.system.gun.projectile;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.loop.MinecraftSyncLoop;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.BooleanStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExplosiveProjectileSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "explosive_projectile";
    public static final int LISTENER_PRIORITY = 0;
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("explosion_power", NumberDescriptorStat.of(0f))
            .init("explosion_set_fire", new BooleanStat(false))
            .init("explosion_break_blocks", new BooleanStat(false))

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
        PaperProjectileSystem.PaperProjectile projectile = event.projectile();
        StatMap stats = projectile.stats;
        if (projectile.travelled() > stats.<NumberDescriptor.Double>val("arm_distance").apply()) {
            Vector3D position = projectile.position();
            projectile.world().createExplosion(position.x(), position.y(), position.z(),
                    stats.<NumberDescriptor.Float>val("explosion_power").apply(),
                    stats.<Boolean>val("explosion_set_fire"),
                    stats.<Boolean>val("explosion_break_blocks"));
        }
        event.tickContext().remove();
    }

    @Override public ExplosiveProjectileSystem copy() { return new ExplosiveProjectileSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
