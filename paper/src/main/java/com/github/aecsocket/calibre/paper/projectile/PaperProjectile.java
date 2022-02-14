package com.github.aecsocket.calibre.paper.projectile;

import com.github.aecsocket.calibre.core.projectile.Projectile;
import com.gitlab.aecsocket.minecommons.core.raycast.Raycast;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.function.Predicate;

public class PaperProjectile extends Projectile<PaperRaycast.PaperBoundable> {
    private final World world;
    private final Entity shooter;

    private boolean leftShooter;

    public PaperProjectile(TreeNode fullTree, TreeNode localTree, Raycast<PaperRaycast.PaperBoundable> raycast, Vector3 position, Vector3 velocity, World world, Entity shooter) {
        super(fullTree, localTree, raycast, position, velocity);
        this.world = world;
        this.shooter = shooter;
    }

    public World world() { return world; }
    public Entity shooter() { return shooter; }

    @Override
    protected Predicate<PaperRaycast.PaperBoundable> test() {
        return b -> {
            if (b.entity() != null)
                return !b.entity().isDead() && b.entity().getType() != EntityType.DROPPED_ITEM;
            return true;
        };
    }

    @Override
    public void tick(TaskContext ctx) {
        if (PaperUtils.toBukkit(position, world).isChunkLoaded())
            super.tick(ctx);
        else
            ctx.cancel();
    }

    @Override
    protected void noHit(TaskContext ctx, Raycast.Result<PaperRaycast.PaperBoundable> ray, double step, Vector3 oPosition, Vector3 oVelocity) {
        leftShooter = true;
        super.noHit(ctx, ray, step, oPosition, oVelocity);
    }

    @Override
    public void hit(TaskContext ctx, Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit, double step, Vector3 oPosition, Vector3 oVelocity) {
        if (!leftShooter && shooter.equals(hit.hit().entity())) {
            position = hit.out().add(velocity.multiply(1e-7));
            return;
        }
        super.hit(ctx, ray, hit, step, oPosition, oVelocity);
    }
}
