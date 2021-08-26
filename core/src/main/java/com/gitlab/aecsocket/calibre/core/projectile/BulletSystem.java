package com.gitlab.aecsocket.calibre.core.projectile;

import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatMap;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;

public abstract class BulletSystem extends AbstractSystem {
    public static final String ID = "bullet";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public static final SDouble STAT_DAMAGE = doubleStat("damage");
    public static final SDouble STAT_PENETRATION = doubleStat("penetration");
    public static final SDouble STAT_DAMAGE_DROPOFF = doubleStat("damage_dropoff");
    public static final SDouble STAT_DAMAGE_RANGE = doubleStat("damage_range");

    public static final StatTypes STATS = StatTypes.of(STAT_DAMAGE, STAT_PENETRATION, STAT_DAMAGE_DROPOFF, STAT_DAMAGE_RANGE);

    public abstract class Instance extends AbstractSystem.Instance {
        private double baseDamage;
        private double damage;
        private double penetration;
        private double damageDropoff;
        private double damageRange;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract BulletSystem base();

        @Override
        public void build(StatLists stats) {
            parent.events().register(Projectile.Events.Create.class, this::event, listenerPriority);
            parent.events().register(Projectile.Events.Hit.class, this::event, listenerPriority);
        }

        protected void event(Projectile.Events.Create event) {
            if (!event.local())
                return;
            // at this point, `this` is detached from the full tree, so we need the full tree's stats
            StatMap stats = event.projectile().fullTree().stats();
            damage = stats.req(STAT_DAMAGE);
            baseDamage = damage;
            penetration = stats.req(STAT_PENETRATION);
            damageDropoff = stats.val(STAT_DAMAGE_DROPOFF).orElse(Double.MAX_VALUE);
            damageRange = stats.val(STAT_DAMAGE_RANGE).orElse(Double.MAX_VALUE);
        }

        protected abstract double hardness(Projectile.Events.Hit event);
        protected abstract void damage(Projectile.Events.Hit event, double damage);

        protected void event(Projectile.Events.Hit event) {
            if (!event.local())
                return;
            event.result(Projectile.OnHit.REMOVE);
            double dst = event.projectile().travelled();
            if (dst > damageRange)
                return;

            // TODO ricochet
            double hardness = hardness(event);
            double reduced = hardness / penetration;

            double dmg = damage * (1 - Numbers.clamp01(reduced + Math.max(0, (dst-damageDropoff) / (damageRange-damageDropoff))));
            if (dmg <= 0)
                return;
            damage(event, dmg);
            damage -= baseDamage * reduced;

            event.result(damage <= 0 ? Projectile.OnHit.REMOVE : Projectile.OnHit.PENETRATE);
        }
    }

    public BulletSystem(int listenerPriority) {
        super(listenerPriority);
    }

    @Override public String id() { return ID; }
    @Override public StatTypes statTypes() { return STATS; }
}
