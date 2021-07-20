package com.gitlab.aecsocket.calibre.paper.gun;

import com.gitlab.aecsocket.calibre.core.gun.SightSystem;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class PaperSightSystem extends SightSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    private static final String keyAiming = "aiming";
    private static final String keyTargetSystem = "target_system";
    private static final String keyTargetIndex = "target_index";

    public final class Instance extends SightSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent, boolean aiming, @Nullable SystemPath targetSystem, int targetIndex) {
            super(parent, aiming, targetSystem, targetIndex);
        }

        @Override public PaperSightSystem base() { return PaperSightSystem.this; }
        @Override public SokolPlatform platform() { return platform; }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            if (aiming) data.set(platform.key(keyAiming), PersistentDataType.BYTE, (byte) 0);
            if (targetSystem != null) data.set(platform.key(keyTargetSystem), platform.typeSystemPath(), targetSystem);
            data.set(platform.key(keyTargetIndex), PersistentDataType.INTEGER, targetIndex);
            return data;
        }

        @Override
        public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            node.node(keyAiming).set(aiming);
            node.node(keyTargetSystem).set(targetSystem);
            node.node(keyTargetIndex).set(targetIndex);
        }
    }

    private final SokolPlugin platform;

    public PaperSightSystem(SokolPlugin platform, int listenerPriority, InputMapper inputs) {
        super(listenerPriority, inputs);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node, false, null, 0);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node,
                data.has(platform.key(keyAiming), PersistentDataType.BYTE),
                data.get(platform.key(keyTargetSystem), platform.typeSystemPath()),
                data.getOrDefault(platform.key(keyTargetIndex), PersistentDataType.INTEGER, 0));
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node,
                cfg.node(keyAiming).getBoolean(),
                cfg.node(keyTargetSystem).get(SystemPath.class),
                cfg.node(keyTargetIndex).getInt());
    }

    public static PaperSystem.Type type(SokolPlugin platform) {
        return cfg -> new PaperSightSystem(platform,
                cfg.node(keyListenerPriority).getInt(),
                Serializers.require(cfg.node("inputs"), InputMapper.class));
    }
}
