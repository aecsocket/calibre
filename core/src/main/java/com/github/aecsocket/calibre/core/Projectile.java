package com.github.aecsocket.calibre.core;

import com.github.aecsocket.minecommons.core.Numbers;
import com.github.aecsocket.minecommons.core.raycast.Boundable;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

public abstract class Projectile<B extends Boundable, F extends Projectile.Fluid<B>> {
    public static final double
        GRAVITY = 9.81,
        EPSILON = 0.001;

    public interface Fluid<B> {
        double density();
        boolean isOf(B hit);
    }

    protected final Raycast<B> raycast;
    protected Vector3 position;
    protected Vector3 velocity;
    protected double gravity;

    protected F fluid;
    protected double speed = -1;
    protected Vector3 direction;
    protected double travelled;

    public Projectile(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, F fluid) {
        this.raycast = raycast;
        this.position = position;
        this.velocity = velocity;
        this.gravity = gravity;
        this.fluid = fluid;
    }

    public Raycast<B> raycast() { return raycast; }

    public Vector3 position() { return position; }
    public void position(Vector3 position) { this.position = position; }

    public Vector3 velocity() { return velocity; }
    public void velocity(Vector3 velocity) { this.velocity = velocity; }

    public double gravity() { return gravity; }
    public void gravity(double gravity) { this.gravity = gravity; }

    public F fluid() { return fluid; }
    public double speed() { return speed; }
    public Vector3 direction() { return direction; }
    public double travelled() { return travelled; }

    public void tick(TaskContext ctx) {
        for (double sec = ctx.delta() / 1000d; sec > EPSILON; sec = step(ctx, sec)) {
            if (ctx.cancelled()) {
                removed(ctx);
                return;
            }
        }
    }

    protected abstract Predicate<B> castTest();
    protected abstract @Nullable F fluidOf(Raycast.Result<B> ray, Raycast.Hit<B> hit);

    protected abstract double dragCoeff();
    protected abstract double dragArea();
    protected abstract double mass();

    protected double step(TaskContext ctx, double sec) {
        velocity = velocity.y(velocity.y() - gravity * sec);
        speed = velocity.length();
        if (speed == 0) {
            direction = Vector3.ZERO;
            return 0;
        }
        direction = velocity.divide(speed);

        double maxDistance = speed * sec;
        var ray = raycast.cast(position, direction, maxDistance, bnd -> !fluid.isOf(bnd) && castTest().test(bnd));
        var hit = ray.hit();
        F oldFluid = fluid;
        double penetrate;
        if (hit == null) {
            System.out.printf("(miss, dst = %.3f)\n", ray.distance());
            penetrate = miss(ctx, sec, maxDistance, ray);
        } else {
            System.out.printf("hit: %s\n", ""+hit);
            F newFluid = fluidOf(ray, hit);
            penetrate = newFluid == null
                ? collided(ctx, sec, maxDistance, ray, hit)
                : changeFluid(ctx, sec, maxDistance, ray, hit, fluid, newFluid);
        }

        speed = Math.max(0, speed - dragCoeff() * fluid.density() * speed * speed * 0.5 * dragArea());
        return sec * Numbers.clamp01(1 - penetrate / maxDistance);
    }

    protected void removed(TaskContext ctx) {}

    protected double miss(TaskContext ctx, double sec, double maxDistance, Raycast.Result<B> ray) {
        double penetrate = ray.distance();
        travelled += penetrate;
        position = position.add(direction.multiply(penetrate));
        return penetrate;
    }

    protected double collided(TaskContext ctx, double sec, double maxDistance, Raycast.Result<B> ray, Raycast.Hit<B> hit) {
        position = hit.out();
        if (ray.distance() > 0) {
            // start outside the bound
            double penetrate = ray.distance();
            travelled += penetrate + hit.penetration();
            position = hit.out();
            return penetrate;
        } else {
            // start inside the bound
            double penetrate = hit.distOut();
            travelled += penetrate;
            return penetrate;
        }
    }

