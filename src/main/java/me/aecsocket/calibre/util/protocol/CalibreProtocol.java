package me.aecsocket.calibre.util.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Utils;
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
     * @param slot The Minecraft protocol slot. Use {@link CalibreProtocol#toProtocol(int)}.
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
     * Converts a Bukkit inventory slot to a Minecraft protocol slot.
     * @param bukkitSlot The Bukkit slot.
     * @return The Minecraft protocol slot.
     * @throws IllegalArgumentException If the slot cannot be handled by the converter.
     */
    public static int toProtocol(int bukkitSlot) throws IllegalArgumentException {
        if (bukkitSlot < 0) throw new IllegalArgumentException("Cannot handle slots below 0");
        if (bukkitSlot <= 8) return 36 + bukkitSlot;
        if (bukkitSlot > 35) throw new IllegalArgumentException("Cannot handle slots above 35");
        return bukkitSlot;
    }

    /**
     * Gets the Minecraft protocol slot from an {@link EquipmentSlot} of a {@link Player}.
     * @param player The player.
     * @param slot The EquipmentSlot.
     * @return The Minecraft protocol slot.
     */
    public static int toProtocol(Player player, EquipmentSlot slot) {
        PlayerInventory inv = player.getInventory();
        if (slot == EquipmentSlot.HAND) return inv.getHeldItemSlot();
        return SLOT_MAPPING.get(slot);
    }
}
