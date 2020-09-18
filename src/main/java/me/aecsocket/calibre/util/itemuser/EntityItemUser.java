package me.aecsocket.calibre.util.itemuser;

import org.bukkit.entity.Entity;

/**
 * Represents an {@link ItemUser} which is attached to a Bukkit {@link Entity}.
 */
public interface EntityItemUser extends ItemUser {
    Entity getEntity();
}
