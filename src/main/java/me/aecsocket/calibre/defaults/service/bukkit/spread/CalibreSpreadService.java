package me.aecsocket.calibre.defaults.service.bukkit.spread;

import org.bukkit.util.Vector;

/**
 * Provides a method to apply randomness to projectile launch directions.
 */
public interface CalibreSpreadService {
    /**
     * Applies random spread to a projectile's launch direction.
     * @param direction The original direction.
     * @param spread The amount of spread to apply.
     * @return The modified direction.
     */
    Vector applySpread(Vector direction, double spread);
}
