package me.aecsocket.calibre.item.util.slot;

import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface EquipmentItemSlot extends ItemSlot {
    EntityEquipment getEquipment();
    EquipmentSlot getEquipmentSlot();

    @Override default ItemStack get() { return getEquipment().getItem(getEquipmentSlot()); }
    @Override default void set(ItemStack item) { getEquipment().setItem(getEquipmentSlot(), item); }
}
