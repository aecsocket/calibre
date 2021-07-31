package com.gitlab.aecsocket.calibre.paper.projectile;

import com.gitlab.aecsocket.calibre.core.projectile.ProjectileLaunchSystem;
import com.gitlab.aecsocket.calibre.core.projectile.ProjectileProvider;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.Validation;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
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
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.ParticlesStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;

public final class PaperProjectileLaunchSystem extends ProjectileLaunchSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put(ProjectileLaunchSystem.STATS)
            .put("launch_particles", particlesStat())
            .put("launch_sounds_indoors", soundsStat())
            .put("launch_sounds_outdoors", soundsStat())

            .put("launch_light", intStat())
            .put("launch_light_remove_after", longStat())
            .put("entity_awareness", doubleStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofBoth(STATS, RULES);

    private static final String keyAvailableAt = "available_at";
    private static final String keyIndoorThreshold = "indoor_threshold";
    private static final String keyOutdoorThreshold = "outdoor_threshold";

    public final class Instance extends ProjectileLaunchSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent, long availableAt) {
            super(parent, availableAt);
        }

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperProjectileLaunchSystem base() { return PaperProjectileLaunchSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        protected void launchProjectiles(ProjectileProvider provider, ItemUser user, Vector3 position, Vector3 direction) {
            super.launchProjectiles(provider, user, position, direction);
            if (!(user instanceof PaperUser paper))
                return;
            Location loc = PaperUtils.toBukkit(position, paper.location().getWorld());

            float skyLight = paper.location().getBlock().getLightFromSky();
            float fac = Numbers.clamp01((skyLight - indoorThreshold) / (outdoorThreshold - indoorThreshold));

            parent.stats().<List<PreciseSound>>val("launch_sounds_outdoors")
                    .ifPresent(v -> v.forEach(s -> s.volume(s.volume() * fac).play(loc)));
            parent.stats().<List<PreciseSound>>val("launch_sounds_indoors")
                    .ifPresent(v -> v.forEach(s -> s.volume(s.volume() * (1 - fac)).play(loc)));
            parent.stats().<List<Particles>>val("launch_particles")
                    .ifPresent(v -> v.forEach(p -> p.spawn(loc)));

            parent.stats().<Integer>val("launch_light").ifPresent(light -> {
                var lightData = (Light) Material.LIGHT.createBlockData();
                lightData.setLevel(light);

                Location cLoc = loc.clone();
                for (Player player : paper.location().getWorld().getPlayers()) {
                    // TODO change this to only target near players
                    player.sendBlockChange(cLoc, lightData);
                }

                calibre.paperScheduler().run(Task.single(
                        ctx -> cLoc.getBlock().getState().update(),
                        parent.stats().<Long>val("launch_light_remove_after").orElse((long) Ticks.MSPT)));
            });

            if (user instanceof LivingEntityUser living && (!(user instanceof PlayerUser player) || player.handle().getGameMode() == GameMode.CREATIVE)) {
                calibre.paperScheduler().run(Task.single(ctx -> {
                    parent.stats().<Double>val("entity_awareness").ifPresent(radius -> {
                        for (Entity entity : paper.location().getNearbyEntities(radius, radius, radius)) {
                            if (entity instanceof Monster monster && monster.getTarget() == null)
                                monster.setTarget(living.handle());
                        }
                    });
                }));
            }
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
            data.set(platform.key(keyAvailableAt), PersistentDataType.LONG, availableAt);
            return data;
        }

        @Override
        public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            node.node(keyAvailableAt).set(availableAt);
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;
    private final int indoorThreshold;
    private final int outdoorThreshold;

    public PaperProjectileLaunchSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority, @Nullable InputMapper inputs,
                                       int indoorThreshold, int outdoorThreshold) {
        super(listenerPriority, inputs);
        this.platform = platform;
        this.calibre = calibre;
        this.indoorThreshold = indoorThreshold;
        this.outdoorThreshold = outdoorThreshold;
        Validation.in("indoorThreshold", indoorThreshold, 0, outdoorThreshold);
        Validation.in("outdoorThreshold", outdoorThreshold, indoorThreshold, 15);
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }
    public int indoorThreshold() { return indoorThreshold; }
    public int outdoorThreshold() { return outdoorThreshold; }

    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node,
                data.getOrDefault(platform.key(keyAvailableAt), PersistentDataType.LONG, 0L));
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node,
                cfg.node(keyAvailableAt).getLong());
    }

    public static ConfigType type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new PaperProjectileLaunchSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt(),
                null,
                cfg.node(keyIndoorThreshold).getInt(),
                cfg.node(keyOutdoorThreshold).getInt());
    }
}
