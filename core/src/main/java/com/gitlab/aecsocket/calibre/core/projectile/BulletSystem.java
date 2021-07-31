package com.gitlab.aecsocket.calibre.core.projectile;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;

import java.util.HashMap;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.doubleStat;

public abstract class BulletSystem extends AbstractSystem {
    public static final String ID = "bullet";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("damage", doubleStat())
            .put("penetration", doubleStat())
            .put("damage_dropoff", doubleStat())
            .put("damage_range", doubleStat())
            .build();

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
            damage = stats.<Double>req("damage");
            baseDamage = damage;
            penetration = stats.<Double>req("penetration");
            damageDropoff = stats.<Double>val("damage_dropoff").orElse(Double.MAX_VALUE);
            damageRange = stats.<Double>val("damage_range").orElse(Double.MAX_VALUE);
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
    @Override public Map<String, Stat<?>> statTypes() { return STATS; }
}
