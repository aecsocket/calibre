package com.github.aecsocket.calibre.core;

import com.github.aecsocket.minecommons.core.raycast.Boundable;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;

import java.util.function.Predicate;

public abstract class Projectile<B extends Boundable> {
    public static final double GRAVITY = 9.81;
    public static final double EPSILON = 0.001;

    protected final Raycast<B> raycast;
    protected Vector3 position;
    protected Vector3 velocity;
    protected double gravity;
    protected double dragCoeff;
    protected double dragArea;
    protected double mass;

    private double travelled;
    private int collisions;

    public Projectile(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, double dragCoeff, double dragArea, double mass) {
        this.raycast = raycast;
        this.position = position;
        this.velocity = velocity;
        this.gravity = gravity;
        this.dragCoeff = dragCoeff;
        this.dragArea = dragArea;
        this.mass = mass;
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

    protected abstract double mediumDensity(Vector3 pos);
    protected abstract Predicate<B> test();

    public double travelled() { return travelled; }
    public int collisions() { return collisions; }

    public void tick(TaskContext ctx) {
        double step = ctx.delta() / 1000d;
        double sqrSpeed = velocity.sqrLength();

        if (sqrSpeed > 0) {
            double speed = Math.sqrt(sqrSpeed);
            double distance = speed * step;
            Vector3 origin = position;
            Vector3 direction = velocity.divide(speed);
            var ray = raycast.cast(position, direction, distance, test());
            position = ray.pos().add(direction.multiply(EPSILON));
            travelled += ray.distance() + EPSILON;
            var hit = ray.hit();
            // TODO hit
            if (hit != null) {
                ctx.cancel();
                return;
            }
            step(ctx, ray, origin, direction);

            double mediumDensity = mediumDensity(position);
            double drag = drag(mediumDensity, dragCoeff, dragArea, sqrSpeed, mass);

            velocity = direction.multiply(Math.max(EPSILON, speed - drag * step));
            velocity = velocity.y(velocity.y() - (gravity * step));
        }
    }

    protected void step(TaskContext ctx, Raycast.Result<B> ray, Vector3 origin, Vector3 direction) {}

    /*
    mediumDensity = kg/m3
    dragCoeff = (unitless, 0 -> inf)
    area = m^2
    sqrSpeed = m/s
    mass = kg
     */
    public static double drag(double mediumDensity, double dragCoeff, double area, double sqrSpeed, double mass) {
        return (((mediumDensity * dragCoeff * area) / 2) * sqrSpeed) * mass;
    }
}
