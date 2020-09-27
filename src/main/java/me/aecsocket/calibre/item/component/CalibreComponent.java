package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.calibre.item.system.SystemSearchOptions;
import me.aecsocket.calibre.item.system.SystemSearchResult;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.calibre.util.itemuser.PlayerItemUser;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.resource.ResourceLoadException;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A component, which can be nested to create component trees. The building block of an item.
 */
public class CalibreComponent implements CalibreItem, Component, ComponentHolder<CalibreComponentSlot>, AcceptsCalibrePlugin {
    public static final String ITEM_TYPE = "component";

    private transient CalibrePlugin plugin;
    private String id;
    private List<String> categories = new ArrayList<>();
    private boolean completeRoot;
    private Map<String, CalibreComponentSlot> slots = new LinkedHashMap<>();
    private transient Map<String, CalibreSystem<?>> systems = new HashMap<>();
    private transient Map<Class<?>, String> services = new HashMap<>();
    private transient StatMap stats = new StatMap();
    private ItemDescriptor item;

    private transient CalibreComponent parent;
    private transient ComponentTree tree;

    public CalibreComponent(CalibrePlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public CalibreComponent() {}

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public boolean isCompleteRoot() { return completeRoot; }
    public void setCompleteRoot(boolean completeRoot) { this.completeRoot = completeRoot; }

    @Override public Map<String, CalibreComponentSlot> getSlots() { return slots; }
    public void setSlots(Map<String, CalibreComponentSlot> slots) { this.slots = slots; }

    public Map<String, CalibreSystem<?>> getSystems() { return systems; }
    public void setSystems(Map<String, CalibreSystem<?>> systems) { this.systems = systems; }

    public Map<Class<?>, String> getServices() { return services; }
    public void setServices(Map<Class<?>, String> services) { this.services = services; }

    public StatMap getStats() { return stats; }
    public void setStats(StatMap stats) { this.stats = stats; }

    public ItemDescriptor getItem() { return item; }
    public void setItem(ItemDescriptor item) { this.item = item; }

    public CalibreComponent getParent() { return parent; }
    public void setParent(CalibreComponent parent) { this.parent = parent; }

    public ComponentTree getTree() { return tree; }
    public void setTree(ComponentTree tree) { this.tree = tree; }

    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        if (context.getJson() == null) return;
        JsonAdapter util = JsonAdapter.INSTANCE;
        JsonObject json = util.assertObject(context.getJson());

        // Load base
        if (json.has("base")) {
            String baseId = json.get("base").getAsString();
            CalibreComponent base = context.get(baseId, CalibreComponent.class);
            if (base == null)
                throw new ResolutionException("Invalid base component " + baseId);
            extendBase(base);
            try {
                load(context, json);
            } catch (ResourceLoadException e) {
                throw new ResolutionException(e);
            }
        }
    }

    /**
     * Loads systems and stats. Mainly for internal use.
     * @param context The {@link ResolutionContext}.
     * @param json The {@link JsonObject}.
     * @throws ResourceLoadException If something errors.
     */
    public void load(ResolutionContext context, JsonObject json) throws ResourceLoadException {
        JsonAdapter util = JsonAdapter.INSTANCE;
        Gson gson = plugin.getGson();
        StatMapAdapter statMapAdapter = plugin.getStatMapAdapter();

        // Load systems
        // Generate the systems
        JsonObject object = util.assertObject(util.get(json, "systems", new JsonObject()));
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String systemId = entry.getKey();
            CalibreSystem<?> system = context.get(systemId, CalibreSystem.class);
            if (system == null)
                throw new ResourceLoadException(TextUtils.format("System {id} does not exist", "id", systemId));
            system = gson.fromJson(entry.getValue(), system.getClass());

            systems.put(systemId, system);
        }
        // Update service map
        updateServices();
        // Validate the systems
        systems.forEach((id, system) -> {
            Collection<Class<?>> conflicts = system.getConflicts();
            Collection<Class<?>> dependencies = new ArrayList<>(system.getDependencies());
            services.forEach((type, id2) -> {
                if (conflicts.contains(type))
                    throw new ResourceLoadException(
                            TextUtils.format("System {id} is not compatible with system type {type}",
                                    "id", id,
                                    "type", type.getSimpleName()
                            )
                    );
                dependencies.remove(type);
            });
            if (dependencies.size() > 0)
                throw new ResourceLoadException(
                        TextUtils.format("System {id} has not had all dependencies fulfilled: {deps}",
                                "id", id,
                                "deps", String.join(", ", dependencies.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")))
                        )
                );
        });

        // Load stats
        // Combine all system stats into one stat map
        Collection<CalibreSystem<?>> statSystems = new ArrayList<>();
        statSystems.addAll(systems.values());
        if (json.has("stat_systems")) {
            util.assertArray(json.get("stat_systems")).forEach(elem -> {
                String systemId = util.assertStringPrimitive(util.assertPrimitive(elem));
                CalibreSystem<?> system = context.get(systemId, CalibreSystem.class);
                if (system == null)
                    throw new ResourceLoadException(TextUtils.format("System {id} does not exist", "id", systemId));
                statSystems.add(system);
            });
        }

        Map<String, Stat<?>> stats = new LinkedHashMap<>();
        statSystems.forEach(system -> {
            Map<String, Stat<?>> toAdd = system.getDefaultStats();
            if (toAdd != null) stats.putAll(toAdd);
        });
        // Load stats with that map
        statMapAdapter.setStats(stats);
        this.stats.addAll(gson.fromJson(util.get(json, "stats", new JsonObject()), StatMap.class));

        // Create simple component tree for this instance
        tree = ComponentTree.of(this);
    }

