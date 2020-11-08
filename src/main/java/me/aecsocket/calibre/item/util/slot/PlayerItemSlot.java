package me.aecsocket.calibre.item.util.slot;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerItemSlot that = (PlayerItemSlot) o;
        return inventorySlot == that.inventorySlot &&
                player.equals(that.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, inventorySlot);
    }
}
