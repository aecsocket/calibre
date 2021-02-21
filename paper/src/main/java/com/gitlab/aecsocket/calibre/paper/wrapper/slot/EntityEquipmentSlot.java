package com.gitlab.aecsocket.calibre.paper.wrapper.slot;

import com.gitlab.aecsocket.calibre.core.world.slot.HandSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface EntityEquipmentSlot extends BukkitSlot, HandSlot<BukkitItem> {
    LivingEntity entity();
    EquipmentSlot slot();

    @Override default ItemStack bukkitGet() { return entity().getEquipment().getItem(slot()); }
    @Override default void bukkitSet(ItemStack item) { entity().getEquipment().setItem(slot(), item); }

    @Override default boolean equipped() { return true; }

    @Override default boolean main() { return slot() == EquipmentSlot.HAND; }
    @Override default HandSlot<BukkitItem> opposite() {
        return EntityEquipmentSlot.of(entity(), slot() == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
    }

    static EntityEquipmentSlot of(LivingEntity entity, EquipmentSlot slot) {
        return new EntityEquipmentSlot() {
            @Override public LivingEntity entity() { return entity; }
            @Override public EquipmentSlot slot() { return slot; }
        };
    }
}
