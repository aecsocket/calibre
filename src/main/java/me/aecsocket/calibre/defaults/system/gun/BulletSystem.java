package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.unifiedframework.loop.TickContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;

public class BulletSystem extends BaseSystem implements ProjectileProviderSystem {
    public static class Projectile extends me.aecsocket.unifiedframework.util.Projectile {
        public Projectile(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion) {
            super(location, velocity, bounce, drag, gravity, expansion);
        }

        public Projectile(Location location, Vector velocity, double bounce, double drag) {
            super(location, velocity, bounce, drag);
        }

        public Projectile(Location location, Vector velocity, double bounce) {
            super(location, velocity, bounce);
        }

        public Projectile(Location location, Vector velocity) {
            super(location, velocity);
        }

        public Projectile(Location location) {
            super(location);
        }

        @Override
        protected void step(TickContext tickContext, Vector from, Vector delta) {
            super.step(tickContext, from, delta);
            World world = getLocation().getWorld();
            world.spawnParticle(Particle.END_ROD, from.add(delta).toLocation(world), 0);
        }
    }

    public static final String ID = "bullet";

    public BulletSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
        ProjectileProviderSystem.super.initialize(parent, tree);
    }

    @Override
    public Projectile createProjectile(Data data) {
        return new Projectile(
                data.getLocation(), data.getVelocity(),
                data.getBounce(), data.getDrag(), data.getGravity(), data.getExpansion());
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public BulletSystem copy() { return this; }
}
