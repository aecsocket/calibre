package me.aecsocket.calibre.util;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.serialization.configurate.ConfigurateSerializer;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public class CasingManager implements Tickable {
    public static class Casing {
        private final Category category;
        private boolean wasOnGround;

        public Casing(Category category) {
            this.category = category;
        }

        public Category category() { return category; }

        public boolean wasOnGround() { return wasOnGround; }
        public void wasOnGround(boolean wasOnGround) { this.wasOnGround = wasOnGround; }
    }

    public static class Category {
        @ConfigSerializable
        public static class MaterialData {
            public static final MaterialData EMPTY = new MaterialData(null, null);

            private final SoundData[] sound;
            private final ParticleData[] particle;

            public MaterialData(SoundData[] sound, ParticleData[] particle) {
                this.sound = sound;
                this.particle = particle;
            }

            public MaterialData() {
                this(null, null);
            }

            public SoundData[] sound() { return sound; }
            public ParticleData[] particle() { return particle; }

            @Override
            public String toString() {
                return "MaterialData{" +
                        "sound=" + Arrays.toString(sound) +
                        ", particle=" + Arrays.toString(particle) +
                        '}';
            }
        }

        public static class Serializer implements TypeSerializer<Category>, ConfigurateSerializer {
            public static final String DEFAULT_MATERIAL = "default";

            private final CalibrePlugin plugin;
            private final TypeSerializer<Category> delegate;

            public Serializer(CalibrePlugin plugin, TypeSerializer<Category> delegate) {
                this.plugin = plugin;
                this.delegate = delegate;
            }

            public CalibrePlugin plugin() { return plugin; }
            public TypeSerializer<Category> delegate() { return delegate; }

            @Override
            public void serialize(Type type, @Nullable Category obj, ConfigurationNode node) throws SerializationException {
                delegate.serialize(type, obj, node);
            }

            @Override
            public Category deserialize(Type type, ConfigurationNode node) throws SerializationException {
                Map<Material, MaterialData> materialData = new HashMap<>();
                for (Map.Entry<?, ? extends ConfigurationNode> entry : asMap(node.node("material_data"), type).entrySet()) {
                    String key = entry.getKey().toString();
                    MaterialData value = entry.getValue().get(MaterialData.class);
                    if (key.equals(DEFAULT_MATERIAL))
                        materialData.put(null, value);
                    else {
                        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(key));
                        if (material == null) {
                            plugin.log(LogLevel.WARN, "Material [" + key + "] for casings is invalid");
                            continue;
                        }
                        materialData.put(material, value);
                    }
                }
                return new Category(
                        materialData,
                        node.node("remove_on_land").getBoolean(false)
                );
            }
        }

        private final Map<Material, MaterialData> materialData;
        private final boolean removeOnLand;

        public Category(Map<Material, MaterialData> materialData, boolean removeOnLand) {
            this.materialData = materialData;
            this.removeOnLand = removeOnLand;
        }

        public Map<Material, MaterialData> material() { return materialData; }
        public boolean removeOnLand() { return removeOnLand; }

        public MaterialData materialData(Material material) {
            return materialData.containsKey(material)
                    ? materialData.get(material)
                    : materialData.getOrDefault(null, MaterialData.EMPTY);
        }

        public MaterialData materialData(Block hit) { return materialData(hit.getType()); }

        @Override
        public String toString() {
            return "Category{" +
                    "material=" + materialData +
                    '}';
        }
    }

    private final CalibrePlugin plugin;
    private final Map<UUID, Casing> casings = new HashMap<>();
    private Map<String, Category> categories = new HashMap<>();

    public CasingManager(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            categories = plugin.setting("casing").get(new TypeToken<>(){});
        } catch (SerializationException e) {
            plugin.log(LogLevel.WARN, e, "Could not load casing settings");
        }
    }

    public CalibrePlugin plugin() { return plugin; }
    public Map<UUID, Casing> casings() { return casings; }
    public Map<String, Category> categories() { return categories; }

    public void register(Item entity, String categoryName) {
        Category category = categories.get(categoryName);
        if (category == null)
            return;
        casings.put(entity.getUniqueId(), new Casing(category));
    }

    @Override
    public void tick(TickContext tickContext) {
        Iterator<Map.Entry<UUID, Casing>> iter = casings.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Casing> entry = iter.next();
            UUID uuid = entry.getKey();
            Casing casing = entry.getValue();
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof Item) || !entity.isValid()) {
                iter.remove();
                return;
            }

            Item item = (Item) entity;
            if (!casing.wasOnGround && item.isOnGround()) {
                Block hit = item.getLocation().subtract(0, 0.1, 0).getBlock();
                Category.MaterialData data = casing.category.materialData(hit);
                SoundData.play(item::getLocation, data.sound);
                ParticleData.spawn(item.getLocation(), hit.getBlockData(), data.particle);

                if (casing.category.removeOnLand) {
                    item.remove();
                    iter.remove();
                    return;
                } else
                    casing.wasOnGround = true;

            }

            if (casing.wasOnGround && !item.isOnGround())
                casing.wasOnGround = false;
        }
    }
}
