package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.Numbers;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Penetration {
    private static final double EPSILON = 0.01;

    @ConfigSerializable
    public record Config(
        Map<BlockData, Double> hardness
    ) {
        public static final Config DEFAULT = new Config(Collections.emptyMap());
    }

    private final CalibrePlugin plugin;
    private Config config;
    private final Map<Material, Map<BlockData, Double>> hardness = new HashMap<>();

    Penetration(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public Config config() { return config; }

    public void load() {
        config = plugin.setting(Config.DEFAULT, (n, d) -> n.get(Config.class, d), "penetration");
        hardness.clear();
        for (var entry : config.hardness.entrySet()) {
            hardness.computeIfAbsent(entry.getKey().getMaterial(), k -> new HashMap<>())
                .put(entry.getKey(), entry.getValue());
        }
    }

    public double hardness(BlockData block) {
        Material mat = block.getMaterial();
        var matHardness = hardness.get(mat);
        if (matHardness != null) {
            for (var entry : matHardness.entrySet()) {
                if (block.matches(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return mat.getBlastResistance();
    }

    public double hardness(PaperRaycast raycast, Vector3 from, Vector3 to, double distance, @Nullable Entity target) {
        double hardness = 0;

        Vector3 pos = from;
        Vector3 dir = to.subtract(from).normalize();
        Vector3 epsilon = dir.multiply(EPSILON);
        Raycast.Result<PaperRaycast.PaperBoundable> res;
        while ((res = raycast.cast(pos, dir, distance, null)).hit() != null) {
            var hit = res.hit().hit();
            if (hit.entity() == target)
                break;
            if (hit.entity() != null) {
                hardness += 0; // TODO hardness calc for entities
            }
            if (hit.block() != null) {
                hardness += hardness(hit.block().getBlockData()) * res.hit().penetration();
            }
            pos = res.hit().out().add(epsilon);
            distance -= res.distance() + EPSILON;
        }

        return hardness;
    }

    public double hardness(PaperRaycast raycast, Vector3 from, Vector3 to, @Nullable Entity target) {
        return hardness(raycast, from, to, from.distance(to), target);
    }

    public double penetration(double hardness, double penetration) {
        return penetration <= 0
            ? hardness > 0 ? 0 : 1
            : 1 - Numbers.clamp01(hardness / penetration);
    }
}
