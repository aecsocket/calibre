package com.gitlab.aecsocket.calibre.paper.gun;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.Pair;
import com.gitlab.aecsocket.calibre.core.gun.SightManagerSystem;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.PlayerData;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.minecommons.paper.persistence.Persistence;
import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolConstants;
import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolLibAPI;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.EquipSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.paper.stat.ItemStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.AnimationStat.*;

public final class PaperSightManagerSystem extends SightManagerSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put(SightManagerSystem.STATS)
            .put("aim_item", itemStat())
            .put("aim_in_sound", soundsStat())
            .put("aim_out_sound", soundsStat())
            .put("aim_out_animation", animationStat())
            .put("change_sight_sound", soundsStat())
            .build();

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
        @Override public SokolPlugin platform() { return platform; }

        @Override
        protected void zoom(ItemUser user, double zoom) {
            if (!(user instanceof PlayerUser player))
                return;
            Player handle = player.handle();
            calibre.zoom(handle, (handle.getWalkSpeed() / 2) + (float) (zoom));
        }

        protected @Nullable ItemStack defaultShaderDataItem() {
            return defaultShaderData == null ? null : defaultShaderData.createRaw();
        }

        @Override
        protected void applySight(ItemUser user, ItemSlot slot, SightReference ref) {
            super.applySight(user, slot, ref);
            if (!(user instanceof PaperUser paper))
                return;
            if (!(ref.sight() instanceof PaperSight sight))
                return;
            if (sight.applySound() != null)
                sight.applySound().forEach(s -> s.play(platform, paper.location()));
            if (user instanceof PlayerUser player) {
                PlayerData data = calibre.playerData(player.handle());
                data.shaderData(defaultShaderDataItem());
                if (sight.shaderData() != null)
                    scheduler.schedule(this, sight.shaderDataDelay(), (self, evt, ctx) ->
                            data.shaderData(sight.shaderData().createRaw()));
                if (sight.applyAnimation() != null &&  slot instanceof EquipSlot equip) {
                    sight.applyAnimation().start(platform, player.handle(), equip);
                }
            }
        }

        @Override
        protected void aim(ItemUser user, ItemSlot slot, boolean aiming, Action action, String key) {
            super.aim(user, slot, aiming, action, key);
            if (!(user instanceof PaperUser paper))
                return;

            parent.stats().<List<PreciseSound>>desc("aim_" + key + "_sound")
                    .ifPresent(v -> v.forEach(s -> s.play(platform, paper.location())));

            if (!(paper instanceof PlayerUser player) || !(slot instanceof EquipSlot equip))
                return;

            if (aiming) {
                parent.stats().<ItemDescriptor>val("aim_item")
                        .ifPresent(desc -> {
                            ItemStack item = desc.createRaw();
                            PacketContainer equipPacket = calibre.protocol().build(PacketType.Play.Server.ENTITY_EQUIPMENT, packet -> {
                                packet.getIntegers().write(0, player.handle().getEntityId());
                                packet.getSlotStackPairLists().write(0, Collections.singletonList(new Pair<>(
                                        ProtocolConstants.SLOTS.get(equip.slot()), item
                                )));
                            });
                            PacketContainer metaPacket = calibre.protocol().build(PacketType.Play.Server.ENTITY_METADATA, packet -> {
                                packet.getIntegers().write(0, player.handle().getEntityId());
                                packet.getWatchableCollectionModifier().write(0, ProtocolLibAPI.watcherObjects()
                                        .add(8, // bow animation
                                                Byte.class, (byte) 1)
                                        .get()
                                );
                            });
                            for (Player viewer : player.handle().getTrackedPlayers()) {
                                calibre.protocol().send(viewer, equipPacket);
                                calibre.protocol().send(viewer, metaPacket);
                            }
                        });
            } else
                calibre.playerData(player.handle()).shaderData(null);
        }

        @Override
        public void cycleSight(ItemUser user, ItemSlot slot, int direction) {
            super.cycleSight(user, slot, direction);
            if (!(user instanceof PaperUser paper))
                return;
            parent.stats().<List<PreciseSound>>desc("change_sight_sound")
                    .ifPresent(v -> v.forEach(s -> s.play(platform, paper.location())));
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
    private final @Nullable ItemDescriptor defaultShaderData;

    public PaperSightManagerSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority, @Nullable InputMapper inputs, @Nullable ItemDescriptor defaultShaderData) {
        super(listenerPriority, inputs);
        this.platform = platform;
        this.calibre = calibre;
        this.defaultShaderData = defaultShaderData;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }
    public ItemDescriptor defaultShaderData() { return defaultShaderData; }

    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

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
                null,
                cfg.node("default_shader_data").get(ItemDescriptor.class));
    }
}
