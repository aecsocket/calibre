package com.gitlab.aecsocket.calibre.paper;

import io.leangen.geantyref.TypeToken;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Materials {
    private final CalibrePlugin plugin;
    private final Map<Material, Map<String, Double>> materialHardness = new HashMap<>();
    private final Map<EntityType, Map<String, Double>> entityHardness = new HashMap<>();
    private double defaultEntityHardness;

    public Materials(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        Map<Material, Map<String, Double>> oMaterialHardness = plugin.setting(Collections.emptyMap(), (n, d) -> n.get(new TypeToken<>() {}, d), "hardness", "material");
        if (oMaterialHardness != null)
            materialHardness.putAll(oMaterialHardness);
        Map<EntityType, Map<String, Double>> oEntityHardness = plugin.setting(Collections.emptyMap(), (n, d) -> n.get(new TypeToken<>() {}, d), "hardness", "entity");
        if (oEntityHardness != null)
            entityHardness.putAll(oEntityHardness);
        defaultEntityHardness = plugin.setting(0.5, ConfigurationNode::getDouble, "hardness", "default_entity_hardness");
    }

    public CalibrePlugin plugin() { return plugin; }
    public Map<Material, Map<String, Double>> materialHardness() { return materialHardness; }
    public Map<EntityType, Map<String, Double>> entityHardness() { return entityHardness; }
    public double defaultEntityHardness() { return defaultEntityHardness; }

    public double hardness(Material material, String bound) {
        return materialHardness.getOrDefault(material, Collections.emptyMap()).getOrDefault(bound, (double) material.getBlastResistance());
    }

    public double hardness(EntityType entity, String bound) {
        return entityHardness.getOrDefault(entity, Collections.emptyMap()).getOrDefault(bound, defaultEntityHardness);
    }
}
