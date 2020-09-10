package me.aecsocket.calibre.defaults.service.system;

import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;

/**
 * A service system which allows adding a cooldown to an item's actions.
 */
public interface ActionSystem {
    /**
     * Gets if the cooldown for an item has expired.
     * @return The result.
     */
    boolean isAvailable();

    /**
     * Adds a cooldown for an item.
     * @param delay The cooldown delay, in milliseconds.
     */
    void availableIn(long delay);

    void startAction(Long delay,
                     Location location, SoundData[] sound, ParticleData[] particles, Object particleData,
                     LivingEntity entity, EquipmentSlot slot, Animation animation);

    default void startAction(Long delay,
                     Location location, SoundData[] sound, ParticleData[] particles,
                     LivingEntity entity, EquipmentSlot slot, Animation animation) {
        startAction(delay,
                location, sound, particles, null,
                entity, slot, animation);
    }

    default void startAction(Long delay,
                     Location location, SoundData[] sound, ParticleData[] particles, Object particleData) {
        startAction(delay,
                location, sound, particles, particleData,
                null, null, null);
    }

    default void startAction(Long delay,
                     Location location, SoundData[] sound, ParticleData[] particles) {
        startAction(delay,
                location, sound, particles, null);
    }

    default void startAction(Long delay) {
        startAction(delay,
                null, null, null);
    }
}
