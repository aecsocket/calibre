package me.aecsocket.calibre.defaults.service;

import me.aecsocket.calibre.item.util.damagecause.DamageCause;
import me.aecsocket.calibre.item.util.user.EntityItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public interface CalibreDamage extends CalibreInbuilt {
    void damage(ItemUser damager, Entity victim, Vector position, double damage, DamageCause cause);

    class Provider implements CalibreDamage {
        @Override
        public void damage(ItemUser damager, Entity victim, Vector position, double damage, DamageCause cause) {
            if (victim instanceof Player) {
                GameMode gameMode =  ((Player) victim).getGameMode();
                if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) return;
            }

            if (victim instanceof LivingEntity) {
                // #damage calls
                LivingEntity lVictim = (LivingEntity) victim;
                Entity eDamager = null;
                if (damager instanceof EntityItemUser && damager != lVictim)
                    eDamager = ((EntityItemUser) damager).getEntity();
                lVictim.damage(0.001, eDamager);
                lVictim.setNoDamageTicks(0);
                lVictim.setVelocity(new Vector());

                // Set health
                lVictim.setHealth(Utils.clamp(lVictim.getHealth() - damage, 0, lVictim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            } else {
                // TODO paintings, item frames... pop off
            }
        }
    }
}
