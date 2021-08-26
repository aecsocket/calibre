package com.gitlab.aecsocket.calibre.paper.sight;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.Pair;
import com.gitlab.aecsocket.calibre.core.sight.Sight;
import com.gitlab.aecsocket.calibre.core.sight.SightManagerSystem;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.PlayerData;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Ray3;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.persistence.Persistence;
import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolConstants;
import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolLibAPI;
import com.gitlab.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.stat.AnimationStat;
import com.gitlab.aecsocket.sokol.paper.stat.ItemStat;
import com.gitlab.aecsocket.sokol.paper.stat.SoundsStat;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.EquipSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;

import static com.gitlab.aecsocket.sokol.paper.stat.ItemStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.AnimationStat.*;

public final class PaperSightManagerSystem extends SightManagerSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public static final ItemStat STAT_AIM_ITEM = itemStat("aim_item");

    public static final SoundsStat STAT_AIM_IN_SOUNDS = soundsStat("aim_in_sounds");
    public static final AnimationStat STAT_AIM_IN_ANIMATION = animationStat("aim_in_animation");
    public static final SoundsStat STAT_AIM_OUT_SOUNDS = soundsStat("aim_out_sounds");
    public static final AnimationStat STAT_AIM_OUT_ANIMATION = animationStat("aim_out_animation");

    public static final SoundsStat STAT_CHANGE_SIGHT_SOUNDS = soundsStat("change_sight_sounds");

    public static final StatTypes STATS = StatTypes.builder()
            .add(SightManagerSystem.STATS)
            .add(
                    STAT_AIM_ITEM,
                    STAT_AIM_IN_SOUNDS, STAT_AIM_IN_ANIMATION, STAT_AIM_OUT_SOUNDS, STAT_AIM_OUT_ANIMATION,
                    STAT_CHANGE_SIGHT_SOUNDS
            )
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofBoth(ID, STATS, RULES);

    private static final String keyAiming = "aiming";
    private static final String keyTargetSystem = "target_system";
    private static final String keyTargetIndex = "target_index";
    private static final String keyAction = "action";
    private static final String keyActionStart = "action_start";
    private static final String keyActionEnd = "action_end";
    private static final String keyResting = "resting";
    private static final String keyLastShaderDataTask = "last_shader_data_task";

    public final class Instance extends SightManagerSystem.Instance implements PaperSystem.Instance {
        private int lastShaderDataTask = -1;

        public Instance(TreeNode parent, boolean aiming, @Nullable SystemPath targetSystem, int targetIndex,
                        @Nullable Action action, long actionStart, long actionEnd, boolean resting,
                        int lastShaderDataTask) {
            super(parent, aiming, targetSystem, targetIndex, action, actionStart, actionEnd, resting);
            this.lastShaderDataTask = lastShaderDataTask;
        }

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperSightManagerSystem base() { return PaperSightManagerSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build(StatLists stats) {
            super.build(stats);
            parent.events().register(ItemTreeEvent.Unequip.class, this::event, listenerPriority);
        }

        @Override
        protected void zoom(ItemUser user, double zoom) {
            if (!(user instanceof PlayerUser player))
                return;
            calibre.zoom(player.handle(), (float) zoom);
        }

        @Override
        protected void sway(ItemUser user, Vector2 vector) {
            if (!(user instanceof PlayerUser player))
                return;
            calibre.rotate(player.handle(), vector.x(), vector.y());
        }

        @Override
        protected boolean resting(ItemUser user, Vector3 offsetA, Vector3 offsetB) {
            if (!(user instanceof PaperUser paper))
                return false;
            Location location = paper.location();
            Vector3 dir = PaperUtils.toCommons(location.getDirection());
            Vector3 fA = Vector3.offset(dir, offsetA);
            Vector3 fB = Vector3.offset(dir, offsetB);

            Vector3 pos = PaperUtils.toCommons(location);
            Vector3 pA = pos.add(fA);
            Vector3 pB = pos.add(fB);

            PaperRaycast raycast = calibre.raycast(location.getWorld());
            Ray3 ray = Ray3.ray3(pA, Vector3.vec3(1));
            return raycast.castBlocks(ray, 0.1, null).hit() != null
                    || raycast.castBlocks(ray.at(pB), 0.1, null).hit() != null;
        }

        protected @Nullable ItemStack defaultShaderDataItem() {
            return defaultShaderData == null ? null : defaultShaderData.createRaw();
        }

        @Override
        protected void event(ItemTreeEvent.Unequip event) {
            super.event(event);
            if (!parent.isRoot())
                return;
            if (event.user() instanceof PlayerUser player)
                calibre.playerData(player.handle()).shaderData(null);
        }

        @Override
        protected void apply(ItemUser user, ItemSlot slot, Sight raw) {
            super.apply(user, slot, raw);
            if (!(user instanceof PaperUser paper))
                return;
            if (!(raw instanceof PaperSight sight))
                return;
            if (sight.applySound() != null)
                sight.applySound().forEach(s -> s.play(platform, paper.location()));
            if (user instanceof PlayerUser player) {
                selected().ifPresent(s -> {
                    PlayerData data = calibre.playerData(player.handle());
                    if (lastShaderDataTask > -1)
                        scheduler.unschedule(lastShaderDataTask);
                    data.shaderData(defaultShaderDataItem());
                    lastShaderDataTask = sight.shaderData() == null ? -1
                            : scheduler.schedule(this, sight.shaderDataDelay(), (self, evt, ctx) ->
                                data.shaderData(sight.shaderData().createRaw()));
                });
                if (sight.applyAnimation() != null)
                    sight.applyAnimation().start(platform, player.handle(), slot);
            }
        }

        @Override
        protected void aim(ItemUser user, ItemSlot slot, boolean aiming, Action action, String key) {
            super.aim(user, slot, aiming, action, key);
            if (!(user instanceof PlayerUser player))
                return;

            if (aiming) {
                if (slot instanceof EquipSlot equip) {
                    parent.stats().val(STAT_AIM_ITEM)
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
                }
            } else {
                if (lastShaderDataTask > -1) {
                    scheduler.unschedule(lastShaderDataTask);
                    lastShaderDataTask = -1;
                }
                calibre.playerData(player.handle()).shaderData(null);
            }
        }

        @Override
        protected boolean changeSight(ItemTreeEvent.Input event, int direction) {
            if (super.changeSight(event, direction)) {
                selected().ifPresent(s -> {
                    if (event.updated() && s instanceof PaperSight sight && sight.applyAnimation() != null)
                        event.update(com.gitlab.aecsocket.sokol.core.wrapper.ItemStack::hideUpdate);
                });
                return true;
            }
            return false;
        }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            if (aiming) data.set(platform.key(keyAiming), PersistentDataType.BYTE, (byte) 1);
            if (targetSystem != null) data.set(platform.key(keyTargetSystem), platform.typeSystemPath(), targetSystem);
            data.set(platform.key(keyTargetIndex), PersistentDataType.INTEGER, targetIndex);
            if (action != null) Persistence.setEnum(data, platform.key(keyAction), action);
            data.set(platform.key(keyActionStart), PersistentDataType.LONG, actionStart);
            data.set(platform.key(keyActionEnd), PersistentDataType.LONG, actionEnd);
            if (resting) data.set(platform.key(keyResting), PersistentDataType.BYTE, (byte) 1);
            data.set(platform.key(keyLastShaderDataTask), PersistentDataType.INTEGER, lastShaderDataTask);
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
            node.node(keyLastShaderDataTask).set(lastShaderDataTask);
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;
    private final @Nullable ItemDescriptor defaultShaderData;

    public PaperSightManagerSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority, @Nullable InputMapper inputs, @Nullable Sight fallback, @Nullable ItemDescriptor defaultShaderData) {
        super(listenerPriority, inputs, fallback);
        this.platform = platform;
        this.calibre = calibre;
        this.defaultShaderData = defaultShaderData;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }
    public ItemDescriptor defaultShaderData() { return defaultShaderData; }

    @Override public StatTypes statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node,
                data.has(platform.key(keyAiming), PersistentDataType.BYTE),
                data.get(platform.key(keyTargetSystem), platform.typeSystemPath()),
                data.getOrDefault(platform.key(keyTargetIndex), PersistentDataType.INTEGER, 0),
                Persistence.getEnum(data, platform.key(keyAction), SightManagerSystem.Instance.Action.class).orElse(null),
                data.getOrDefault(platform.key(keyActionStart), PersistentDataType.LONG, 0L),
                data.getOrDefault(platform.key(keyActionEnd), PersistentDataType.LONG, 0L),
                data.has(platform.key(keyResting), PersistentDataType.BYTE),
                data.getOrDefault(platform.key(keyLastShaderDataTask), PersistentDataType.INTEGER, -1));
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node,
                cfg.node(keyAiming).getBoolean(),
                cfg.node(keyTargetSystem).get(SystemPath.class),
                cfg.node(keyTargetIndex).getInt(),
                cfg.node(keyAction).get(SightManagerSystem.Instance.Action.class),
                cfg.node(keyActionStart).getLong(),
                cfg.node(keyActionEnd).getLong(),
                cfg.node(keyResting).getBoolean(),
                cfg.node(keyLastShaderDataTask).getInt(-1));
    }

    public static ConfigType type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new PaperSightManagerSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt(),
                null,
                null,
                cfg.node("default_shader_data").get(ItemDescriptor.class));
    }
}
