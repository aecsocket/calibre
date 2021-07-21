package com.gitlab.aecsocket.calibre.paper;

import com.comphenix.protocol.PacketType;
import com.gitlab.aecsocket.calibre.core.gun.SightManagerSystem;
import com.gitlab.aecsocket.calibre.core.gun.SightsSystem;
import com.gitlab.aecsocket.calibre.paper.gun.PaperSightManagerSystem;
import com.gitlab.aecsocket.calibre.paper.gun.PaperSightsSystem;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    /** The ID for this plugin on https://bstats.org. */
    public static final int BSTATS_ID = 10479;

    @Override
    public void onEnable() {
        super.onEnable();
        SokolPlugin sokol = SokolPlugin.instance();
        sokol
                .registerSystemType(SightManagerSystem.ID, PaperSightManagerSystem.type(sokol, this))
                .registerSystemType(SightsSystem.ID, PaperSightsSystem.type(sokol));
    }

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

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
