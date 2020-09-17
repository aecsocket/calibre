package me.aecsocket.calibre.defaults.gun;

import me.aecsocket.calibre.defaults.service.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A system which stores and manipulates chamber components (components with a {@link ProjectileProviderSystem}).
 */
public interface AmmoProviderSystem {
    /**
     * Gets the chamber systems remaining, with the last entry being the next to be fired.
     * @return The chamber systems,
     */
    List<CalibreComponent> getAmmo();

    /**
     * If {@link AmmoProviderSystem#hasNext()}, removes and returns the last entry in the ammo list.
     * Otherwise returns null.
     * @return The result.
     */
    @Nullable CalibreComponent pop();

    /**
     * If {@link AmmoProviderSystem#hasNext()}, returns the last entry in the ammo list.
     * Otherwise returns null.
     * @return The result.
     */
    @Nullable CalibreComponent peek();

    /**
     * Adds a chamber to the end of the ammo list.
     * @param chamber The chamber to add.
     */
    void add(CalibreComponent chamber);

    /**
     * Gets how much ammo is remaining.
     * @return The amount.
     */
    default int getAmmoAmount() { return getAmmo().size(); }

    /**
     * Returns true if the ammo list has more elements.
     * @return The result.
     */
    default boolean hasNext() { return getAmmoAmount() > 0; }
}
