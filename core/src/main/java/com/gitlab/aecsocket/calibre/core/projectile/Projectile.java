package com.gitlab.aecsocket.calibre.core.projectile;

import com.gitlab.aecsocket.minecommons.core.raycast.Boundable;
import com.gitlab.aecsocket.minecommons.core.raycast.Raycast;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;

import java.util.function.Predicate;

public abstract class Projectile<B extends Boundable> {
    public enum OnHit {
        REMOVE,
        BOUNCE,
        PENETRATE
    }

    public static final double GRAVITY = 9.81;

    protected final TreeNode fullTree;
    protected final TreeNode localTree;
    protected final Raycast<B> raycast;
    protected Vector3 position;
    protected Vector3 velocity;

    protected double gravity = GRAVITY;

    private double travelled;
    private int collisions;

    public Projectile(TreeNode fullTree, TreeNode localTree, Raycast<B> raycast, Vector3 position, Vector3 velocity) {
        this.fullTree = fullTree;
        this.localTree = localTree;
        this.raycast = raycast;
        this.position = position;
        this.velocity = velocity;
    }

    public TreeNode fullTree() { return fullTree; }
    public TreeNode localTree() { return localTree; }
    public Raycast<B> raycast() { return raycast; }

    public Vector3 position() { return position; }
    public Projectile<B> position(Vector3 position) { this.position = position; return this; }

    public Vector3 velocity() { return velocity; }
    public Projectile<B> velocity(Vector3 velocity) { this.velocity = velocity; return this; }

    public double gravity() { return gravity; }
    public Projectile<B> gravity(double gravity) { this.gravity = gravity; return this; }

    public double travelled() { return travelled; }
    public int collisions() { return collisions; }

    protected abstract Predicate<B> test();

    protected abstract OnHit initialHitResult(TaskContext ctx, Raycast.Result<B> ray, Raycast.Hit<B> hit);

    public void tick(TaskContext ctx) {
        double step = ctx.delta() / 1000d;

        velocity = new Vector3(
                velocity.x(),
                velocity.y() - (gravity * step),
                velocity.z()
        );

        if (velocity.manhattanLength() > 0) {
            double distance = velocity.length() * step;
            Vector3 oPosition = position;
            Vector3 oVelocity = velocity;
            Raycast.Result<B> ray = raycast.cast(position, velocity.normalize(), distance, test());
            position = ray.pos();
            travelled += ray.distance();
            Raycast.Hit<B> hit = ray.hit();
            if (hit == null) {
                noHit(ctx, ray, step, oPosition, oVelocity);
            } else {
                hit(ctx, ray, hit, step, oPosition, oVelocity);
            }
            endTick(ctx, ray, step, oPosition, oVelocity);
        }
    }

    protected void hit(TaskContext ctx, Raycast.Result<B> ray, Raycast.Hit<B> hit, double step, Vector3 oPosition, Vector3 oVelocity) {
        OnHit result = initialHitResult(ctx, ray, hit);
        result = fullTree.events().call(new Events.Hit(fullTree, this, false, ctx, step, ray, oPosition, oVelocity, hit, result)).result;
        result = localTree.events().call(new Events.Hit(localTree, this, true, ctx, step, ray, oPosition, oVelocity, hit, result)).result;
        switch (result) {
            case REMOVE -> ctx.cancel();
            case BOUNCE -> {
                velocity = Vector3.reflect(velocity, hit.normal());
                position = position.add(velocity.multiply(1e-7));
            }
            case PENETRATE -> position = hit.out().add(velocity.multiply(1e-7));
        }
        ++collisions;
    }

    protected void noHit(TaskContext ctx, Raycast.Result<B> ray, double step, Vector3 oPosition, Vector3 oVelocity) {
        new Events.NoHit(fullTree, this, false, ctx, step, ray, oPosition, oVelocity).call();
        new Events.NoHit(localTree, this, true, ctx, step, ray, oPosition, oVelocity).call();
    }

    protected void endTick(TaskContext ctx, Raycast.Result<B> ray, double step, Vector3 oPosition, Vector3 oVelocity) {
        new Events.Tick(fullTree, this, false, ctx, step, ray, oPosition, oVelocity).call();
        new Events.Tick(localTree, this, true, ctx, step, ray, oPosition, oVelocity).call();
    }

    public static final class Events {
        private Events() {}

        public static class Base implements TreeEvent {
            private final TreeNode node;
            private final Projectile<?> projectile;
            private final boolean local;

            public Base(TreeNode node, Projectile<?> projectile, boolean local) {
                this.node = node;
                this.projectile = projectile;
                this.local = local;
            }

            @Override public TreeNode node() { return node; }
            public Projectile<?> projectile() { return projectile; }
            public boolean local() { return local; }
        }

        public static abstract class TickBased extends Base {
            private final TaskContext task;
            private final double step;
            private final Raycast.Result<?> ray;
            private final Vector3 oPosition;
            private final Vector3 oVelocity;

            public TickBased(TreeNode node, Projectile<?> projectile, boolean local, TaskContext task, double step, Raycast.Result<?> ray, Vector3 oPosition, Vector3 oVelocity) {
                super(node, projectile, local);
                this.task = task;
                this.step = step;
                this.ray = ray;
                this.oPosition = oPosition;
                this.oVelocity = oVelocity;
            }

            public TaskContext task() { return task; }
            public double step() { return step; }
            public Raycast.Result<?> ray() { return ray; }
            public Vector3 oPosition() { return oPosition; }
            public Vector3 oVelocity() { return oVelocity; }
        }

        public static final class Tick extends TickBased {
            public Tick(TreeNode node, Projectile<?> projectile, boolean local, TaskContext task, double step, Raycast.Result<?> ray, Vector3 oPosition, Vector3 oVelocity) {
                super(node, projectile, local, task, step, ray, oPosition, oVelocity);
            }
        }

        public static final class Hit extends TickBased {
            private final Raycast.Hit<?> hit;
            private OnHit result;

            public Hit(TreeNode node, Projectile<?> projectile, boolean local, TaskContext task, double step, Raycast.Result<?> ray, Vector3 oPosition, Vector3 oVelocity, Raycast.Hit<?> hit, OnHit result) {
                super(node, projectile, local, task, step, ray, oPosition, oVelocity);
                this.hit = hit;
                this.result = result;
            }

            public Raycast.Hit<?> hit() { return hit; }

            public OnHit result() { return result; }
            public void result(OnHit result) { this.result = result; }
        }

        public static final class NoHit extends TickBased {
            public NoHit(TreeNode node, Projectile<?> projectile, boolean local, TaskContext task, double step, Raycast.Result<?> ray, Vector3 oPosition, Vector3 oVelocity) {
                super(node, projectile, local, task, step, ray, oPosition, oVelocity);
            }
        }
    }
}
