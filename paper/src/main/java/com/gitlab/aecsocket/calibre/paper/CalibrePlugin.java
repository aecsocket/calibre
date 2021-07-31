package com.gitlab.aecsocket.calibre.paper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.gitlab.aecsocket.calibre.core.projectile.BulletSystem;
import com.gitlab.aecsocket.calibre.core.projectile.ProjectileLaunchSystem;
import com.gitlab.aecsocket.calibre.core.mode.Mode;
import com.gitlab.aecsocket.calibre.core.mode.ModeManagerSystem;
import com.gitlab.aecsocket.calibre.core.mode.ModesSystem;
import com.gitlab.aecsocket.calibre.core.sight.Sight;
import com.gitlab.aecsocket.calibre.core.sight.SightManagerSystem;
import com.gitlab.aecsocket.calibre.core.sight.SightsSystem;
import com.gitlab.aecsocket.calibre.paper.projectile.PaperBulletSystem;
import com.gitlab.aecsocket.calibre.paper.projectile.PaperProjectileLaunchSystem;
import com.gitlab.aecsocket.calibre.paper.projectile.ProjectileProviderSystem;
import com.gitlab.aecsocket.calibre.paper.mode.PaperMode;
import com.gitlab.aecsocket.calibre.paper.mode.PaperModeManagerSystem;
import com.gitlab.aecsocket.calibre.paper.mode.PaperModesSystem;
import com.gitlab.aecsocket.calibre.paper.sight.PaperSight;
import com.gitlab.aecsocket.calibre.paper.sight.PaperSightManagerSystem;
import com.gitlab.aecsocket.calibre.paper.sight.PaperSightsSystem;
import com.gitlab.aecsocket.calibre.paper.sight.SwayStabilizerSystem;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.bounds.Compound;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.scheduler.ThreadScheduler;
import com.gitlab.aecsocket.minecommons.core.serializers.ProxySerializer;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolLibAPI;
import com.gitlab.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.gitlab.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class CalibrePlugin extends BasePlugin<CalibrePlugin> implements Listener {
    /** The ID for this plugin on https://bstats.org. */
    public static final int BSTATS_ID = 10479;

    private final PaperScheduler paperScheduler = new PaperScheduler(this);
    private final ThreadScheduler threadScheduler = new ThreadScheduler(Executors.newSingleThreadExecutor());
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private final Materials materials = new Materials(this);
    private PaperRaycast.Builder raycast;

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(this, this);
        SokolPlugin sokol = SokolPlugin.instance();
        sokol
                .registerSystemType(SightManagerSystem.ID, PaperSightManagerSystem.type(sokol, this), () -> PaperSightManagerSystem.LOAD_PROVIDER)
                .registerSystemType(SightsSystem.ID, PaperSightsSystem.type(sokol), () -> PaperSightsSystem.LOAD_PROVIDER)
                .registerSystemType(SwayStabilizerSystem.ID, SwayStabilizerSystem.type(sokol, this), () -> SwayStabilizerSystem.LOAD_PROVIDER)
                .registerSystemType(ModeManagerSystem.ID, PaperModeManagerSystem.type(sokol, this), () -> PaperModeManagerSystem.LOAD_PROVIDER)
                .registerSystemType(ModesSystem.ID, PaperModesSystem.type(sokol), () -> PaperModesSystem.LOAD_PROVIDER)
                .registerSystemType(ProjectileLaunchSystem.ID, PaperProjectileLaunchSystem.type(sokol, this), () -> PaperProjectileLaunchSystem.LOAD_PROVIDER)
                .registerSystemType(ProjectileProviderSystem.ID, ProjectileProviderSystem.type(sokol, this), () -> ProjectileProviderSystem.LOAD_PROVIDER)
                .registerSystemType(BulletSystem.ID, PaperBulletSystem.type(sokol, this), () -> PaperBulletSystem.LOAD_PROVIDER);
        sokol.configOptionInitializer((serializers, mapper) -> serializers
                .registerExact(Sight.class, new ProxySerializer<>(new TypeToken<PaperSight>() {}))
                .registerExact(Mode.class, new ProxySerializer<>(new TypeToken<PaperMode>() {})));

        paperScheduler.run(Task.repeating(ctx -> {
            for (var data : playerData.values()) {
                synchronized (data) {
                    data.paperTick(ctx);
                }
            }
        }, Ticks.MSPT));
        threadScheduler.run(Task.repeating(ctx -> {
            for (var data : playerData.values()) {
                synchronized (data) {
                    data.threadTick(ctx);
                }
            }
        }, 10));

        // TODO configs for raycaster
        raycast = PaperRaycast.builder();
        raycast.blockBound(Material.BARRIER, e -> true, raycast.boundsBuilder()
                .put(Material.BARRIER.getKey().value(), Compound.compound())
                .get());
    }

    @Override
    public void onDisable() {
        for (PlayerData data : playerData.values())
            data.disable();
    }

    @Override
    public void load() {
        super.load();
        materials.load();
    }

    public PaperScheduler paperScheduler() { return paperScheduler; }
    public PlayerData playerData(Player player) { return playerData.computeIfAbsent(player.getUniqueId(), u -> new PlayerData(this, player)); }
    public Materials materials() { return materials; }

    private enum PlayerTeleportFlag {
        X, Y, Z, Y_ROT, X_ROT
    }

    private static final Set<PlayerTeleportFlag> positionFlags = new HashSet<>(Arrays.asList(
            PlayerTeleportFlag.X,
            PlayerTeleportFlag.Z,
            PlayerTeleportFlag.Y,
            PlayerTeleportFlag.X_ROT,
            PlayerTeleportFlag.Y_ROT
    ));

    private static final EquivalentConverter<PlayerTeleportFlag> teleportFlagConverter = EnumWrappers.getGenericConverter(MinecraftReflection
            .getMinecraftClass("network.protocol.game.EnumPlayerTeleportFlags",
                    "network.protocol.game.PacketPlayOutPosition$EnumPlayerTeleportFlags"), PlayerTeleportFlag.class);

    public void zoom(Player player, float zoom) {
        protocol.send(player, PacketType.Play.Server.ABILITIES, packet -> {
            packet.getBooleans().write(0, player.isInvulnerable());
            packet.getBooleans().write(1, player.isFlying());
            packet.getBooleans().write(2, player.getAllowFlight());
            packet.getBooleans().write(3, player.getGameMode() == GameMode.CREATIVE);
            packet.getFloat().write(0, player.getFlySpeed() / 2);
            packet.getFloat().write(1, zoom);
        });
    }

    public void rotate(Player player, double yaw, double pitch) {
        protocol.send(player, PacketType.Play.Server.POSITION, packet -> {
            packet.getDoubles().write(0, 0d);
            packet.getDoubles().write(1, 0d);
            packet.getDoubles().write(2, 0d);
            packet.getFloat().write(0, (float) yaw);
            packet.getFloat().write(1, (float) pitch);
            packet.getSets(teleportFlagConverter).write(0, positionFlags);
        }, true);
    }

    public void air(Player player, double percent) {
        int air = (int) (((Math.round(percent * 10d) / 10d) - 0.05) * player.getMaximumAir());
        protocol.send(player, PacketType.Play.Server.ENTITY_METADATA, packet -> {
            packet.getIntegers().write(0, player.getEntityId());
            packet.getWatchableCollectionModifier().write(0, ProtocolLibAPI.watcherObjects()
                    .add(1, // air remaining
                            Integer.class, air)
                    .get()
            );
        });
    }

    public PaperRaycast raycast(World world) {
        return raycast.build(world);
    }

    @EventHandler
    private void event(PlayerQuitEvent event) {
        playerData(event.getPlayer()).disable();
        playerData.remove(event.getPlayer().getUniqueId());
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
