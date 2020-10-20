package me.aecsocket.calibre.item.util.slot;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

public class EntityItemSlot implements EquipmentItemSlot {
    private final LivingEntity entity;
    private final EquipmentSlot equipmentSlot;

    public EntityItemSlot(LivingEntity entity, EquipmentSlot equipmentSlot) {
        this.entity = entity;
        this.equipmentSlot = equipmentSlot;
    }

    public LivingEntity getEntity() { return entity; }

    @Override public EntityEquipment getEquipment() { return entity.getEquipment(); }
    @Override public EquipmentSlot getEquipmentSlot() { return equipmentSlot; }
}