    /**
     * Makes this component inherit the same:
     * <ul>
     *     <li>slots</li>
     *     <li>systems</li>
     *     <li>stats</li>
     * </ul>
     * as the provided base. Existing fields will be merged, with the new ones taking precedence.
     * All collections involved are deep cloned. This may be an intensive operation.
     * @param base The base component.
     */
    public void extendBase(CalibreComponent base) {
        slots.putAll(base.copySlots());
        systems.putAll(CalibreSystem.copyMap(base.systems));
        stats.addAll(base.stats);
    }

    /**
     * Updates the service map to properly map services to systems.
     * @throws ResourceLoadException If a service could not be mapped to a system.
     */
    public void updateServices() throws ResourceLoadException {
        services.clear();
        systems.forEach((id, system) -> {Collection<Class<?>> systemServices = system.getServiceTypes();
            systemServices = systemServices == null ? new HashSet<>() : new HashSet<>(systemServices);
            systemServices.add(system.getClass());

            for (Class<?> serviceType : systemServices) {
                if (!serviceType.isAssignableFrom(system.getClass()))
                    throw new ResourceLoadException(TextUtils.format("System {id} states it implements service {service} but does not extend class",
                            "service", serviceType.getSimpleName(),
                            "id", id));
                if (services.containsKey(serviceType))
                    throw new ResourceLoadException(TextUtils.format("Component already has system implementing service type {service} (on {id})",
                            "service", serviceType.getSimpleName(),
                            "id", id));
                services.put(serviceType, id);
            }
        });
    }

    /**
     * Gets if this component is the root component in its tree.
     * @return The result.
     */
    public boolean isRoot() { return tree.getRoot() == this; }

    /**
     * Gets if this component is part of a complete tree.
     * @return The result.
     * @see ComponentTree#isComplete()
     */
    public boolean isComplete() { return tree.isComplete(); }

    @Override
    public <T> T callEvent(T event) { tree.getEventDispatcher().call(event); return event; }

    /**
     * When creating a tree of components, modifies the tree to this instance's needs, such as
     * adding its stats to the tree.
     * <p>
     * In {@link ComponentDescriptor#create(Registry)}, this method is called breadth-first in the tree, so the
     * root component gets this method called first.
     * @param tree The tree.
     */
    public void modifyTree(ComponentTree tree) {
        tree.getStats().addAll(stats);
    }

    /**
     * Gets a system on this component by its ID.
     * @param id The ID of the system.
     * @param <T> The system type.
     * @return The system.
     */
    @SuppressWarnings("unchecked")
    public <T extends CalibreSystem<?>> T getSystem(String id) { return (T) systems.get(id); }

    /**
     * Gets a service system on this component by its type.
     * @param service The type of service.
     * @param <T> The service type.
     * @return The service system.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> service) { return (T) systems.get(services.get(service)); }

    @Override public String getItemType() { return ITEM_TYPE; }

    @Override
    public ItemStack createItem(@Nullable Player player, int amount) throws ItemCreationException {
        if (item == null) throw new ItemCreationException("Item is null");
        ItemStack result = item.create();
        result.setAmount(amount);
        plugin.getItemManager().saveTypeKey(result, this);
        return Utils.modMeta(result, meta -> {
            PersistentDataContainer data = meta.getPersistentDataContainer();

            data.set(plugin.key("data"), PersistentDataType.STRING, plugin.getGson().toJson(ComponentDescriptor.of(this)));

            meta.setDisplayName(tree != null && isRoot() && tree.isComplete() && completeRoot
                    ? getCompleteLocalizedName(player)
                    : getLocalizedName(player));

            List<String> sections = new ArrayList<>();
            /*ItemEvents.ItemCreation event = new ItemEvents.ItemCreation(
                    player, amount, result, meta, sections
            );
            callEvent(event);*/

