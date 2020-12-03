package me.aecsocket.calibre.item;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.serialization.json.JsonAdapter;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemAnimation implements Cloneable, AcceptsCalibrePlugin {
    public static class Adapter implements JsonDeserializer<ItemAnimation>, JsonAdapter {
        private final CalibrePlugin plugin;

        public Adapter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        @Override
        public ItemAnimation deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonArray array = assertArray(json);
            List<Frame> frames = new ArrayList<>();
            for (JsonElement elem : array) {
                JsonObject object = assertObject(elem);
                String frameType = get(object, "type", new JsonPrimitive("simple")).getAsString();
                switch (frameType) {
                    case "range":
                        long duration = get(object, "duration", long.class, 0L);
                        int from = get(object, "from", int.class);
                        int alt = get(object, "alt", int.class, null);

                        EquipmentSlot slot = context.deserialize(get(object, "slot", JsonNull.INSTANCE), EquipmentSlot.class);
                        int step = get(object, "step", int.class, 1);
                        boolean useDamage = get(object, "use_damage", boolean.class, true);

                        for (int i = 0; i < get(object, "range", int.class); i++) {
                            frames.add(new Frame(
                                    duration, slot,
                                    useDamage ? alt : from + (i * step),
                                    useDamage ? from + (i * step) : alt
                            ));
                        }
                        break;
                    default:
                        frames.add(new Frame(
                                get(object, "duration", long.class),
                                get(object, "slot", context, EquipmentSlot.class, null),
                                get(object, "item", context, ItemDescriptor.class, null),
                                get(object, "model_data", context, Integer.class, null),
                                get(object, "damage", context, Integer.class, null)
                        ));
                        break;
                }
            }

            return new ItemAnimation(plugin, frames);
        }
    }

    public class Instance implements Tickable {
        private final CalibrePlugin plugin;
        private final Player player;
        private final EquipmentSlot slot;
        private int index;
        private Frame frame;
        private long frameTime;

        public Instance(CalibrePlugin plugin, Player player, EquipmentSlot slot, int index, Frame frame) {
            this.plugin = plugin;
            this.player = player;
            this.slot = slot;
            this.index = index;
            this.frame = frame;
        }

        public Instance(CalibrePlugin plugin, Player player, EquipmentSlot slot) {
            this.plugin = plugin;
            this.player = player;
            this.slot = slot;
            updateFrame();
        }

        public CalibrePlugin getPlugin() { return plugin; }

        public Player getPlayer() { return player; }
        public EquipmentSlot getSlot() { return slot; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public Frame getFrame() { return frame; }
        public void setFrame(Frame frame) { this.frame = frame; }
        public Frame updateFrame() { frame = isFinished() ? null : frames.get(index); return frame; }

        public long getFrameTime() { return frameTime; }
        public void setFrameTime(long frameTime) { this.frameTime = frameTime; }

        public boolean isFinished() { return index < 0 || index >= frames.size(); }

        @Override
        public void tick(TickContext tickContext) {
            if (frame == null) return;
            frameTime += tickContext.getPeriod();
            while (frame != null && frameTime >= frame.duration)
                nextFrame();
        }

        public void apply() {
            if (frame != null) frame.apply(plugin, player, slot);
        }

        public void nextFrame() {
            ++index;
            frameTime -= frame.duration;
            updateFrame();
            if (frame == null) return;
            frame.apply(plugin, player, slot);
        }

        public ItemAnimation getAnimation() { return ItemAnimation.this; }

        @Override public String toString() { return index + "/" + frameTime + ": [" + frame + "]"; }
    }

    public static class Frame implements Cloneable {
        private long duration;
        private EquipmentSlot slot;
        private ItemDescriptor item; // TODO use an "item delta" object, diff between items OR default real item22
        private Integer modelData;
        private Integer damage;

        public Frame(long duration, EquipmentSlot slot, ItemDescriptor item, Integer modelData, Integer damage) {
            this.duration = duration;
            this.slot = slot;
            this.item = item;
            this.modelData = modelData;
            this.damage = damage;
        }

        public Frame(long duration, EquipmentSlot slot, ItemDescriptor item) {
            this.duration = duration;
            this.slot = slot;
            this.item = item;
        }

        public Frame(long duration, ItemDescriptor item) {
            this.duration = duration;
            this.item = item;
        }

        public Frame(long duration, EquipmentSlot slot, Integer modelData, Integer damage) {
            this.duration = duration;
            this.slot = slot;
            this.modelData = modelData;
            this.damage = damage;
        }

        public Frame(long duration, Integer modelData, Integer damage) {
            this.duration = duration;
            this.modelData = modelData;
            this.damage = damage;
        }

        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }

        public ItemDescriptor getItem() { return item; }
        public void setItem(ItemDescriptor item) { this.item = item; }

        public EquipmentSlot getSlot() { return slot; }
        public void setSlot(EquipmentSlot slot) { this.slot = slot; }

        public Integer getModelData() { return modelData; }
        public void setModelData(Integer modelData) { this.modelData = modelData; }

        public Integer getDamage() { return damage; }
        public void setDamage(Integer damage) { this.damage = damage; }

        public ItemStack create(CalibrePlugin plugin, Player player, EquipmentSlot slot) {
            ItemStack playerItem = player.getEquipment().getItem(slot);
            playerItem = playerItem == null ? new ItemStack(Material.AIR) : CalibreComponent.setHidden(plugin, playerItem.clone(), false);
            if (item != null) return item.apply(playerItem);
            return Utils.modMeta(playerItem, meta -> {
                if (modelData != null) meta.setCustomModelData(modelData);
                if (damage != null && meta instanceof Damageable) ((Damageable) meta).setDamage(damage);
            });
        }

        public void apply(CalibrePlugin plugin, Player player, EquipmentSlot slot) {
            if (this.slot != null) slot = this.slot;
            CalibreProtocol.sendItem(player, create(plugin, player, slot), slot);
        }

        public Frame clone() { try { return (Frame) super.clone(); } catch (CloneNotSupportedException e) { return null; } }

        @Override public String toString() { return duration + "ms: " + modelData + "/" + damage + " [" + slot + "]"; }
    }

    private transient CalibrePlugin plugin;
    private List<Frame> frames = new ArrayList<>();

    public ItemAnimation(CalibrePlugin plugin, List<Frame> frames) {
        this.plugin = plugin;
        this.frames = frames;
    }

    public ItemAnimation(ItemAnimation o) {
        plugin = o.plugin;
        frames = new ArrayList<>(o.frames);
    }

    public ItemAnimation(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public List<Frame> getFrames() { return frames; }
    public void setFrames(List<Frame> frames) { this.frames = frames; }

    public Instance start(Player player, EquipmentSlot slot) {
        Instance instance = new Instance(plugin, player, slot);
        instance.apply();
        return instance;
    }

    public ItemAnimation clone() { try { return (ItemAnimation) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    public ItemAnimation copy() {
        ItemAnimation copy = clone();
        copy.frames = frames.stream().map(Frame::clone).collect(Collectors.toList());
        return copy;
    }

    @Override public String toString() { return frames.toString(); }
}
