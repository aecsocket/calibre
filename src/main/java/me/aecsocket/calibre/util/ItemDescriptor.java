package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * A description of an {@link ItemStack}.
 */
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

    /**
     * Applies this instance's properties to an existing {@link ItemStack}.
     * @param existing The existing item. Will be modified.
     * @return The modified item.
     */
    public ItemStack apply(ItemStack existing) {
        if (material != null) existing.setType(material);
        return Utils.modMeta(existing, meta -> {
            meta.setCustomModelData(modelData);
            if (meta instanceof Damageable)
                ((Damageable) meta).setDamage(damage);
        });
    }

    /**
     * Creates the {@link ItemStack}.
     * @return The ItemStack.
     */
    public ItemStack create() {
        return apply(new ItemStack(Material.AIR));
    }

    /**
     * Gets lines of info used by other objects in <code>/calibre info</code>. The string is split
     * by <code>\n</code> to create the line separations. Can be null.
     * @param plugin The CalibrePlugin used for text generation.
     * @param sender The command's sender.
     * @return The info.
     */
    public String getLongInfo(CalibrePlugin plugin, CommandSender sender) {
        return plugin.gen(sender, "chat.info.item",
                "material", new ItemStack(material).getI18NDisplayName(),
                "model_data", modelData,
                "damage", damage);
    }

    @Override
    public String toString() { return material + "{modelData=" + modelData + ",damage=" + damage + "}"; }
}
