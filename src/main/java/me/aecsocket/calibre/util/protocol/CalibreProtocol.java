package me.aecsocket.calibre.util.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public final class CalibreProtocol {
    public enum PlayerTeleportFlag {
        X, Y, Z, Y_ROT, X_ROT
    }

    // https://wiki.vg/images/thumb/1/13/Inventory-slots.png/300px-Inventory-slots.png
    public static final EnumMap<EquipmentSlot, Integer> SLOT_MAPPING = new Utils.MapInitializer<EquipmentSlot, Integer, EnumMap<EquipmentSlot, Integer>>(new EnumMap<>(EquipmentSlot.class))
            .init(EquipmentSlot.OFF_HAND, 45)
            .init(EquipmentSlot.HEAD, 5)
            .init(EquipmentSlot.CHEST, 6)
            .init(EquipmentSlot.LEGS, 7)
            .init(EquipmentSlot.FEET, 8)
            .get();

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

    private CalibreProtocol() {}

    private static CalibrePlugin plugin;

    public static void initialize(CalibrePlugin plugin) {
        CalibreProtocol.plugin = plugin;
    }

    public static CalibrePlugin getPlugin() { return plugin; }

    /**
     * Send a packet setting an item in the player's inventory.
     * @param player The player to send to.
     * @param item The item to set to.
     * @param slot The item slot.
     */
    public static void sendItem(Player player, ItemStack item, int slot) {
        PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT);
        packet.getIntegers().write(0, -2);
        packet.getIntegers().write(1, slot);
        packet.getItemModifier().write(0, item);
        plugin.sendPacket(player, packet);
    }

    /**
     * Send a packet setting an item in the player's inventory.
     * @param player The player to send to.
     * @param item The item to set to.
     * @param slot The EquipmentSlot to be converted to a protocol slot.
     */
    public static void sendItem(Player player, ItemStack item, EquipmentSlot slot) {
        sendItem(player, item, toProtocol(player, slot));
    }

    /**
     * Gets the inventory slot from an {@link EquipmentSlot} of a {@link Player}.
     * @param player The player.
     * @param slot The EquipmentSlot.
     * @return The inventory slot.
     */
    public static int toProtocol(Player player, EquipmentSlot slot) {
        PlayerInventory inv = player.getInventory();
        if (slot == EquipmentSlot.HAND) return inv.getHeldItemSlot();
        return SLOT_MAPPING.get(slot);
    }

    /**
     * Sends the damage animation for an entity to a player.
     * @param player The player to send to.
     * @param entity The entity to damage.
     */
    public static void damageAnimation(Player player, Entity entity) {
        PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, entity.getEntityId());
        packet.getIntegers().write(1, 1);
        plugin.sendPacket(player, packet);
    }

    /**
     * Sends a packet stating the player's FOV multiplier.
     * <p>
     * 0.1 is default FOV, going up to 1. Values below 0 are stronger than 1, and the closer to 0 the stronger.
     * @param player The player to send to.
     * @param fov The FOV multiplier.
     */
    public static void fovMultiplier(Player player, double fov) {
        PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.ABILITIES);
        packet.getBooleans().write(0, player.isInvulnerable());
        packet.getBooleans().write(1, player.isFlying());
        packet.getBooleans().write(2, player.getAllowFlight());
        packet.getBooleans().write(3, player.getGameMode() == GameMode.CREATIVE);
        packet.getFloat().write(0, player.getFlySpeed());
        packet.getFloat().write(1, (float) fov);
        plugin.sendPacket(player, packet);
    }

    /**
     * Sends a packet resetting the player's FOV multiplier.
     * @param player The player to send to.
     */
    public static void resetFov(Player player) {
        fovMultiplier(player, 0.1);
    }

    /**
     * Rotates a player's camera relative to their current rotation.
     * @param player The player to send to.
     * @param yaw The relative yaw.
     * @param pitch The relative pitch.
     */
    public static void rotateCamera(Player player, double yaw, double pitch) {
        PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.POSITION);
        packet.getDoubles().write(0, 0d);
        packet.getDoubles().write(1, 0d);
        packet.getDoubles().write(2, 0d);
        packet.getFloat().write(0, (float) yaw);
        packet.getFloat().write(1, (float) pitch);
        packet.getSets(TELEPORT_FLAG_CONVERTER).write(0, POSITION_FLAGS);
        plugin.sendPacket(player, packet);
    }


    /**
     * Converts a Minecraft protocol slot to a Bukkit slot.
     * @param slot The Minecraft protocol slot.
     * @return The Bukkit slot.
     */
    public static int toBukkit(int slot) {
        if (slot >= 36 && slot <= 44) return slot - 36;
        return slot;
    }
}
