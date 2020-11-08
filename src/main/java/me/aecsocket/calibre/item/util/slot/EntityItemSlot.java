package me.aecsocket.calibre.item.util.slot;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityItemSlot that = (EntityItemSlot) o;
        return entity.equals(that.entity) &&
                equipmentSlot == that.equipmentSlot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, equipmentSlot);
    }
}
