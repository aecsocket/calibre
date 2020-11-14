package me.aecsocket.calibre.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.search.SlotSearchOptions;
import me.aecsocket.calibre.item.component.search.SystemSearchOptions;
import me.aecsocket.calibre.item.component.search.SystemSearchResult;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.item.util.LoadTimeDependencies;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.user.AnimatableItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.calibre.util.OrderedStatMap;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.item.Item;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Calibre's implementation of a component. Stores systems and stats.
 */
public class CalibreComponent implements CalibreIdentifiable, ComponentHolder<CalibreComponentSlot>, Cloneable, Item {
    public static final String ITEM_TYPE = "component";

    /**
     * Temporarily stores info on deserialization for resolution later.
     */
    protected static class Dependencies {
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

            .init("slot_view_add", new SoundStat())
            .init("slot_view_remove", new SoundStat())
            .init("quick_modify", new SoundStat())
            .get();

    @LoadTimeDependencies private transient Dependencies dependencies;
    private transient CalibrePlugin plugin;
    private final String id;
    private List<String> categories;
    private boolean canComplete;
    private Map<String, CalibreComponentSlot> slots;
    // todo clean these 2 up, combine them and their copy* methods
    private transient Map<String, CalibreSystem> systems = new HashMap<>();
    private transient OrderedStatMap stats = new OrderedStatMap();
    private transient OrderedStatMap completeStats = new OrderedStatMap();

    private transient Map<Class<? extends CalibreSystem>, String> systemServices = new HashMap<>();
    private transient CalibreComponent parent;
    private transient String parentSlotName;
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

    public boolean canComplete() { return canComplete; }
    public void setCanComplete(boolean canComplete) { this.canComplete = canComplete; }

    @Override public Map<String, CalibreComponentSlot> getSlots() { return slots; }
    public Map<String, CalibreSystem> getSystems() { return systems; }
    public OrderedStatMap getStats() { return stats; }
    public OrderedStatMap getCompleteStats() { return completeStats; }

    public Map<Class<? extends CalibreSystem>, String> getSystemServices() { return systemServices; }

    public CalibreComponent getParent() { return parent; }
    public void setParent(CalibreComponent parent) { this.parent = parent; }

    public String getParentSlotName() { return parentSlotName; }
    public void setParentSlotName(String parentSlotName) { this.parentSlotName = parentSlotName; }

    public ComponentTree getTree() { return tree; }
    public void setTree(ComponentTree tree) { this.tree = tree; }

    //region Systems and services

    public CalibreSystem getSystem(String id) { return systems.get(id); }
    public <T extends CalibreSystem> T getSystem(Class<T> type) {
        for (CalibreSystem sys : systems.values()) {
            if (sys.getClass().isAssignableFrom(type))
                return type.cast(sys);
        }
        return null;
    }

    public <T extends CalibreSystem> void registerService(Class<T> type, String systemId) {
        systemServices.put(type, systemId);
    }
    public <T extends CalibreSystem> void registerService(Class<T> type, T system) {
        if (systems.containsKey(system.getId()))
            registerService(type, system.getId());
    }
    public <T extends CalibreSystem> T unregisterService(Class<T> type) {
        CalibreSystem result = systems.get(systemServices.remove(type));
        return result == null ? null : type.cast(result);
    }
    public Map<Class<? extends CalibreSystem>, CalibreSystem> getMappedServices() {
        Map<Class<? extends CalibreSystem>, CalibreSystem> result = new HashMap<>();
        systemServices.forEach((type, id) -> result.put(type, systems.get(id)));
        return result;
    }
    public <T extends CalibreSystem> T getService(Class<T> type) {
        return type.cast(systems.get(systemServices.get(type)));
    }

    //endregion

    @Override public String getNameKey() { return "component." + id; }

