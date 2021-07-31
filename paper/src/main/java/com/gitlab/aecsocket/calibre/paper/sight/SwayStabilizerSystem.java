package com.gitlab.aecsocket.calibre.paper.sight;

import com.gitlab.aecsocket.calibre.core.sight.SwayStabilizer;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.PlayerData;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.VectorStat.*;

public final class SwayStabilizerSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "sway_stabilizer";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("sway_stabilization", vector2Stat())
            .put("sway_stamina_drain", longStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(STATS);

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance, SwayStabilizer {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public SwayStabilizerSystem base() { return SwayStabilizerSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public Vector2 stabilization(ItemTreeEvent.Hold event) {
            if (event.user() instanceof PlayerUser player && player.sneaking()) {
                Player handle = player.handle();
                Vector2 stabilization = parent.stats().req("sway_stabilization");
                if (handle.getGameMode() == GameMode.CREATIVE)
                    return stabilization;
                PlayerData data = calibre.playerData(handle);
                if (data.canStabilize()) {
                    data.drainStamina((long) ((parent.stats().<Long>req("sway_stamina_drain") * (event.delta() / 1000d))));
                    return stabilization;
                }
            }
            return Vector2.vec2(1);
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;

    public SwayStabilizerSystem(SokolPlugin platform, CalibrePlugin calibre) {
        super(0);
        this.platform = platform;
        this.calibre = calibre;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new SwayStabilizerSystem(platform, calibre);
    }
}
