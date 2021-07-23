package com.gitlab.aecsocket.calibre.paper;

import com.comphenix.protocol.PacketType;
import com.gitlab.aecsocket.calibre.core.gun.Sight;
import com.gitlab.aecsocket.calibre.core.gun.SightManagerSystem;
import com.gitlab.aecsocket.calibre.core.gun.SightsSystem;
import com.gitlab.aecsocket.calibre.paper.gun.PaperSight;
import com.gitlab.aecsocket.calibre.paper.gun.PaperSightManagerSystem;
import com.gitlab.aecsocket.calibre.paper.gun.PaperSightsSystem;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.serializers.ProxySerializer;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CalibrePlugin extends BasePlugin<CalibrePlugin> implements Listener {
    /** The ID for this plugin on https://bstats.org. */
    public static final int BSTATS_ID = 10479;

    private final Map<UUID, PlayerData> playerData = new HashMap<>();

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(this, this);
        SokolPlugin sokol = SokolPlugin.instance();
        sokol
                .registerSystemType(SightManagerSystem.ID, PaperSightManagerSystem.type(sokol, this))
                .registerSystemType(SightsSystem.ID, PaperSightsSystem.type(sokol));
        sokol.configOptionInitializer((serializers, mapper) -> serializers
                .registerExact(Sight.class, new ProxySerializer<>(new TypeToken<PaperSight>() {})));
        sokol.schedulers().paperScheduler().run(Task.repeating(ctx -> {
            for (var data : playerData.values())
                data.paperTick(ctx);
        }, Ticks.MSPT));
    }

    @Override
    public void onDisable() {
        for (PlayerData data : playerData.values())
            data.disable();
    }

    public Map<UUID, PlayerData> playerData() { return playerData; }
    public PlayerData playerData(Player player) { return playerData.computeIfAbsent(player.getUniqueId(), u -> new PlayerData(this, player)); }

    public void zoom(Player player, float zoom) {
        protocol.send(player, PacketType.Play.Server.ABILITIES, packet -> {
            packet.getBooleans().write(0, player.isInvulnerable());
            packet.getBooleans().write(1, player.isFlying());
            packet.getBooleans().write(2, player.getAllowFlight());
            packet.getBooleans().write(3, player.getGameMode() == GameMode.CREATIVE);
            packet.getFloat().write(0, player.getFlySpeed());
            packet.getFloat().write(1, zoom);
        });
    }

    @EventHandler
    private void event(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
