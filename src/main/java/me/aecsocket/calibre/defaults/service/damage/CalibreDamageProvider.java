package me.aecsocket.calibre.defaults.service.damage;

import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CalibreDamageProvider implements CalibreDamageService {
    private final CalibrePlugin plugin;

    public CalibreDamageProvider(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    @Override
    public void damage(LivingEntity damager, Entity victim, double damage, Vector position, ItemStack item) {
        if (victim instanceof LivingEntity) {
            LivingEntity livingVictim = (LivingEntity) victim;
            livingVictim.damage(0, damager);
            double health = livingVictim.getHealth() - damage;
            if (health <= 0)
                livingVictim.setHealth(0);
            else {
                livingVictim.setHealth(health);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> livingVictim.setVelocity(new Vector()), 1);
            }
        }
    }
}
