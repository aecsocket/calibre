package me.aecsocket.calibre.defaults.system.projectile;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.util.Projectile;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface ProjectileProviderSystem extends CalibreSystem {
    @Override default void initialize(CalibreComponent parent, ComponentTree tree) {
        parent.registerSystemService(ProjectileProviderSystem.class, this);
    }

    default Projectile createProjectile(Data data) {
        return new Projectile(
                data.location,
                data.velocity,
                data.bounce,
                data.drag,
                data.gravity,
                data.expansion
        );
    }

    class Data {
        private Location location;
        private Vector velocity;
        private double bounce;
        private double drag;
        private double gravity;
        private double expansion;

        public Data(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion) {
            this.location = location;
            this.velocity = velocity;
            this.bounce = bounce;
            this.drag = drag;
            this.gravity = gravity;
            this.expansion = expansion;
        }

        public Data(Data o) {
            location = o.location.clone();
            velocity = o.velocity.clone();
            bounce = o.bounce;
            drag = o.drag;
            gravity = o.gravity;
            expansion = o.expansion;
        }

        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }

        public Vector getVelocity() { return velocity; }
        public void setVelocity(Vector velocity) { this.velocity = velocity; }

        public double getBounce() { return bounce; }
        public void setBounce(double bounce) { this.bounce = bounce; }

        public double getDrag() { return drag; }
        public void setDrag(double drag) { this.drag = drag; }

        public double getGravity() { return gravity; }
        public void setGravity(double gravity) { this.gravity = gravity; }

        public double getExpansion() { return expansion; }
        public void setExpansion(double expansion) { this.expansion = expansion; }
    }
}
