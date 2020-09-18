package me.aecsocket.calibre.util.itemuser;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Vector2;

/**
 * Represents an {@link ItemUser} which can use a gun.
 */
public interface GunItemUser extends ItemUser {
    /**
     * Sets and saves the amount of spread on this user.
     * @param spread The amount of spread.
     */
    void setSpread(double spread);

    /**
     * Gets the saved amount of spread on this user.
     * @return The amount of spread.
     */
    double getSpread();

    /**
     * Applies the specified amount of camera recoil to the user.
     * @param recoil The amount of recoil.
     * @param recoilSpeed How fast it takes for recoil to take effect (0.0 - 1.0)
     * @param recoverAfter The time in milliseconds until recoil recovery can start.
     * @param recoilRecovery The percentage of how much to move the camera back to its origin after recoil has been applied.
     * @param recoilRecoverySpeed How fast it takes for recoil to recover once started (0.0 - 1.0)
     */
    void applyRecoil(Vector2 recoil, double recoilSpeed, long recoverAfter, double recoilRecovery, double recoilRecoverySpeed);
}