    @Override
    public String getLongInfo(String locale) {
        String none = plugin.gen(locale, "none");
        String slotSeparator = plugin.gen(locale, "info.component.slot");
        String systemSeparator = plugin.gen(locale, "info.component.system");
        String statSeparator = plugin.gen(locale, "info.component.stat");
        return plugin.gen(locale, "info.component",
                "localized_name", getLocalizedName(locale),
                "categories", categories.size() == 0 ? none : String.join(", ", categories),
                "slots", slots.size() == 0
                        ? none
                        : slotSeparator + slots.entrySet().stream()
                            .map(entry -> entry.getValue().getInfo(plugin, entry.getKey(), locale))
                            .collect(Collectors.joining(slotSeparator)),
                "systems", systems.size() == 0
                        ? none
                        : systemSeparator + systems.values().stream()
                            .flatMap(sys -> Stream.of(sys.getLongInfo(locale).split("\n")))
                            .collect(Collectors.joining(systemSeparator)),
                "stats", stats.size() == 0
                        ? none
                        : statSeparator + String.join(statSeparator, stats.getInfo(plugin, locale).split("\n")),
                "complete_stats", completeStats.size() == 0
                        ? none
                        : completeStats.getInfo(plugin, locale)
        );
    }

    @Override
    public void validate() throws ValidationException {
        CalibreIdentifiable.super.validate();
        if (categories == null) categories = new ArrayList<>();
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
        dependencies.systems.forEach((id, data) -> {
            CalibreSystem base = context.getResolve(id, CalibreSystem.class);
            CalibreSystem sys = plugin.getGson().fromJson(data, base.getClass());
            if (sys == null)
                throw new ResolutionException(TextUtils.format(
                        "Could not create system {sys} for {id}",
                        "sys", base.getId(),
                        "id", id
                ));
            sys.getServiceTypes().forEach(type -> {
                if (!type.isInstance(sys))
                    throw new ResolutionException(TextUtils.format(
                            "System {sys} is not of declared service type {type} on {id}",
                            "sys", sys.getId(),
                            "type", type.getName(),
                            "id", sys.getId()
                    ));
                registerService(type, sys.getId());
            });
            systems.put(id, sys);
        });

        // Stats
        Map<String, Stat<?>> stats = new HashMap<>(DEFAULT_STATS);
        systems.values().forEach(sys -> stats.putAll(sys.getDefaultStats()));
        plugin.getStatMapAdapter().setStats(stats);

        try {
            dependencies.stats.forEach((order, map) -> this.stats.combine(order, plugin.getGson().fromJson(map, StatMap.class)));
        } catch (JsonParseException e) {
            throw new ResolutionException(TextUtils.format(
                    "Could not deserialize stats for {id}: {msg}",
                    "id", id,
                    "msg", e.getMessage()), e);
        }

        try {
            dependencies.completeStats.forEach((order, map) -> this.completeStats.combine(order, plugin.getGson().fromJson(map, StatMap.class)));
        } catch (JsonParseException e) {
            throw new ResolutionException(TextUtils.format(
                    "Could not deserialize complete stats for {id}: {msg}",
                    "id", id,
                    "msg", e.getMessage()), e);
        }

        try {
            systems.values().forEach(sys -> sys.systemInitialize(this));
            tree = ComponentTree.createAndBuild(this);
        } catch (SystemInitializationException e) {
            throw new ResolutionException(TextUtils.format(
                    "Could not build component tree for {id}: {msg}",
                    "id", id,
                    "msg", e.getMessage()), e);
        }

        plugin.getStatMapAdapter().setStats(null);

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
        });
    }

    public ItemStack updateItem(ItemUser user, ItemSlot slot) {
        ItemStack item = createItem(
                user instanceof PlayerItemUser
                ? ((PlayerItemUser) user).getEntity()
                : null, slot.get().getAmount());
        tree.callEvent(new ItemEvents.Update(item, slot, user));
        if (user instanceof AnimatableItemUser && ((AnimatableItemUser) user).getAnimation() != null)
            setHidden(plugin, item, true);
        slot.set(item);
        return item;
    }

    /**
     * Add the parent component's attributes to this component.
     * <p>
     * This action modifies:
     * <ul>
     *     <li>categories</li>
     *     <li>slots</li>
     *     <li>stats</li>
     *     <li>systems</li>
     * </ul>
     * @param parent The parent component.
     */
    public void extend(CalibreComponent parent) {
        categories.addAll(parent.categories);
        slots.putAll(parent.copySlots());
        parent.stats.forEach((order, map) -> {
            if (stats.containsKey(order))
                stats.get(order).modifyAll(map);
            else
                stats.put(order, map.copy());
        });
        systems.putAll(parent.copySystems());
        systemServices.putAll(parent.systemServices);
    }

    //region Utils

    public CalibreComponentSlot combine(CalibreComponent mod, boolean limitedModification) {
        CalibreComponentSlot[] result = new CalibreComponentSlot[]{null};
        walk(data -> {
            if (result[0] != null) return;
            if (data.getSlot() instanceof CalibreComponentSlot) {
                CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
                if (
                        slot.get() == null
                        && slot.isCompatible(mod)
                        && (slot.canFieldModify() || !limitedModification)
                ) {
                    slot.set(mod);
                    result[0] = slot;
                }
            }
        });
        return result[0];
    }

    public void searchSlots(SlotSearchOptions opt, Consumer<CalibreComponentSlot> consumer) {
        walk(data -> {
            if (data.getSlot() instanceof CalibreComponentSlot) {
                CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
                if (opt.matches(slot))
                    consumer.accept(slot);
            }
        });
    }
    public List<CalibreComponentSlot> collectSlots(SlotSearchOptions opt) {
        List<CalibreComponentSlot> result = new ArrayList<>();
        searchSlots(opt, result::add);
        return result;
    }

    public <T extends CalibreSystem> void searchSystems(SystemSearchOptions<T> opt, BiConsumer<CalibreComponentSlot, T> consumer) {
        searchSlots(opt, slot -> opt.onEachMatching(slot, consumer));
    }
    public <T extends CalibreSystem> List<SystemSearchResult<T>> collectSystems(SystemSearchOptions<T> opt) {
        List<SystemSearchResult<T>> result = new ArrayList<>();
        searchSystems(opt, (slot, sys) -> result.add(new SystemSearchResult<>(slot, sys)));
        return result;
    }
    public <T extends CalibreSystem> SystemSearchResult<T> firstOf(SystemSearchOptions<T> opt, BiPredicate<CalibreComponentSlot, T> predicate) {
        AtomicReference<SystemSearchResult<T>> result = new AtomicReference<>();
        searchSystems(opt, (slot, sys) -> {
            if (result.get() != null || (predicate != null && !predicate.test(slot, sys))) return;
            result.set(new SystemSearchResult<>(slot, sys));
        });
        return result.get();
    }
    public <T extends CalibreSystem> SystemSearchResult<T> firstOf(SystemSearchOptions<T> opt) { return firstOf(opt, null); }

    public final CalibreComponent withSimpleTree() {
        CalibreComponent copy = copy();
        ComponentTree.createAndBuild(copy);
        return copy;
    }
    public final CalibreComponent withSingleTree() {
        CalibreComponent copy = copy();
        copy.slots = Collections.emptyMap();
        ComponentTree.createAndBuild(copy);
        copy.tree.setComplete(false);
        return copy;
    }

    public final <T> T stat(String key) { return tree.stat(key); }
    public final <T> T callEvent(T event) { return tree.callEvent(event); }

    public CalibreComponentSlot getParentSlot() {
        // use #getSlots directly because it's faster
        return parent.getSlots().get(parentSlotName);
    }

    public final boolean isRoot() { return tree.getRoot() == this; }
    public final String[] getTreePath() {
        if (getParent() == null) return new String[0];
        String[] parents = getParent().getTreePath();
        String[] result = new String[parents.length + 1];
        System.arraycopy(parents, 0, result, 0, parents.length);
        result[result.length - 1] = parentSlotName;
        return result;
    }
    public final String getJoinedTreePath() { return String.join(".", getTreePath()); }

    //endregion

    // TODO cleanup
    public Map<String, CalibreComponentSlot> copySlots() {
        Map<String, CalibreComponentSlot> map = new LinkedHashMap<>();
        slots.forEach((name, slot) -> map.put(name, slot.copy()));
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
        copy.stats = stats.copy();
        copy.completeStats = completeStats.copy();
        copy.systems = copySystems();
        copy.systemServices = new HashMap<>(systemServices);
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categories, slots, systems, stats, completeStats);
    }

    @Override public String toString() { return "CalibreComponent:" + id + slots; }

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
