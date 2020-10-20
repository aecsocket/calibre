package me.aecsocket.calibre.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import me.aecsocket.unifiedframework.stat.impl.SimpleStat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.lang.reflect.Type;
import java.util.function.Function;

// TODO docs
public class ItemDescriptor implements Cloneable {
    public static class Stat extends SimpleStat<ItemDescriptor> {
        public Stat(ItemDescriptor defaultValue) { super(defaultValue); }
        public Stat() {}

        @Override public Type getValueType() { return ItemDescriptor.class; }

        @Override
        public Function<ItemDescriptor, ItemDescriptor> getModFunction(JsonElement json, JsonDeserializationContext context) throws JsonParseException {
            return b -> context.deserialize(json, ItemDescriptor.class);
        }

        @Override public ItemDescriptor copy(ItemDescriptor value) { return value == null ? null : value.clone(); }
    }

    private final Material material;
    private final Integer modelData;
    private final Integer damage;

    public ItemDescriptor(Material material, Integer modelData, Integer damage) {
        this.material = material;
        this.modelData = modelData;
        this.damage = damage;
    }

    public ItemDescriptor() {
        this(null, null, null);
    }

    public Material getMaterial() { return material; }
    public Integer getModelData() { return modelData; }
    public Integer getDamage() { return damage; }

    public ItemStack apply(ItemStack item) {
        item.setType(material);
        Utils.modMeta(item, meta -> {
            if (modelData != null) meta.setCustomModelData(modelData);
            if (damage != null && meta instanceof Damageable)
                ((Damageable) meta).setDamage(damage);
        });
        return item;
    }

    public ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.AIR, amount);
        apply(item);
        return item;
    }

    public ItemStack create() { return create(1); }

    public ItemDescriptor clone() { try { return (ItemDescriptor) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
}
