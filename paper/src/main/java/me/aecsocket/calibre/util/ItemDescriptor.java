package me.aecsocket.calibre.util;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.unifiedframework.stat.AbstractStat;
import me.aecsocket.unifiedframework.stat.serialization.ConfigurateStat;
import me.aecsocket.unifiedframework.stat.serialization.FunctionCreationException;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.function.Function;

@ConfigSerializable
public class ItemDescriptor {
    public static class Stat extends AbstractStat<ItemDescriptor> implements ConfigurateStat<ItemDescriptor> {
        public Stat(ItemDescriptor defaultValue) { super(defaultValue); }
        public Stat() {}
        public Stat(AbstractStat<ItemDescriptor> o) { super(o); }
        @Override public TypeToken<ItemDescriptor> valueType() { return new TypeToken<>(){}; }

        @Override
        public Function<ItemDescriptor, ItemDescriptor> getModFunction(ConfigurationNode node) throws FunctionCreationException {
            ItemDescriptor value;
            try {
                value = node.get(ItemDescriptor.class);
            } catch (SerializationException e) {
                throw new FunctionCreationException(e);
            }
            return base -> value;
        }
    }

    private final NamespacedKey id;
    private final Integer modelData;
    private final Integer damage;

    public ItemDescriptor(NamespacedKey id, Integer modelData, Integer damage) {
        this.id = id;
        this.modelData = modelData;
        this.damage = damage;
    }

    public ItemDescriptor() { this(null, null, null); }

    public NamespacedKey id() { return id; }
    public ItemDescriptor id(NamespacedKey id) { return new ItemDescriptor(id, modelData, damage); }

    public Integer modelData() { return modelData; }
    public ItemDescriptor modelData(Integer modelData) { return new ItemDescriptor(id, modelData, damage); }

    public Integer damage() { return damage; }
    public ItemDescriptor damage(Integer damage) { return new ItemDescriptor(id, modelData, damage); }

    public ItemStack apply(ItemStack item) {
        Material material = Registry.MATERIAL.get(id);
        if (material == null)
            throw new IllegalArgumentException("Invalid item key [" + id + "]");
        item.setType(material);
        return BukkitUtils.modMeta(item, meta -> {
            if (modelData != null)
                meta.setCustomModelData(modelData);
            if (damage != null && meta instanceof Damageable)
                ((Damageable) meta).setDamage(damage);
        });
    }

    public ItemStack create(int amount) {
        return apply(new ItemStack(Material.AIR, amount));
    }

    public ItemStack create() { return create(1); }

    @Override
    public String toString() {
        return "ItemDescriptor{" +
                "id=" + id +
                ", modelData=" + modelData +
                ", damage=" + damage +
                '}';
    }
}
