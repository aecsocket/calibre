package me.aecsocket.calibre.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class CalibreProtocol {
    private CalibreProtocol() {}

    private static CalibrePlugin plugin() { return CalibrePlugin.getInstance(); }

    public static void fov(Player target, double fov) {
        CalibrePlugin plugin = plugin();
        PacketContainer packet = plugin.getProtocol().createPacket(PacketType.Play.Server.ABILITIES);
        packet.getBooleans().write(0, target.isInvulnerable());
        packet.getBooleans().write(1, target.isFlying());
        packet.getBooleans().write(2, target.getAllowFlight());
        packet.getBooleans().write(3, target.getGameMode() == GameMode.CREATIVE);
        packet.getFloat().write(0, target.getFlySpeed() / 2);
        packet.getFloat().write(1, (float) fov);
        plugin.sendPacket(packet, target);
    }
}
