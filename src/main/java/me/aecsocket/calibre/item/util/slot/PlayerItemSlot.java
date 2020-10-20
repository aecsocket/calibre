package me.aecsocket.calibre.item.util.slot;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PlayerItemSlot implements InventoryItemSlot {
    private final Player player;
    private final int inventorySlot;

    public PlayerItemSlot(Player player, int inventorySlot) {
        this.player = player;
        this.inventorySlot = inventorySlot;
    }

    public Player getPlayer() { return player; }

    @Override public Inventory getInventory() { return player.getInventory(); }
    @Override public int getInventorySlot() { return inventorySlot; }
}
