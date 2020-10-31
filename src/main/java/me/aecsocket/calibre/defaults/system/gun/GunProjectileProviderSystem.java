package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.defaults.system.projectile.ProjectileProviderSystem;
import org.bukkit.inventory.ItemStack;

public interface GunProjectileProviderSystem extends ProjectileProviderSystem {
    String getPrefix();
    String getIcon();

    ItemStack createEjection();
}
