package com.github.aecsocket.calibre.paper.projectile;

import com.github.aecsocket.calibre.core.projectile.BulletSystem;
import com.github.aecsocket.calibre.core.projectile.Projectile;
import com.github.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatMap;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.stat.EffectsStat;
import com.gitlab.aecsocket.sokol.paper.stat.SoundsStat;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.EffectsStat.*;

public final class PaperBulletSystem extends BulletSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public static final SDouble STAT_PASS_RADIUS = doubleStat("pass_radius");
    public static final SoundsStat STAT_PASS_SOUNDS = soundsStat("pass_sounds");
    public static final EffectsStat STAT_PASS_EFFECTS = effectsStat("pass_effects");

    public static final StatTypes STATS = StatTypes.builder()
            .add(BulletSystem.STATS)
            .add(STAT_PASS_RADIUS, STAT_PASS_SOUNDS, STAT_PASS_EFFECTS)
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(ID, STATS);

    public final class Instance extends BulletSystem.Instance implements PaperSystem.Instance {
        private double passRadius;
        private List<PreciseSound> passSounds;
        private List<PotionEffect> passEffects;
        private Set<Player> passed;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperBulletSystem base() { return PaperBulletSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build(StatLists stats) {
            super.build(stats);
            parent.events().register(Projectile.Events.Tick.class, this::event, listenerPriority);
        }

        @Override
        protected double hardness(Projectile.Events.Hit event) {
            if (!(event.hit().hit() instanceof PaperRaycast.PaperBoundable paper))
                return 0;
            if (paper.entity() != null)
                return calibre.materials().entityHardness(paper.name());
            if (paper.block() != null)
                return calibre.materials().blockHardness(paper.name(), paper.block().getType());
            return 0;
        }

        @Override
        protected void damage(Projectile.Events.Hit event, double damage) {
            if (!(event.hit().hit() instanceof PaperRaycast.PaperBoundable paper))
                return;
            if (paper.entity() instanceof LivingEntity living) {
                if (living.isInvulnerable())
                    return;
                // TODO locational damage
                living.damage(1e-6, event.projectile() instanceof PaperProjectile proj ? proj.shooter() : null);
                living.setVelocity(new Vector());
                living.setHealth(Math.max(0, living.getHealth() - damage));
                living.setNoDamageTicks(0);
            }
            if (paper.block() != null) {
                // TODO block cracks
            }
        }

        @Override
        protected void event(Projectile.Events.Create event) {
            super.event(event);
            StatMap stats = event.projectile().fullTree().stats();
            stats.val(STAT_PASS_RADIUS).ifPresent(radius -> {
                passRadius = radius;
                passSounds = stats.val(STAT_PASS_SOUNDS).orElse(null);
                passEffects = stats.val(STAT_PASS_EFFECTS).orElse(null);
                passed = new HashSet<>();
            });
        }

        protected void event(Projectile.Events.Tick event) {
            if (!event.local())
                return;
            if (!(event.projectile() instanceof PaperProjectile projectile))
                return;
            if (passRadius > 0) {
                Location loc = PaperUtils.toBukkit(event.oPosition(), projectile.world());
                Vector step = PaperUtils.toBukkit(event.oVelocity().normalize().multiply(passRadius));
                for (double f = 0; f < event.ray().distance(); f += passRadius) {
                    for (Player player : loc.getNearbyPlayers(passRadius)) {
                        if (player.equals(projectile.shooter()) || passed.contains(player))
                            continue;
                        passed.add(player);
                        if (passSounds != null) passSounds.forEach(s -> s.play(player, loc));
                        if (passEffects != null && !player.isInvulnerable()) player.addPotionEffects(passEffects);
                    }
                }
            }
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;

    public PaperBulletSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority) {
        super(listenerPriority);
        this.platform = platform;
        this.calibre = calibre;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }

    @Override public String id() { return ID; }
    @Override public StatTypes statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new PaperBulletSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt());
    }
}
