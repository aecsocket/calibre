package me.aecsocket.calibre.item.animation;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.calibre.util.protocol.CalibreProtocol;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;

public class Animation {
    public class Instance implements Tickable {
        private final Player player;
        private final EquipmentSlot slot;
        private int index;
        private Frame frame;
        private long frameTime;

        public Instance(Player player, EquipmentSlot slot, int index, Frame frame) {
            this.player = player;
            this.slot = slot;
            this.index = index;
            this.frame = frame;
        }

        public Instance(Player player, EquipmentSlot slot) {
            this.player = player;
            this.slot = slot;
            updateFrame();
        }

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
            if (frame != null) frame.apply(player, slot);
        }

        public void nextFrame() {
            ++index;
            frameTime -= frame.duration;
            updateFrame();
            if (frame == null) return;
            frame.apply(player, slot);
        }
    }

    public static class Frame {
        private final CalibrePlugin plugin;
        private long duration;
        private EquipmentSlot slot;
        private ItemDescriptor item;
        private Integer modelData;
        private Integer damage;

        public Frame(CalibrePlugin plugin, long duration, EquipmentSlot slot, ItemDescriptor item) {
            this.plugin = plugin;
            this.duration = duration;
            this.slot = slot;
            this.item = item;
        }

        public Frame(CalibrePlugin plugin, long duration, ItemDescriptor item) {
            this.plugin = plugin;
            this.duration = duration;
            this.item = item;
        }

        public Frame(CalibrePlugin plugin, long duration, EquipmentSlot slot, Integer modelData, Integer damage) {
            this.plugin = plugin;
            this.duration = duration;
            this.slot = slot;
            this.modelData = modelData;
            this.damage = damage;
        }

        public Frame(CalibrePlugin plugin, long duration, Integer modelData, Integer damage) {
            this.plugin = plugin;
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

        public ItemStack create(Player player, EquipmentSlot slot) {
            ItemStack playerItem = CalibreItem.setHidden(plugin, player.getEquipment().getItem(slot).clone(), false);
            if (item != null) return item.apply(playerItem);
            return Utils.modMeta(playerItem, meta -> {
                if (modelData != null) meta.setCustomModelData(modelData);
                if (damage != null && meta instanceof Damageable) ((Damageable) meta).setDamage(damage);
            });
        }

        public void apply(Player player, EquipmentSlot slot) {
            if (this.slot != null) slot = this.slot;
            CalibreProtocol.sendItem(player, create(player, slot), slot);
        }

        @Override
        public String toString() {
            return duration + "ms / " + slot + "{" + (
                    item == null
                    ? "modelData=" + modelData + ", damage=" + damage
                    : item
           ) + "}";
        }
    }

    private List<Frame> frames = new ArrayList<>();

    public Animation(List<Frame> frames) {
        this.frames = frames;
    }

    public Animation(Animation o) {
        this.frames = new ArrayList<>(o.frames);
    }

    public Animation() {}

    public List<Frame> getFrames() { return frames; }
    public void setFrames(List<Frame> frames) { this.frames = frames; }

    public Instance start(Player player, EquipmentSlot slot) {
        Instance instance = new Instance(player, slot);
        instance.apply();
        return instance;
    }

    @Override
    public String toString() { return frames.toString(); }
}
