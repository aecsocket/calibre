package com.gitlab.aecsocket.calibre.paper.fire;

import com.gitlab.aecsocket.calibre.core.projectile.ProjectileLaunchSystem;
import com.gitlab.aecsocket.calibre.core.projectile.ProjectileProvider;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.paper.stat.ParticlesStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;

public final class PaperProjectileLaunchSystem extends ProjectileLaunchSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put(ProjectileLaunchSystem.STATS)
            .put("launch_particles", particlesStat())
            .put("launch_sounds_indoors", soundsStat())
            .put("launch_sounds_outdoors", soundsStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofBoth(STATS, RULES);

    public final class Instance extends ProjectileLaunchSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperProjectileLaunchSystem base() { return PaperProjectileLaunchSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        protected void launchProjectile(ProjectileProvider provider, ItemUser user, Vector3 position, Vector3 velocity) {
            super.launchProjectile(provider, user, position, velocity);
            if (!(user instanceof PaperUser paper))
                return;
            Location loc = PaperUtils.toBukkit(position, paper.location().getWorld());
            parent.stats().<List<Particles>>desc("launch_particles")
                    .ifPresent(v -> v.forEach(p -> p.spawn(loc)));
            parent.stats().<List<PreciseSound>>desc("launch_sounds_" + (loc.getBlock().getLightFromSky() >= 12 ? "outdoors" : "indoors"))
                    .ifPresent(v -> v.forEach(s -> s.play(loc)));
        }

        @Override
        protected void recoil(ItemUser user, Vector2 recoil, double speed, double recovery, double recoverySpeed, long recoveryAfter) {
            if (!(user instanceof PlayerUser player))
                return;
            calibre.playerData(player.handle()).applyRecoil(recoil, speed, recovery, recoverySpeed, recoveryAfter);
        }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            return data;
        }

        @Override
        public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;

    public PaperProjectileLaunchSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority, @Nullable InputMapper inputs) {
        super(listenerPriority, inputs);
        this.platform = platform;
        this.calibre = calibre;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }

    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

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
        return cfg -> new PaperProjectileLaunchSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt(),
                null);
    }
}