            StringJoiner lore = new StringJoiner(plugin.gen(player, "lore.separator"));
            sections.forEach(lore::add);
            List<String> loreLines = Arrays.asList(lore.toString().split("\n"));
            if (loreLines.size() > 0 && !(loreLines.size() == 1 && loreLines.get(0).length() == 0))
                meta.setLore(loreLines);
        });
    }

    /**
     * Sets the item in the equipment slot of the specified entity's inventory to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * This retains the stack amount of the old slot.
     * @param user The user to get items from, and if a {@link me.aecsocket.calibre.util.itemuser.PlayerItemUser}, to be passed to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * @param slot The equipment slot.
     * @param hidden If the item should be passed through {@link CalibreComponent#setHidden(CalibrePlugin, ItemStack)} to improve animations.
     * @return The new item.
     */
    public ItemStack updateItem(ItemUser user, EquipmentSlot slot, boolean hidden) {
        ItemStack item = createItem(user instanceof PlayerItemUser ? ((PlayerItemUser) user).getEntity() : null, user.getItem(slot));
        ItemEvents.ItemUpdate event = new ItemEvents.ItemUpdate(item, slot, user, hidden);
        callEvent(event);
        hidden = event.isHidden();
        if (hidden) CalibreItem.setHidden(plugin, item);
        user.setItem(slot, item);
        return item;
    }

    /**
     * Sets the item in the equipment slot of the specified entity's inventory to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * This retains the stack amount of the old slot.
     * @param user The user to get items from, and if a {@link me.aecsocket.calibre.util.itemuser.PlayerItemUser}, to be passed to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * @param slot The equipment slot.
     * @param animationUsed The animation applied to this item before updating.
     * @return The new item.
     */
    public ItemStack updateItem(ItemUser user, EquipmentSlot slot, Animation animationUsed) {
        return updateItem(user, slot, animationUsed != null);
    }

    /**
     * Sets the item in the equipment slot of the specified entity's inventory to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * This retains the stack amount of the old slot.
     * @param user The user to get items from, and if a {@link me.aecsocket.calibre.util.itemuser.PlayerItemUser}, to be passed to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * @param slot The equipment slot.
     * @return The new item.
     */
    public ItemStack updateItem(ItemUser user, EquipmentSlot slot) {
        return updateItem(user, slot, false);
    }

    /**
     * Generic method for combining another component into this component tree. This is achieved by
     * recursively walking through this component's slots and finding one which is:
     * <ol>
     *     <li>empty</li>
     *     <li>is compatible</li>
     *     <li>if modification is limited, passes {@link CalibreComponentSlot#canFieldModify()}</li>
     * </ol>
     * then sets the component.
     * @param other The component to add on to this.
     * @param limitModification If a slot has to pass {@link CalibreComponentSlot#canFieldModify()}.
     * @return The modified slot.
     */
    public CalibreComponentSlot combine(CalibreComponent other, boolean limitModification) {
        CalibreComponentSlot[] target = {null};
        walk(data -> {
            if (!(data.getSlot() instanceof CalibreComponentSlot)) return;
            CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
            if (slot.get() == null && slot.isCompatible(other) && (!limitModification || slot.canFieldModify()))
                target[0] = slot;
        });
        if (target[0] == null) return null;
        target[0].set(other);
        return target[0];
    }

    /**
     * Generic method for combining another component into this component tree. This is achieved by
     * recursively walking through this component's slots and finding one which is:
     * <ol>
     *     <li>empty</li>
     *     <li>is compatible</li>
     *     <li>modification is limited, passes {@link CalibreComponentSlot#canFieldModify()}</li>
     * </ol>
     * then sets the component.
     * This version does not have limited modification.
     * @param other The component to add on to this.
     * @return The modified slot.
     */
    public CalibreComponentSlot combine(CalibreComponent other) { return combine(other, false); }

    /**
     * Iterates through all slots, components and systems which match the {@link SystemSearchOptions} and runs the
     * consumer on them.
     * @param options The {@link SystemSearchOptions}.
     * @param consumer The action to run for each {@link SystemSearchResult}. If returns true, will stop searching.
     * @param <T> The type of system to search for.
     */
    @SuppressWarnings("unchecked")
    public <T> void searchSystems(SystemSearchOptions<T> options, Predicate<SystemSearchResult<T>> consumer) {
        boolean[] exit = {false};
        walk(data -> {
            if (exit[0]) return;
            if (!(data.getSlot() instanceof CalibreComponentSlot)) return;
            CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
            if (!options.test(slot)) return;

            CalibreComponent component = slot.get();
            if (component == null) return;
            if (options.getSystemType() == null)
                component.getSystems().values().forEach(system ->
                        exit[0] = consumer.test(new SystemSearchResult<>((T) system, data)));
            else {
                T result = component.getService(options.getSystemType());
                if (result != null)
                    exit[0] = consumer.test(new SystemSearchResult<>(result, data));
            }
        });
    }

    /**
     * Gets the localized name of the item in its complete form, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link me.aecsocket.calibre.item.CalibreIdentifiable#getCalibreType()}.{@link me.aecsocket.unifiedframework.registry.Identifiable#getId()}.complete</code>.
     * @param locale The locale to use.
     * @return The localized name.
     */
    public String getCompleteLocalizedName(String locale) { return getPlugin().gen(locale, getCalibreType() + "." + getId() + ".complete"); }

    /**
     * Gets the localized name of the item in its complete form, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link me.aecsocket.calibre.item.CalibreIdentifiable#getCalibreType()}.{@link me.aecsocket.unifiedframework.registry.Identifiable#getId()}.complete</code>.
     * @param sender The command sender to use the locale for.
     * @return The localized name.
     */
    public String getCompleteLocalizedName(CommandSender sender) { return getCompleteLocalizedName(sender instanceof Player ? ((Player) sender).getLocale() : getPlugin().getLocaleManager().getDefaultLocale()); }

    @Override
    public @Nullable String getLongInfo(CommandSender sender) {
        String prefix = plugin.gen(sender, "chat.info.prefix");
        return plugin.gen(sender, "chat.component.info",
                "localized_name", getLocalizedName(sender),
                "categories", String.join(", ", categories),
                "slots", TextUtils.prefixLines(
                        slots.size() == 0 ? plugin.gen(sender, "chat.info.empty") :
                        slots.entrySet().stream().map(entry -> plugin.gen(
                                sender, "chat.component.info.slots.entry",
                                "name", entry.getKey(),
                                "slot", TextUtils.prefixLines(entry.getValue().getLongInfo(plugin, sender), prefix.repeat(2)))).collect(Collectors.joining("\n"))
                        , prefix),
                "systems", TextUtils.prefixLines(
                        systems.size() == 0 ? plugin.gen(sender, "chat.info.empty") :
                                systems.values().stream().map(system -> {
                                    String info = system.getLongInfo(sender);
                                    if (info == null)
                                        return plugin.gen(
                                                sender, "chat.component.info.systems.entry.no_info",
                                                "id", system.getId(),
                                                "type", system.getClass().getSimpleName());
                                    else
                                        return plugin.gen(
                                                sender, "chat.component.info.systems.entry.info",
                                                "id", system.getId(),
                                                "type", system.getClass().getSimpleName(),
                                                "info", TextUtils.prefixLines(info, prefix.repeat(2)));
                                }).collect(Collectors.joining("\n"))
                        , prefix),
                "stats", TextUtils.prefixLines(
                        stats.size() == 0 ? plugin.gen(sender, "chat.info.empty") :
                        stats.entrySet().stream().map(entry -> plugin.gen(
                                sender, "chat.component.info.stats.entry",
                                "key", entry.getKey(),
                                "type", TypeToken.get(entry.getValue().getStat().getValueType()).getRawType().getSimpleName(),
                                "value", entry.getValue().valueToString(),
                                "default", entry.getValue().getStat().getDefaultValue())).collect(Collectors.joining("\n"))
                        , prefix),
                "item", TextUtils.prefixLines(item == null ? "null" : item.getLongInfo(plugin, sender), prefix)
                );
    }

    /**
     * Creates a copy of this component, with a simple {@link ComponentTree}.
     * @return The copy with a simple component tree.
     */
    public CalibreComponent simpleTree() {
        CalibreComponent copy = copy();
        copy.tree = ComponentTree.of(copy);
        return copy;
    }

    @Override public CalibreComponent clone() { try { return (CalibreComponent) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override
    public CalibreComponent copy() {
        CalibreComponent copy = clone();
        copy.slots = copySlots();
        copy.systems = CalibreSystem.copyMap(systems);
        copy.services = new HashMap<>(services);
        copy.stats = stats.copy();
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalibreComponent that = (CalibreComponent) o;
        return completeRoot == that.completeRoot &&
                id.equals(that.id) &&
                categories.equals(that.categories) &&
                slots.equals(that.slots) &&
                systems.equals(that.systems) &&
                stats.equals(that.stats) &&
                Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categories, completeRoot, slots, systems, stats, item);
    }

    @Override
    public String toString() {
        return plugin.getGson().toJson(ComponentDescriptor.of(this));
    }
}