    protected double changeFluid(TaskContext ctx, double sec, double maxDistance, Raycast.Result<B> ray, Raycast.Hit<B> hit, F oldFluid, F newFluid) {
        double penetrate = ray.distance() > 0 ? ray.distance() + EPSILON : hit.distOut();
        fluid = newFluid;
        travelled += penetrate;
        position = ray.pos().add(direction.multiply(EPSILON));
        return penetrate;
    }

    /*protected abstract Predicate<B> castTest();
    protected abstract F mediumOf(Raycast.Result<B> ray, Raycast.Hit<B> hit);

    protected double step(TaskContext ctx, double remaining) {
        Vector3 epsilon = direction.multiply(EPSILON);
        double friction = medium.friction() * speed;
        //double distance = speed * friction * remaining;
        double distance = Math.max(0, (speed - (friction * remaining)) * remaining);
        if (distance <= 0) {
            velocity = Vector3.ZERO;
            return 0;
        }

        System.out.printf("%.3f * %.3f * %.3f\n", speed, friction, remaining);
        var ray = raycast.cast(position, direction, distance, b -> !medium.isOf(b) && castTest().test(b));
        var hit = ray.hit();
        F oldMedium = medium;
        double penetrated;
        if (hit == null) {
            // this ray penetrated the full distance of our current medium
            penetrated = ray.distance();
        } else {
            F newMedium = mediumOf(ray, hit);
            var atomicDir = new AtomicReference<>(direction);
            if (changeMedium(new StepContext(ctx, direction, speed, epsilon, atomicDir::set), ray, hit, ray.pos(), oldMedium, newMedium)) {
                medium = newMedium;
                if (ray.distance() > 0) {
                    // ray started outside the bound
                    // move it inwards by the epsilon so, on the next step, it is inside the bound
                    penetrated = ray.distance() + EPSILON;
                } else {
                    // ray started inside the bound
                    // so we manually move the pos forward
                    // use distOut here rather than negative rayDist, since it avoids weird large gaps
                    penetrated = hit.distOut();
                }
            } else {
                // else we didn't change medium, so we assume our implementation handles it
                penetrated = 0;
            }
            direction = atomicDir.get();
        }

        double d = speed;
        speed = Math.max(EPSILON, speed - friction * remaining * penetrated);
        velocity = direction.multiply(speed);
        System.out.printf("%.3f -> %.3f (med = %s, fric = %.3f, rem = %.3f, pen = %.3f)\n", d, speed, medium, friction, remaining, penetrated);
        travelled += penetrated;
        position = position.add(direction.multiply(penetrated));
        remaining *= 1 - penetrated / distance;
        return remaining > EPSILON ? remaining : 0;
    }

    public void tick(TaskContext ctx) {
        double sec = ctx.delta() / 1000d;
        velocity = velocity.y(velocity.y() - gravity * sec);
        double sqrSpeed = velocity.sqrLength();
        if (sqrSpeed <= 0)
            return;
        speed = Math.sqrt(sqrSpeed);
        direction = velocity.divide(speed);

        tick0(ctx, sec);
    }

    protected void tick0(TaskContext ctx, double sec) {
        for (double remaining = sec; remaining > EPSILON; remaining = step(ctx, remaining)) {
            if (ctx.cancelled()) {
                removed(ctx);
                break;
            }
            direction = velocity.divide(speed);
        }
    }

    public record StepContext(
        TaskContext task, Vector3 direction, double speed, Vector3 epsilon,
        Consumer<Vector3> setDirection
    ) {
        public void direction(Vector3 direction) {
            setDirection.accept(direction);
        }
    }

    protected boolean changeMedium(StepContext ctx, Raycast.Result<B> ray, Raycast.Hit<B> hit, Vector3 position, F oldMedium, F newMedium) {
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
    }*/
}
