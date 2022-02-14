package com.github.aecsocket.calibre.paper;

import io.leangen.geantyref.TypeToken;
import org.bukkit.Material;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Materials {
    private final CalibrePlugin plugin;
    private final Map<String, Double> hardness = new HashMap<>();
    private double defBlockHardness;
    private double defEntityHardness;

    public Materials(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        Map<String, Double> oHardness = plugin.setting(Collections.emptyMap(), (n, d) -> n.get(new TypeToken<>() {}, d), "materials", "hardness");
        if (oHardness != null)
            hardness.putAll(oHardness);
        defBlockHardness = plugin.setting(1d, ConfigurationNode::getDouble, "materials", "default_block_hardness");
        defEntityHardness = plugin.setting(1d, ConfigurationNode::getDouble, "materials", "default_entity_hardness");
    }

    public CalibrePlugin plugin() { return plugin; }
    public Map<String, Double> entityHardness() { return hardness; }
    public double defBlockHardness() { return defBlockHardness; }
    public double defEntityHardness() { return defEntityHardness; }

    public double blockHardness(String material, Material blockType) {
        return hardness.getOrDefault(material, (double) blockType.getBlastResistance() * defBlockHardness);
    }

    public double entityHardness(String material) {
        return hardness.getOrDefault(material, defEntityHardness);
    }
}
