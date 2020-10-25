package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.item.Item;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.impl.BooleanStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Calibre's implementation of a component. Stores systems and stats.
 */
public class CalibreComponent implements CalibreIdentifiable, ComponentHolder<CalibreComponentSlot>, Cloneable, Item {
    public static final String ITEM_TYPE = "component";

    /**
     * A GSON type adapter for this class.
     */
    public static class Adapter implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!CalibreComponent.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<>() {
                @Override public void write(JsonWriter out, T value) throws IOException { delegate.write(out, value); }

                @Override
                public T read(JsonReader in) {
                    JsonElement tree = Streams.parse(in);
                    T value = delegate.fromJsonTree(tree);

                    CalibreComponent component = (CalibreComponent) value;
                    component.dependencies = gson.fromJson(tree, Dependencies.class);

                    return value;
                }
            };
        }
    }

    /**
     * Temporarily stores info on deserialization for resolution later.
     */
    private static class Dependencies {
        @SerializedName("extends")
        private final String extend;
        private Map<String, JsonElement> systems = new HashMap<>();
        private Map<Integer, JsonObject> stats = new LinkedHashMap<>();
        private Map<Integer, JsonObject> completeStats = new LinkedHashMap<>();

        public Dependencies() {
            extend = null;
        }
    }

    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("item", new ItemDescriptor.Stat())
            .init("lower", new BooleanStat(false))
            .get();
    public static final AttributeModifier ATTACK_SPEED_ATTR = new AttributeModifier(new UUID(42069, 69420), "generic.attack_speed", -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HAND);

    private transient Dependencies dependencies;
    private transient CalibrePlugin plugin;
    private final String id;
    private List<String> categories;
    private Map<String, CalibreComponentSlot> slots;
    // todo clean these 2 up, combine them and their copy* methods
    private transient Map<String, CalibreSystem> systems = new HashMap<>();
    private transient Map<Integer, StatMap> stats = new HashMap<>();
    private transient Map<Integer, StatMap> completeStats = new HashMap<>();

    private final transient Map<Class<? extends CalibreSystem>, String> systemServices = new HashMap<>();
    private transient CalibreComponent parent;
    private transient ComponentTree tree;

    public CalibreComponent(CalibrePlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public CalibreComponent() {
        this(null, null);
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    @Override public Map<String, CalibreComponentSlot> getSlots() { return slots; }
    public Map<String, CalibreSystem> getSystems() { return systems; }
    public Map<Integer, StatMap> getStats() { return stats; }
    public Map<Integer, StatMap> getCompleteStats() { return completeStats; }

    public Map<Class<? extends CalibreSystem>, String> getSystemServices() { return systemServices; }

    public CalibreComponent getParent() { return parent; }
    public void setParent(CalibreComponent parent) { this.parent = parent; }

    public ComponentTree getTree() { return tree; }
    public void setTree(ComponentTree tree) { this.tree = tree; }

    //region Systems and services

    public CalibreSystem getSystem(String id) { return systems.get(id); }
    public <T extends CalibreSystem> T getSystem(Class<T> type) {
        for (CalibreSystem sys : systems.values()) {
            if (sys.getClass() == type)
                return type.cast(sys);
        }
        return null;
    }

    public <T extends CalibreSystem> void registerSystemService(Class<T> type, String systemId) {
        systemServices.put(type, systemId);
    }
    public <T extends CalibreSystem> void registerSystemService(Class<T> type, T system) {
        if (systems.containsKey(system.getId()))
            registerSystemService(type, system.getId());
    }
    public Map<Class<? extends CalibreSystem>, CalibreSystem> getMappedServices() {
        Map<Class<? extends CalibreSystem>, CalibreSystem> result = new HashMap<>();
        systemServices.forEach((type, id) -> result.put(type, systems.get(id)));
        return result;
    }
    public <T extends CalibreSystem> T getSystemService(Class<T> type) {
        return type.cast(systems.get(systemServices.get(type)));
    }

    //endregion

    @Override public String getNameKey() { return "component." + id; }

    @Override
    public void validate() throws ValidationException {
        CalibreIdentifiable.super.validate();
        if (slots == null) slots = new LinkedHashMap<>();
        if (dependencies.stats == null) dependencies.stats = new LinkedHashMap<>();
        if (dependencies.systems == null) dependencies.systems = new HashMap<>();
    }

    @Override
    public Collection<String> getDependencies() {
        Collection<String> result = new ArrayList<>();
        result.add(dependencies.extend);
        result.addAll(dependencies.systems.keySet());
        return result;
    }
    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        // Extend
        context.consumeResolve(dependencies.extend, CalibreComponent.class, this::extend);

        // Systems
        dependencies.systems.forEach((id, data) -> systems.put(id, plugin.getGson().fromJson(data, context.getResolve(id, CalibreSystem.class).getClass())));

        // Stats
        Map<String, Stat<?>> stats = new HashMap<>(DEFAULT_STATS);
        systems.values().forEach(sys -> stats.putAll(sys.getDefaultStats()));
        plugin.getStatMapAdapter().setStats(stats);

        try {
            dependencies.stats.forEach((order, map) -> this.stats.put(order, plugin.getGson().fromJson(map, StatMap.class)));
        } catch (JsonParseException e) {
            throw new ResolutionException(TextUtils.format(
                    "Could not deserialize stats for {id}: {msg}",
                    "id", id,
                    "msg", e.getMessage()), e);
        }

        try {
            dependencies.completeStats.forEach((order, map) -> this.completeStats.put(order, plugin.getGson().fromJson(map, StatMap.class)));
        } catch (JsonParseException e) {
            throw new ResolutionException(TextUtils.format(
                    "Could not deserialize complete stats for {id}: {msg}",
                    "id", id,
                    "msg", e.getMessage()), e);
        }

        try {
            tree = ComponentTree.createAndBuild(this);
        } catch (SystemInitializationException e) {
            throw new ResolutionException(TextUtils.format(
                    "Could not build component tree for {id}: {msg}",
                    "id", id,
                    "msg", e.getMessage()), e);
        }

        // Throw away the dependencies object, let it be GC'd
        dependencies = null;
    }

    @Override public String getItemType() { return ITEM_TYPE; }

    @Override
    public ItemStack createItem(@Nullable Player player, int amount) throws ItemCreationException {
        ItemDescriptor desc = stat("item");
        if (desc == null) throw new ItemCreationException("Item descriptor is null");
        ItemStack item = desc.create(amount);
        return Utils.modMeta(item, meta -> {
            callEvent(new ItemEvents.Create(player, amount, item, meta));
            PersistentDataContainer data = meta.getPersistentDataContainer();
            plugin.getItemManager().saveTypeKey(meta, this);
            data.set(plugin.key("tree"), PersistentDataType.STRING, plugin.getGson().toJson(tree));
            if (stat("lower"))
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, ATTACK_SPEED_ATTR);
        });
    }

    /**
     * Add the parent component's attributes to this component.
     * <p>
     * This action modifies:
     * <ul>
     *     <li>slots</li>
     *     <li>stats</li>
     *     <li>systems</li>
     * </ul>
     * @param parent The parent component.
     */
    public void extend(CalibreComponent parent) {
        slots.putAll(parent.copySlots());
        parent.stats.forEach((order, map) -> {
            if (stats.containsKey(order))
                stats.get(order).modifyAll(map);
            else
                stats.put(order, map.copy());
        });
        systems.putAll(parent.copySystems());
    }

    //region Utils

    public <T extends CalibreSystem> void searchSystems(SystemSearchOptions<T> opt, BiConsumer<CalibreComponentSlot, T> consumer) {
        walk(data -> {
            if (data.getSlot() instanceof CalibreComponentSlot) {
                CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
                opt.onEachMatching(slot, consumer);
            }
        });
    }
    public <T extends CalibreSystem> List<Map.Entry<CalibreComponentSlot, T>> collectSystems(SystemSearchOptions<T> opt) {
        List<Map.Entry<CalibreComponentSlot, T>> result = new ArrayList<>();
        searchSystems(opt, (slot, sys) -> result.add(new AbstractMap.SimpleEntry<>(slot, sys)));
        return result;
    }

    public final CalibreComponent withSimpleTree() {
        CalibreComponent copy = copy();
        copy.tree = ComponentTree.createAndBuild(copy);
        return copy;
    }

    public final <T> T stat(String key) { return tree.stat(key); }
    public final <T> T callEvent(T event) { return tree.callEvent(event); }

    //endregion

    // TODO cleanup
    public Map<String, CalibreComponentSlot> copySlots() {
        Map<String, CalibreComponentSlot> map = new LinkedHashMap<>();
        slots.forEach((name, slot) -> map.put(name, slot.copy()));
        return map;
    }
    public Map<Integer, StatMap> copyStats() {
        Map<Integer, StatMap> map = new HashMap<>();
        stats.forEach((order, stats) -> map.put(order, stats.copy()));
        return map;
    }
    public Map<Integer, StatMap> copyCompleteStats() {
        Map<Integer, StatMap> map = new HashMap<>();
        completeStats.forEach((order, stats) -> map.put(order, stats.copy()));
        return map;
    }
    public Map<String, CalibreSystem> copySystems() {
        Map<String, CalibreSystem> map = new HashMap<>();
        systems.forEach((id, sys) -> map.put(id, sys.copy()));
        return map;
    }
    public CalibreComponent clone() { try { return (CalibreComponent) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    public CalibreComponent copy() {
        CalibreComponent copy = clone();
        copy.slots = copySlots();
        copy.stats = copyStats();
        copy.completeStats = copyCompleteStats();
        copy.systems = copySystems();
        return copy;
    }

    /**
     * Modifies a flag of an item which makes it hidden to the player who holds it. Items with this flag do not appear
     * during item animations.
     * @param plugin The {@link CalibrePlugin}.
     * @param item The item to modify. The reference passed will not be cloned.
     * @param hidden Enables the flag or not.
     * @return The modified item.
     * @see CalibreComponent#isHidden(CalibrePlugin, ItemStack) 
     */
    public static ItemStack setHidden(CalibrePlugin plugin, ItemStack item, boolean hidden) {
        return Utils.modMeta(item, meta -> {
            if (hidden)
                meta.getPersistentDataContainer().set(plugin.key("hidden"), PersistentDataType.BYTE, (byte) 1);
            else
                meta.getPersistentDataContainer().remove(plugin.key("hidden"));
        });
    }

    /**
     * Gets if the specified item has a flag which makes it hidden to the player who holds it.
     * @param plugin The {@link CalibrePlugin}.
     * @param item The item to check.
     * @return If the flag is enabled.
     * @see CalibreComponent#setHidden(CalibrePlugin, ItemStack, boolean) 
     */
    public static boolean isHidden(CalibrePlugin plugin, ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta()
                .getPersistentDataContainer()
                .has(plugin.key("hidden"), PersistentDataType.BYTE);
    }
}
