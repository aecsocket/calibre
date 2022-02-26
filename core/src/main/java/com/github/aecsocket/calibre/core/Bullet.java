package com.github.aecsocket.calibre.core;

import com.github.aecsocket.minecommons.core.raycast.Boundable;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;

public abstract class Bullet<B extends Boundable, M extends Projectile.Medium<B>> extends Projectile<B, M> {
    private final double originalSpeed;

    public Bullet(Raycast<B> raycast, Vector3 position, Vector3 velocity, double gravity, M medium) {
        super(raycast, position, velocity, gravity, medium);
        originalSpeed = velocity.length();
    }

    public double originalSpeed() { return originalSpeed; }

    protected abstract double hardness(B hit);
    protected abstract double penetration();
    protected abstract Vector2 penetrationDeflect();
    protected abstract double deflectThreshold();

    @Override
    protected boolean changeMedium(StepContext ctx, Raycast.Result<B> ray, Raycast.Hit<B> hit, Vector3 position, M oldMedium, M newMedium) {
        return super.changeMedium(ctx, ray, hit, position, oldMedium, newMedium);
        /*double hardness = hardness(hit.hit());
        double power = Numbers.clamp01(speed() / originalSpeed);
        System.out.println("power = " + power);
        double penetration = penetration() * power;
        double penMult;
        if (penetration > 0 && (penMult = 1 - Numbers.clamp01(hardness / (penetration * power))) > 0) {
            // penetrates
            Coord3 coord = Coord3.coord3(ctx.speed(), velocity.sphericalYaw(), velocity.sphericalPitch());
            Vector2 penDeflect = penetrationDeflect();
            velocity = coord
                // the weaker the penetration (lower penMult), the more the deflection
                .yaw(coord.yaw() + ThreadLocalRandom.current().nextGaussian() * (penDeflect.x() / penMult))
                .pitch(coord.pitch() + ThreadLocalRandom.current().nextGaussian() * (penDeflect.y() * penMult))
                .cartesian();
            return true;
        } else {
            // try deflect
            Vector3 normal = hit.normal();
            double dot = Math.abs(ctx.direction().dot(normal));
            double deflectMult = 1 - dot / (deflectThreshold() * power);
            if (deflectMult > 0) {
                // deflect
                this.position = position.subtract(ctx.direction().multiply(EPSILON));
                velocity = Vector3.reflect(velocity, normal).multiply(deflectMult);
            } else {
                // stuck
                ctx.task().cancel();
            }
            return false;
        }*/
    }
}
