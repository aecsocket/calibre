package me.aecsocket.calibre.wrapper.slot;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface EntityEquipmentSlot extends BukkitSlot {
    LivingEntity entity();
    EquipmentSlot slot();

    default ItemStack bukkitGet() { return entity().getEquipment().getItem(slot()); }
    default void bukkitSet(ItemStack item) { entity().getEquipment().setItem(slot(), item); }

    static EntityEquipmentSlot of(LivingEntity entity, EquipmentSlot slot) {
        return new EntityEquipmentSlot() {
            @Override public LivingEntity entity() { return entity; }
            @Override public EquipmentSlot slot() { return slot; }
        };
    }
}
