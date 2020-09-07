package me.aecsocket.calibre.util.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;

public final class CalibreProtocol {
    // https://wiki.vg/images/thumb/1/13/Inventory-slots.png/300px-Inventory-slots.png
    public static final EnumMap<EquipmentSlot, Integer> SLOT_MAPPING = new Utils.MapInitializer<EquipmentSlot, Integer, EnumMap<EquipmentSlot, Integer>>(new EnumMap<>(EquipmentSlot.class))
            .init(EquipmentSlot.OFF_HAND, 45)
            .init(EquipmentSlot.HEAD, 5)
            .init(EquipmentSlot.CHEST, 6)
            .init(EquipmentSlot.LEGS, 7)
            .init(EquipmentSlot.FEET, 8)
            .get();

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
     * Converts a Minecraft protocol slot to a Bukkit slot.
     * @param slot The Minecraft protocol slot.
     * @return The Bukkit slot.
     */
    public static int toBukkit(int slot) {
        if (slot >= 36 && slot <= 44) return slot - 36;
        return slot;
    }
}
