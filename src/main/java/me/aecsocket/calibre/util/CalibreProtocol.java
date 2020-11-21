package me.aecsocket.calibre.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.MapInit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public final class CalibreProtocol {
    private static CalibrePlugin plugin;

    private CalibreProtocol() {}

    public static void initialize(CalibrePlugin plugin) { CalibreProtocol.plugin = plugin; }

    public enum PlayerTeleportFlag {
        X, Y, Z, Y_ROT, X_ROT
    }

    // https://wiki.vg/images/thumb/1/13/Inventory-slots.png/300px-Inventory-slots.png
    public static final EnumMap<EquipmentSlot, Integer> SLOT_MAPPING = MapInit.of(new EnumMap<EquipmentSlot, Integer>(EquipmentSlot.class))
            .init(EquipmentSlot.OFF_HAND, 40)
            .init(EquipmentSlot.HEAD, 39)
            .init(EquipmentSlot.CHEST, 38)
            .init(EquipmentSlot.LEGS, 37)
            .init(EquipmentSlot.FEET, 36)
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
        sendItem(player, item, slotOf(player, slot));
    }

    /**
     * Gets the inventory slot from an {@link EquipmentSlot} of a {@link Player}.
     * @param player The player.
     * @param slot The EquipmentSlot.
     * @return The inventory slot.
     */
    public static int slotOf(Player player, EquipmentSlot slot) {
        PlayerInventory inv = player.getInventory();
        if (slot == EquipmentSlot.HAND) return inv.getHeldItemSlot();
        return SLOT_MAPPING.get(slot);
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
        packet.getFloat().write(0, player.getFlySpeed() / 2);
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

    public static void sendAir(Player player, int air) {
        PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, player.getEntityId());
        packet.getWatchableCollectionModifier().write(0, Collections.singletonList(
                new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(1, WrappedDataWatcher.Registry.get(Integer.class)), air)
        ));
        plugin.sendPacket(player, packet);
    }

    public static void sendAir(Player player, double percentage) {
        sendAir(player, (int) (((Math.round(percentage * 10.0) / 10.0) - 0.05) * player.getMaximumAir()));
    }

    public static void sendFood(Player player, int food) {
        PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_HEALTH);
        packet.getFloat().write(0, (float) player.getHealth());
        packet.getIntegers().write(0, food);
        packet.getFloat().write(1, player.getSaturation());
        plugin.sendPacket(player, packet);
    }

    public static void resetFood(Player player) {
        sendFood(player, player.getFoodLevel());
    }

    public static void showAiming(Entity target, ItemStack item) {
        ProtocolManager protocol = plugin.getProtocolManager();
        int eid = target.getEntityId();

        PacketContainer metaPacket = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metaPacket.getIntegers().write(0, eid);
        metaPacket.getWatchableCollectionModifier().write(0, Collections.singletonList(
                new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(7, WrappedDataWatcher.Registry.get(Byte.class)), (byte) 1) // Aiming bow
        ));

        PacketContainer equipmentPacket = protocol.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        equipmentPacket.getIntegers().write(0, eid);
        equipmentPacket.getSlotStackPairLists().write(0, Collections.singletonList(
                new Pair<>(EnumWrappers.ItemSlot.MAINHAND, item)
        ));

        for (Player player : target.getWorld().getPlayers()) {
            if ((!(target instanceof Player) || player.canSee((Player) target)) && player != target) {
                plugin.sendPacket(player, metaPacket);
                plugin.sendPacket(player, equipmentPacket);
            }
        }
    }
}
