package me.aecsocket.calibre.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CalibreProtocol {
    public static final int NO_SPRINT_FOOD = 6;

    private CalibreProtocol() {}

    private static CalibrePlugin plugin() { return CalibrePlugin.getInstance(); }

    public enum PlayerTeleportFlag {
        X, Y, Z, Y_ROT, X_ROT
    }

    public static final Set<PlayerTeleportFlag> POSITION_FLAGS = new HashSet<>(Arrays.asList(
            PlayerTeleportFlag.X,
            PlayerTeleportFlag.Z,
            PlayerTeleportFlag.Y,
            PlayerTeleportFlag.X_ROT,
            PlayerTeleportFlag.Y_ROT
    ));
    public static final EquivalentConverter<PlayerTeleportFlag> TELEPORT_FLAG_CONVERTER = EnumWrappers.getGenericConverter(MinecraftReflection
            .getMinecraftClass("EnumPlayerTeleportFlags",
                    "PacketPlayOutPosition$EnumPlayerTeleportFlags"), PlayerTeleportFlag.class);


    /*
    -1-0: zoomed out (0.05 - 0.1)
    0-1: zoomed in
      0-0.5: positive
      0.5-1: negative
     */
    public static double toProtocol(double fov) {
        return fov;
    }

    public static void fov(Player target, double fov) {
        CalibrePlugin plugin = plugin();
        PacketContainer packet = plugin.protocol().createPacket(PacketType.Play.Server.ABILITIES);
        packet.getBooleans().write(0, target.isInvulnerable());
        packet.getBooleans().write(1, target.isFlying());
        packet.getBooleans().write(2, target.getAllowFlight());
        packet.getBooleans().write(3, target.getGameMode() == GameMode.CREATIVE);
        packet.getFloat().write(0, target.getFlySpeed() / 2);
        packet.getFloat().write(1, (float) toProtocol(fov));
        plugin.sendPacket(packet, target);
    }

    public static void food(Player target, int amount) {
        CalibrePlugin plugin = plugin();
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.UPDATE_HEALTH);
        packet.getFloat().write(0, (float) target.getHealth());
        packet.getIntegers().write(0, amount);
        packet.getFloat().write(1, target.getSaturation());
        plugin.sendPacket(packet, target);
    }

    public static void allowSprint(Player target, boolean allow) {
        if (allow)
            food(target, target.getFoodLevel());
        else if (target.getFoodLevel() > NO_SPRINT_FOOD)
            food(target, NO_SPRINT_FOOD);
    }

    public static void rotate(Player target, double yaw, double pitch) {
        CalibrePlugin plugin = plugin();
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.POSITION);
        packet.getDoubles().write(0, 0d);
        packet.getDoubles().write(1, 0d);
        packet.getDoubles().write(2, 0d);
        packet.getFloat().write(0, (float) yaw);
        packet.getFloat().write(1, (float) pitch);
        packet.getSets(TELEPORT_FLAG_CONVERTER).write(0, POSITION_FLAGS);
        plugin.sendPacket(packet, target);
    }
}
