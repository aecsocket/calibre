package com.gitlab.aecsocket.calibre.paper.wrapper.user;

import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import org.bukkit.inventory.Inventory;

public interface InventoryUser extends ItemUser {
    Inventory inventory();
}
