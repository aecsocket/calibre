package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.world.user.ItemUser;
import org.bukkit.inventory.Inventory;

public interface InventoryUser extends ItemUser {
    Inventory inventory();
}
