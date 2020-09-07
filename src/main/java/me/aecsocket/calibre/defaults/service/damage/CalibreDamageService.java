package me.aecsocket.calibre.defaults.service.damage;

import me.aecsocket.calibre.item.ItemEvents;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Provides a method to damage entities.
 */
public interface CalibreDamageService {
    /**
     * Damages an entity by a damager at a position, along with an {@link ItemEvents.Damage}.
     * @param damager The damager.
     * @param victim The victim.
     * @param damage The amount to damage the victim.
     * @param position The position that the victim was hit at.
     * @param item The item that was used.
     */
    void damage(LivingEntity damager, Entity victim, double damage, Vector position, ItemStack item);
}
