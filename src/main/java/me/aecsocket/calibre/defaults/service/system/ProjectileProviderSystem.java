package me.aecsocket.calibre.defaults.service.system;

import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * A system which can create a projectile with specific conditions.
 */
public interface ProjectileProviderSystem {
    /**
     * Creates a projectile according to a data class.
     * @param data The event.
     * @return The projectile.
     */
    Projectile create(Data data);

    /**
     * Represents projectile creation data.
     */
    class Data {
        private final Location location;
        private final Vector velocity;
        private final double bounce;
        private final double drag;
        private final double gravity;
        private final double expansion;

        public Data(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion) {
            this.location = location;
            this.velocity = velocity;
            this.bounce = bounce;
            this.drag = drag;
            this.gravity = gravity;
            this.expansion = expansion;
        }

        public Location getLocation() { return location; }
        public Vector getVelocity() { return velocity; }
        public double getBounce() { return bounce; }
        public double getDrag() { return drag; }
        public double getGravity() { return gravity; }
        public double getExpansion() { return expansion; }
    }

    /**
     * Represents a generic implementation of Data, with extra fields.
     */
    class GenericData extends Data {
        private final ParticleData[] trail;

        public GenericData(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, ParticleData[] trail) {
            super(location, velocity, bounce, drag, gravity, expansion);
            this.trail = trail;
        }

        public ParticleData[] getTrail() { return trail; }
    }
}
