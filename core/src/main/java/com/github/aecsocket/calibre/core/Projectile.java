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
        double density();
        boolean isOf(B hit);
    }

    protected final Raycast<B> raycast;
    protected Vector3 position;
    protected Vector3 velocity;
    protected double gravity;
    protected double dragCoeff;
    protected double dragArea;
    protected double mass;

    private M medium;
    private double speed;
    private double travelled;

    public Projectile(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, double dragCoeff, double dragArea, double mass, M medium) {
        this.raycast = raycast;
        this.position = position;
        this.velocity = velocity;
        this.gravity = gravity;
        this.dragCoeff = dragCoeff;
        this.dragArea = dragArea;
        this.mass = mass;
        this.medium = medium;
    }

    public Raycast<B> raycast() { return raycast; }

    public Vector3 position() { return position; }
    public void position(Vector3 position) { this.position = position; }

    public Vector3 velocity() { return velocity; }
    public void velocity(Vector3 velocity) { this.velocity = velocity; }

    public double gravity() { return gravity; }
    public void gravity(double gravity) { this.gravity = gravity; }

    public double dragCoeff() { return dragCoeff; }
    public void dragCoeff(double dragCoeff) { this.dragCoeff = dragCoeff; }

    public double dragArea() { return dragArea; }
    public void dragArea(double dragArea) { this.dragArea = dragArea; }

    public double mass() { return mass; }
    public void mass(double mass) { this.mass = mass; }

    public M medium() { return medium; }
    public double speed() { return speed; }
    public double travelled() { return travelled; }

    protected abstract Predicate<B> castTest();
    protected abstract M mediumOf(Raycast.Result<B> ray, Raycast.Hit<B> hit);

    protected double step(TaskContext ctx, double sec) {
        velocity = velocity.y(velocity.y() - gravity * sec);
        double sqrSpeed = velocity.sqrLength();
        if (sqrSpeed <= 0)
            return 0; // no seconds left to process

        speed = Math.sqrt(sqrSpeed);
        Vector3 direction = velocity.divide(speed);
        // drag applies deceleration to the speed
        // it can, at most, bring the speed to 0, but never invert direction
        double drag = drag(medium.density(), dragCoeff, dragArea, sqrSpeed, mass);
        speed = Math.max(0, speed - drag * sec);
        velocity = direction.multiply(speed);
        if (speed <= 0)
            return 0;

        Vector3 epsilon = direction.multiply(EPSILON);
        double distance = speed * sec;
        var ray = raycast.cast(position, direction, distance, b ->  !medium.isOf(b) && castTest().test(b));

        var hit = ray.hit();
        double rayDist = ray.distance();
        if (hit == null) {
            position = ray.pos();
            travelled += rayDist;
        } else {
            M newMedium = mediumOf(ray, hit);
            changeMedium(new StepContext(ctx, direction, speed, epsilon), ray, ray.pos(), medium, newMedium);
            medium = newMedium;

            if (rayDist > 0) {
                // ray started outside the bound
                position = ray.pos();
                travelled += rayDist;
            } else {
                // ray started inside the bound
                // so we manually move the pos forward
                distance = hit.distOut();
                position = position.add(direction.multiply(distance));
                travelled += distance;
                rayDist = distance;
            }
        }

        double remaining = 1 - rayDist / distance;
        return remaining > EPSILON ? remaining * sec : 0;
    }

    public void tick(TaskContext ctx) {
        for (double sec = ctx.delta() / 1000d; sec > 0; sec = step(ctx, sec)) {
            if (ctx.cancelled()) {
                removed(ctx);
                break;
            }
        }
    }

    public record StepContext(
        TaskContext task, Vector3 direction, double speed, Vector3 epsilon
    ) {}

    protected void changeMedium(StepContext ctx, Raycast.Result<B> ray, Vector3 position, M oldMedium, M newMedium) {}

    protected void removed(TaskContext ctx) {}

    protected void deflect(Vector3 direction, Vector3 normal, double power) {
        position = position.subtract(direction.multiply(EPSILON));
        velocity = Vector3.reflect(velocity, normal).multiply(power);
    }

    protected void penetrate(Vector3 direction, Vector3 out, double penetration) {
        position = out.add(direction.multiply(EPSILON));
        travelled += penetration + EPSILON;
    }

    /*
    mediumDensity = kg/m3
    dragCoeff = (unitless, 0 -> inf)
    area = m^2
    sqrSpeed = m/s
    mass = kg
     */
    public static double drag(double mediumDensity, double dragCoeff, double area, double sqrSpeed, double mass) {
        double force = (mediumDensity * dragCoeff * area * sqrSpeed) / 2;
        // a = F / m
        return force / mass;
    }
}
