package com.github.aecsocket.calibre.core;

import com.github.aecsocket.minecommons.core.raycast.Boundable;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;

import java.util.function.Predicate;

public abstract class Projectile<B extends Boundable, M extends Projectile.Medium<B>> {
    public static final double
        GRAVITY = 9.81,
        EPSILON = 0.001;

    public interface Medium<B> {
        double friction();
        boolean isOf(B hit);
    }

    protected final Raycast<B> raycast;
    protected Vector3 position;
    protected Vector3 velocity;
    protected double gravity;

    private M medium;
    private double speed;
    private double travelled;

    public Projectile(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, M medium) {
        this.raycast = raycast;
        this.position = position;
        this.velocity = velocity;
        this.gravity = gravity;
        this.medium = medium;
    }

    public Raycast<B> raycast() { return raycast; }

    public Vector3 position() { return position; }
    public void position(Vector3 position) { this.position = position; }

    public Vector3 velocity() { return velocity; }
    public void velocity(Vector3 velocity) { this.velocity = velocity; }

    public double gravity() { return gravity; }
    public void gravity(double gravity) { this.gravity = gravity; }

    public M medium() { return medium; }
    public double speed() { return speed; }
    public double travelled() { return travelled; }

    protected abstract Predicate<B> castTest();
    protected abstract M mediumOf(Raycast.Result<B> ray, Raycast.Hit<B> hit);

    protected double step(TaskContext ctx, double sec, double distance) {
        Vector3 direction = velocity.divide(speed);
        Vector3 epsilon = direction.multiply(EPSILON);
        var ray = raycast.cast(position, direction, distance, b -> !medium.isOf(b) && castTest().test(b));

        var hit = ray.hit();
        double penetration = 0;
        M oldMedium = medium;
        if (hit == null) {
            penetration = ray.distance();
        } else {
            M newMedium = mediumOf(ray, hit);
            if (changeMedium(new StepContext(ctx, direction, speed, epsilon), ray, hit, ray.pos(), oldMedium, newMedium)) {
                medium = newMedium;
                if (ray.distance() > 0) {
                    // ray started outside the bound
                    // move it inwards by the epsilon so, on the next step, it is inside the bound
                    penetration += EPSILON;
                } else {
                    // ray started inside the bound
                    // so we manually move the pos forward
                    // use distOut here rather than negative rayDist, since it avoids weird large gaps
                    penetration = hit.distOut();
                }
            }
        }
        travelled += penetration;

        System.out.println(oldMedium + " speed = " + speed + " - " + oldMedium.friction() + " over " + sec + "s over " + penetration + "m");
        speed = Math.max(EPSILON, speed - oldMedium.friction() * sec * penetration);
        velocity = direction.multiply(speed);
        position = position.add(direction.multiply(penetration));

        return distance - penetration;
    }

    public void tick(TaskContext ctx) {
        double sec = ctx.delta() / 1000d;
        velocity = velocity.y(velocity.y() - gravity * sec);
        double sqrSpeed = velocity.sqrLength();
        if (sqrSpeed <= 0)
            return;
        speed = Math.sqrt(sqrSpeed);

        for (double distance = speed * (ctx.delta() / 1000d); distance > EPSILON; distance = step(ctx, sec, distance)) {
            if (ctx.cancelled()) {
                removed(ctx);
                break;
            }
        }
    }

    public record StepContext(
        TaskContext task, Vector3 direction, double speed, Vector3 epsilon
    ) {}

    protected boolean changeMedium(StepContext ctx, Raycast.Result<B> ray, Raycast.Hit<B> hit, Vector3 position, M oldMedium, M newMedium) {
        return true;
    }

    protected void removed(TaskContext ctx) {}

    protected void deflect(Vector3 direction, Vector3 normal, double power) {
        position = position.subtract(direction.multiply(EPSILON));
        velocity = Vector3.reflect(velocity, normal).multiply(power);
    }

    protected void penetrate(Vector3 direction, Vector3 out, double penetration) {
        position = out.add(direction.multiply(EPSILON));
        travelled += penetration + EPSILON;
    }
}
