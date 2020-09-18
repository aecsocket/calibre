package me.aecsocket.calibre.util.itemuser;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * A {@link LivingEntity} item user.
 */
public class LivingEntityItemUser implements EntityItemUser {
    private final LivingEntity entity;

    public LivingEntityItemUser(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() { return entity; }

    @Override public Location getLocation() { return entity.getEyeLocation(); }
    @Override public void startAnimation(Animation animation, EquipmentSlot slot) {}

    @Override public ItemStack getItem(EquipmentSlot slot) { return entity.getEquipment().getItem(slot); }
    @Override public void setItem(EquipmentSlot slot, ItemStack item) { entity.getEquipment().setItem(slot, item); }
}
