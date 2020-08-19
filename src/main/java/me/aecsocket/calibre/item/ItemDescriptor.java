package me.aecsocket.calibre.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemDescriptor {
    private Material material;
    private int modelData;
    private int damage;

    public ItemDescriptor(Material material, int modelData, int damage) {
        this.material = material;
        this.modelData = modelData;
        this.damage = damage;
    }

    public ItemDescriptor(Material material, int modelData) {
        this.material = material;
        this.modelData = modelData;
    }

    public ItemDescriptor(Material material) {
        this.material = material;
    }

    public ItemDescriptor() {}

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public int getModelData() { return modelData; }
    public void setModelData(int modelData) { this.modelData = modelData; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public ItemStack create() {
        ItemStack result = new ItemStack(material);
        ItemMeta meta = result.getItemMeta();
        meta.setCustomModelData(modelData);
        if (meta instanceof Damageable)
            ((Damageable) meta).setDamage(damage);
        result.setItemMeta(meta);
        return result;
    }
}
