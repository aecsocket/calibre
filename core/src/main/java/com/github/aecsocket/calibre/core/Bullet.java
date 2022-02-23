package com.github.aecsocket.calibre.core;

import com.github.aecsocket.minecommons.core.raycast.Boundable;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.core.vector.polar.Coord3;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Bullet<B extends Boundable, M extends Projectile.Medium<B>> extends Projectile<B, M> {
    private final double originalSpeed;

    public Bullet(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, double dragCoeff, double dragArea, double mass, M medium) {
        super(raycast, position, velocity, gravity, dragCoeff, dragArea, mass, medium);
        originalSpeed = velocity.length();
    }

    public double originalSpeed() { return originalSpeed; }

    protected abstract double deflectThreshold(B hit);

    protected abstract double penetration();

    protected abstract double hardness(B hit);

    /*@Override
    protected void hit(TaskContext ctx, Vector3 origin, Vector3 direction, double speed, Raycast.Result<B> ray, Raycast.Hit<B> hit) {
        double power = speed / originalSpeed;
        B bound = hit.hit();

        double penetration = 1 - hardness(bound) / (penetration() * power);
        if (penetration > 0) {
            // penetrates
            penetrate(direction, hit.out(), hit.penetration());
            velocity = velocity.multiply(Math.min(1, penetration));
            Coord3 coord = Coord3.coord3(speed, velocity.sphericalYaw(), velocity.sphericalPitch());
            // TODO deflection inside a medium stuff, make it variable
            velocity = coord
                .yaw(coord.yaw() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                .pitch(coord.pitch() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                .cartesian();
            ++penetrated;
        } else {
            // try deflect
            Vector3 normal = hit.normal();
            double dot = Math.abs(direction.dot(normal));
            double deflectMult = 1 - dot / (deflectThreshold(bound) * power);
            if (deflectMult > 0) {
                // deflect
                deflect(direction, normal, deflectMult);
            } else {
                // stuck
                ctx.cancel();
            }
        }
    }*/
}
