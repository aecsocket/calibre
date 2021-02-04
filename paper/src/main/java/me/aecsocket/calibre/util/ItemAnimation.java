package me.aecsocket.calibre.util;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.world.slot.ItemSlot;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.wrapper.slot.EntityEquipmentSlot;
import me.aecsocket.calibre.wrapper.slot.InventorySlot;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.serialization.configurate.ConfigurateSerializer;
import me.aecsocket.unifiedframework.stat.AbstractStat;
import me.aecsocket.unifiedframework.stat.serialization.ConfigurateStat;
import me.aecsocket.unifiedframework.stat.serialization.FunctionCreationException;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemAnimation {
    public static class Serializer implements TypeSerializer<ItemAnimation>, ConfigurateSerializer {
        private final CalibrePlugin plugin;

        public Serializer(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        @Override
        public void serialize(Type type, @Nullable ItemAnimation obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.setList(Frame.class, obj.frames);
            }
        }

        @Override
        public ItemAnimation deserialize(Type type, ConfigurationNode node) throws SerializationException {
            List<? extends ConfigurationNode> children = asList(node, type);
            List<Frame> frames = new ArrayList<>();
            for (ConfigurationNode child : children) {
                if (child.hasChild("type")) {
                    String frameType = child.node("type").getString();
                    if (frameType == null)
                        continue;
                    switch (frameType) {
                        case "range":
                            long duration = child.node("duration").getLong(0);
                            int from = child.node("from").getInt();
                            boolean useDamage = child.node("use_damage").getBoolean(false);

                            EquipmentSlot slot = child.node("slot").get(EquipmentSlot.class);
                            int step = node.node("step").getInt(1);
                            int range = node.node("range").getInt();
                            for (int i = 0; i < range; i++) {
                                int val = from + (i * step);
                                frames.add(new Frame(
                                        duration, slot,
                                        useDamage ? null : val,
                                        useDamage ? val : null
                                ));
                            }
                            break;
                        default:
                            throw new SerializationException("Invalid complex frame type '" + frameType + "'");
                    }
                } else
                    frames.add(child.get(Frame.class));
            }
            return new ItemAnimation(plugin, frames);
        }
    }

    public static class Stat extends AbstractStat<ItemAnimation> implements ConfigurateStat<ItemAnimation> {
        public Stat(ItemAnimation defaultValue) { super(defaultValue); }
        public Stat() {}
        public Stat(AbstractStat<ItemAnimation> o) { super(o); }
        @Override public TypeToken<ItemAnimation> valueType() { return new TypeToken<>(){}; }

        @Override
        public Function<ItemAnimation, ItemAnimation> getModFunction(ConfigurationNode node) throws FunctionCreationException {
            ItemAnimation value;
            try {
                value = node.get(ItemAnimation.class);
            } catch (SerializationException e) {
                throw new FunctionCreationException(e);
            }
            return base -> value;
        }
    }

    public class Instance implements Tickable {
        private final CalibrePlugin plugin;
        private final Player player;
        private final int slot;
        private int index;
        private Frame frame;
        private long frameTime;

        public Instance(CalibrePlugin plugin, Player player, int slot, int index, Frame frame) {
            this.plugin = plugin;
            this.player = player;
            this.slot = slot;
            this.index = index;
            this.frame = frame;
        }

        public Instance(CalibrePlugin plugin, Player player, int slot) {
            this.plugin = plugin;
            this.player = player;
            this.slot = slot;
            updateFrame();
        }

        public ItemAnimation animation() { return ItemAnimation.this; }
        public CalibrePlugin getPlugin() { return plugin; }
        public Player player() { return player; }
        public int slot() { return slot; }

        public int index() { return index; }
        public void index(int index) { this.index = index; }

        public Frame frame() { return frame; }
        public void frame(Frame frame) { this.frame = frame; }

        public long frameTime() { return frameTime; }
        public void frameTime(long frameTime) { this.frameTime = frameTime; }

        public boolean finished() { return index < 0 || index >= frames.size(); }

        public Frame updateFrame() { frame = finished() ? null : frames.get(index); return frame; }

        @Override
        public void tick(TickContext tickContext) {
            if (frame == null)
                return;
            frameTime += tickContext.delta();
            while (frame != null && frameTime >= frame.duration) {
                frameTime -= frame.duration;
                nextFrame();
            }
        }

        public void apply() {
            if (frame != null)
                frame.apply(plugin, player, slot);
        }

        public void nextFrame() {
            ++index;
            updateFrame();
            if (frame == null)
                return;
            frame.apply(plugin, player, slot);
        }

        @Override public String toString() { return index + "/" + frameTime + ": [" + frame + "]"; }
    }

    @ConfigSerializable
    public static class Frame {
        private final long duration;
        private final EquipmentSlot slot;
        private final ItemDescriptor item;
        private final Integer modelData;
        private final Integer damage;

        public Frame(long duration, EquipmentSlot slot, ItemDescriptor item) {
            this.duration = duration;
            this.slot = slot;
            this.item = item;
            modelData = null;
            damage = null;
        }

        public Frame(long duration, EquipmentSlot slot, Integer modelData, Integer damage) {
            this.duration = duration;
            this.slot = slot;
            item = null;
            this.modelData = modelData;
            this.damage = damage;
        }

        public Frame() {
            duration = 0;
            slot = null;
            item = null;
            modelData = null;
            damage = null;
        }

        public Frame(Frame o) {
            duration = o.duration;
            slot = o.slot;
            item = o.item;
            modelData = o.modelData;
            damage = o.damage;
        }

        public long duration() { return duration; }
        public EquipmentSlot slot() { return slot; }
        public ItemDescriptor item() { return item; }
        public Integer modelData() { return modelData; }
        public Integer damage() { return damage; }

        public boolean usesItem() { return item != null && item.id() != null; }

        public ItemStack create(CalibrePlugin plugin, ItemStack existing) {
            existing = existing == null ? new ItemStack(Material.AIR) : existing.clone();
            plugin.itemManager().hide(existing, false);

            if (usesItem())
                return item.apply(existing);
            return BukkitUtils.modMeta(existing, meta -> {
                if (modelData != null)
                    meta.setCustomModelData(modelData);
                if (damage != null && meta instanceof Damageable) {
                    Damageable damageable = (Damageable) meta;
                    damageable.setDamage(damage);
                }
            });
        }

        public void apply(CalibrePlugin plugin, Player player, int slot) {
            if (this.slot != null)
                slot = CalibreProtocol.slotOf(player, this.slot);
            CalibreProtocol.item(player, create(plugin, player.getInventory().getItem(slot)), slot);
        }

        @Override public String toString() {
            return duration + "ms: " + (
                    usesItem()
                            ? item
                            : modelData + "/" + damage + " [" + slot + "]"
                    );
        }
    }

    private final CalibrePlugin plugin;
    private List<Frame> frames;

    public ItemAnimation(CalibrePlugin plugin, List<Frame> frames) {
        this.plugin = plugin;
        this.frames = frames;
    }

    public ItemAnimation(ItemAnimation o) {
        plugin = o.plugin;
        frames = o. frames.stream().map(Frame::new).collect(Collectors.toList());
    }

    public CalibrePlugin plugin() { return plugin; }

    public List<Frame> frames() { return frames; }
    public void frames(List<Frame> frames) { this.frames = frames; }

    public Instance start(Player player, int slot) {
        Instance instance = new Instance(plugin, player, slot);
        instance.apply();
        plugin.playerData(player).animation(instance);
        return instance;
    }

    @Override public String toString() { return frames.toString(); }

    public static Instance start(Player player, int slot, ItemAnimation animation) {
        return animation == null ? null : animation.start(player, slot);
    }

    public static Instance start(ItemUser user, ItemSlot<?> slot, ItemAnimation animation) {
        if (!(user instanceof PlayerUser))
            return null;
        Player player = ((PlayerUser) user).entity();
        if (slot instanceof EntityEquipmentSlot)
            return start(player, CalibreProtocol.slotOf(player, ((EntityEquipmentSlot) slot).slot()), animation);
        if (slot instanceof InventorySlot)
            return start(player, ((InventorySlot) slot).slot(), animation);
        return null;
    }
}
