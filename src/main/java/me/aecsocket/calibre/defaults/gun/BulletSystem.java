package me.aecsocket.calibre.defaults.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class BulletSystem implements CalibreSystem<Void>,
        ProjectileProviderSystem {
    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;

    public static class Projectile extends me.aecsocket.unifiedframework.util.Projectile {
        private ParticleData[] trail;
        private double stepSize = 0.5;
        private double travelled;

        public Projectile(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, ParticleData[] trail) {
            super(location, velocity, bounce, drag, gravity, expansion);
            this.trail = trail;
        }

        public ParticleData[] getTrail() { return trail; }
        public void setTrail(ParticleData[] trail) { this.trail = trail; }

        public double getStepSize() { return stepSize; }
        public void setStepSize(double stepSize) { this.stepSize = stepSize; }

        public double getTravelled() { return travelled; }
        public void setTravelled(double travelled) { this.travelled = travelled; }

        @Override
        protected void step(TickContext tickContext, Vector from, Vector delta) {
            if (trail == null) return;

            World world = getLocation().getWorld();

            double distance = delta.length();

            travelled += distance;
            while (travelled > stepSize) {
                double percent = travelled / distance;
                ParticleData.spawn(from.clone().add(delta.clone().multiply(percent)).toLocation(world), trail);
                travelled -= stepSize;
            }
        }

        @Override
        protected void successHit(TickContext tickContext, RayTraceResult ray) {
            tickContext.remove(); // TODO
        }
    }

    public BulletSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "bullet"; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) { this.parent = parent; }

    @Override
    public Projectile create(Data data) {
        GenericData gData = data instanceof GenericData ? (GenericData) data : null;
        return new Projectile(
                data.getLocation(), data.getVelocity(),
                data.getBounce(), data.getDrag(), data.getGravity(), data.getExpansion(),
                gData == null ? null : gData.getTrail()
        );
    }

    public BulletSystem clone() { try { return (BulletSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public BulletSystem copy() { return clone(); }
}
