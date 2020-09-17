package me.aecsocket.calibre.defaults.service.bukkit.damage;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.calibre.util.itemuser.LivingEntityItemUser;
import me.aecsocket.calibre.util.protocol.CalibreProtocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CalibreDamageProvider implements CalibreDamageService {
    private final CalibrePlugin plugin;

    public CalibreDamageProvider(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    @Override
    public void damage(ItemUser damager, Entity victim, double damage, Vector position, ItemStack item) {
        if (victim == null) return;
        if (victim.isDead()) return;
        if (victim instanceof LivingEntity) {
            LivingEntity livingVictim = (LivingEntity) victim;
            LivingEntity eDamager = damager instanceof LivingEntityItemUser ? ((LivingEntityItemUser) damager).getEntity() : null;
            if (eDamager == victim) {
                livingVictim.damage(0);
                if (victim instanceof Player)
                    CalibreProtocol.damageAnimation((Player) victim, victim);
            } else {
                livingVictim.damage(0, eDamager);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> livingVictim.setVelocity(new Vector()), 1);
            }

            livingVictim.setHealth(Math.max(0, livingVictim.getHealth() - damage));
        }
    }
}
