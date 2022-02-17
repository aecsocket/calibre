package com.github.aecsocket.calibre.core;

import com.github.aecsocket.minecommons.core.raycast.Boundable;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.core.vector.polar.Coord3;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Bullet<B extends Boundable> extends Projectile<B> {
    public Bullet(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, double dragCoeff, double dragArea, double mass) {
        super(raycast, position, velocity, gravity, dragCoeff, dragArea, mass);
    }

    protected abstract double deflectThreshold(Raycast.Hit<B> hit);

    protected abstract double penetration();

    protected abstract double hardness(Raycast.Hit<B> hit);

    @Override
    protected void hit(TaskContext ctx, Vector3 origin, Vector3 direction, double speed, Raycast.Result<B> ray, Raycast.Hit<B> hit) {
        Vector3 normal = hit.normal();
        double dot = Math.abs(direction.dot(normal));
        double deflectMult = 1 - (dot / deflectThreshold(hit));
        if (deflectMult > 0) {
            deflect(direction, normal, deflectMult);
        } else {
            double hardness = hardness(hit);

            penetrate(direction, hit.out(), hit.penetration());
            velocity = velocity.multiply(0.5);
            Coord3 coord = Coord3.coord3(speed, velocity.sphericalYaw(), velocity.sphericalPitch());
            velocity = coord
                    .yaw(coord.yaw() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                    .pitch(coord.pitch() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                    .cartesian();
        }
    }
}
