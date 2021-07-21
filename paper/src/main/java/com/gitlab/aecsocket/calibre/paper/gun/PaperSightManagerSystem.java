package com.gitlab.aecsocket.calibre.paper.gun;

import com.gitlab.aecsocket.calibre.core.gun.SightManagerSystem;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.minecommons.paper.persistence.Persistence;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class PaperSightManagerSystem extends SightManagerSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    private static final String keyAiming = "aiming";
    private static final String keyTargetSystem = "target_system";
    private static final String keyTargetIndex = "target_index";
    private static final String keyAction = "action";
    private static final String keyActionStart = "action_start";
    private static final String keyActionEnd = "action_end";

    public final class Instance extends SightManagerSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent, boolean aiming, @Nullable SystemPath targetSystem, int targetIndex, @Nullable Action action, long actionStart, long actionEnd) {
            super(parent, aiming, targetSystem, targetIndex, action, actionStart, actionEnd);
        }

        @Override public PaperSightManagerSystem base() { return PaperSightManagerSystem.this; }
        @Override public SokolPlatform platform() { return platform; }

        @Override
        protected void zoom(ItemUser user, double zoom) {
            if (!(user instanceof PlayerUser player))
                return;
            Player handle = player.handle();
            calibre.zoom(handle, (handle.getWalkSpeed() / 2) + (float) (zoom));
        }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            if (aiming) data.set(platform.key(keyAiming), PersistentDataType.BYTE, (byte) 0);
            if (targetSystem != null) data.set(platform.key(keyTargetSystem), platform.typeSystemPath(), targetSystem);
            data.set(platform.key(keyTargetIndex), PersistentDataType.INTEGER, targetIndex);
            if (action != null) Persistence.setEnum(data, platform.key(keyAction), action);
            data.set(platform.key(keyActionStart), PersistentDataType.LONG, actionStart);
            data.set(platform.key(keyActionEnd), PersistentDataType.LONG, actionEnd);
            return data;
        }

        @Override
        public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            node.node(keyAiming).set(aiming);
            node.node(keyTargetSystem).set(targetSystem);
            node.node(keyTargetIndex).set(targetIndex);
            node.node(keyAction).set(action);
            node.node(keyActionStart).set(actionStart);
            node.node(keyActionEnd).set(actionEnd);
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;

    public PaperSightManagerSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority, @Nullable InputMapper inputs) {
        super(listenerPriority, inputs);
        this.platform = platform;
        this.calibre = calibre;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node, false, null, 0, null, 0, 0);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node,
                data.has(platform.key(keyAiming), PersistentDataType.BYTE),
                data.get(platform.key(keyTargetSystem), platform.typeSystemPath()),
                data.getOrDefault(platform.key(keyTargetIndex), PersistentDataType.INTEGER, 0),
                Persistence.getEnum(data, platform.key(keyAction), SightManagerSystem.Instance.Action.class).orElse(null),
                data.getOrDefault(platform.key(keyActionStart), PersistentDataType.LONG, 0L),
                data.getOrDefault(platform.key(keyActionEnd), PersistentDataType.LONG, 0L));
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node,
                cfg.node(keyAiming).getBoolean(),
                cfg.node(keyTargetSystem).get(SystemPath.class),
                cfg.node(keyTargetIndex).getInt(),
                cfg.node(keyAction).get(SightManagerSystem.Instance.Action.class),
                cfg.node(keyActionStart).getLong(),
                cfg.node(keyActionEnd).getLong());
    }

    public static PaperSystem.Type type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new PaperSightManagerSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt(),
                null);
    }
}
