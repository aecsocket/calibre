package com.gitlab.aecsocket.calibre.paper.component;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.proto.Tree;
import com.gitlab.aecsocket.calibre.core.util.ItemCreationException;
import com.gitlab.aecsocket.calibre.paper.util.ItemDescriptor;
import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import com.google.protobuf.Any;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public class PaperComponent extends CalibreComponent<BukkitItem> {
    public static class Serializer implements TypeSerializer<PaperComponent> {
        private final TypeSerializer<PaperComponent> delegate;

        public Serializer(TypeSerializer<PaperComponent> delegate) {
            this.delegate = delegate;
        }

        public TypeSerializer<PaperComponent> delegate() { return delegate; }

        @Override
        public void serialize(Type type, @Nullable PaperComponent obj, ConfigurationNode node) throws SerializationException {
            delegate.serialize(type, obj, node);
        }

        @Override
        public PaperComponent deserialize(Type type, ConfigurationNode node) throws SerializationException {
            PaperComponent obj = delegate.deserialize(type, node);

            obj.slots.clear();
            Map<String, PaperSlot> slots = node.node("slots").get(new TypeToken<>(){});
            if (slots != null)
                obj.slots.putAll(slots);

            return obj;
        }
    }

    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("item", new ItemDescriptor.Stat())

            .init("insert_sound", new SoundDataStat())
            .init("remove_sound", new SoundDataStat())
            .init("modify_sound", new SoundDataStat())
            .get();

    private transient final CalibrePlugin plugin;
    private transient final Map<String, Tree.Component> invalidSlots = new HashMap<>();
    private transient final Map<String, Any> invalidSystems = new HashMap<>();

    public PaperComponent(CalibrePlugin plugin, String id) {
        super(id);
        this.plugin = plugin;
    }

    public PaperComponent() { this(CalibrePlugin.instance(), null); }

    public PaperComponent(CalibreComponent<BukkitItem> o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperComponent(PaperComponent o) {
        this(o, o.plugin);
        invalidSlots.putAll(o.invalidSlots);
        invalidSystems.putAll(o.invalidSystems);
    }

    public CalibrePlugin plugin() { return plugin; }
    public Map<String, Tree.Component> invalidSlots() { return invalidSlots; }
    public Map<String, Any> invalidSystems() { return invalidSystems; }

    @Override
    public StatCollection buildStats() {
        if (!Bukkit.isPrimaryThread())
            new RuntimeException("not main thread when building stats").printStackTrace();
        return super.buildStats();
    }

    @Override public net.kyori.adventure.text.Component gen(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }
    @Override
    protected void prepareStatDeserialization(Map<String, Stat<?>> originals) {
        plugin.statMapSerializer().originals(originals);
    }

    @Override
    public CalibreComponent<BukkitItem> getComponent(BukkitItem item) {
        if (item == null)
            return null;
        return plugin.itemManager().get(item.item());
    }

    @Override
    protected BukkitItem createInitial(int amount) throws ItemCreationException {
        ItemDescriptor descriptor = tree.stat("item");
        if (descriptor == null)
            throw new ItemCreationException("No item descriptor");
        try {
            return BukkitItem.of(BukkitUtils.modMeta(descriptor.create(amount), meta -> {
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            }));
        } catch (IllegalArgumentException e) {
            throw new ItemCreationException(e);
        }
    }

    public ItemStack create(String locale, ItemStack existing) {
        BukkitItem result = create(locale, existing.getAmount());
        return result == null ? null : result.item();
    }

    @Override public PaperComponent buildTree() { return (PaperComponent) super.buildTree(); }

    @Override public PaperComponent copy() { return new PaperComponent(this); }
}
