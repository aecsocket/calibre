package me.aecsocket.calibre.defaults.system.gun.projectile;

import me.aecsocket.calibre.defaults.system.projectile.ProjectileProviderSystem;
import org.bukkit.inventory.ItemStack;

public interface GunProjectileProviderSystem extends ProjectileProviderSystem {
    String getPrefix();
    String getIcon();

    ItemStack createEjection();
}
