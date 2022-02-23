package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.Logging;
import com.github.aecsocket.minecommons.core.bounds.Bound;
import com.github.aecsocket.minecommons.core.bounds.Box;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;

public final class Raycasts {
    private static final String FLUID = "fluid";
    private static final Box BOX = Box.box(Vector3.ZERO, Vector3.vec3(1));

    private final CalibrePlugin plugin;
    private PaperRaycast.Builder raycastBuilder;

    Raycasts(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        raycastBuilder = PaperRaycast.builder()
            .blockBound(Material.AIR, b -> true, b -> b.put(FLUID, BOX))
            .blockBound(Material.WATER, b -> true, b -> b.put(FLUID, BOX))
            .blockBound(Material.LAVA, b -> true, b -> b.put(FLUID, BOX));
        for (var entry : plugin.settings().root().node("raycasts").childrenMap().entrySet()) {
            BlockData data = Bukkit.createBlockData(""+entry.getKey());
            try {
                Map<String, Bound> bound = Serializers.require(entry.getValue(), new TypeToken<Map<String, Bound>>() {});
                raycastBuilder.blockBound(data.getMaterial(), b -> b.matches(data), bound);
            } catch (SerializationException e) {
                plugin.log(Logging.Level.WARNING, e, "Could not load bounds entry");
            }
        }
    }

    public PaperRaycast build(PaperRaycast.Options options, World world) {
        return raycastBuilder.build(options, world);
    }
}
