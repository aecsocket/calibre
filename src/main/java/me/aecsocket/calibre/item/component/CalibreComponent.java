package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A component, which can be nested to create component trees. The building block of an item.
 */
public class CalibreComponent implements CalibreItem, Component, ComponentHolder<CalibreComponentSlot>, AcceptsCalibrePlugin {
    public static final String ITEM_TYPE = "component";
    // TODO figure out something better than this
    private static final StatMapAdapter statMapAdapter = new StatMapAdapter();
    private static final Gson statMapGson = new GsonBuilder()
            .registerTypeAdapter(StatMap.class, statMapAdapter)
            .create();

    private transient CalibrePlugin plugin;
    private String id;
    private List<String> categories = new ArrayList<>();
    private Map<String, CalibreComponentSlot> slots = new HashMap<>();
    private transient Map<Class<? extends CalibreSystem>, CalibreSystem> systems = new HashMap<>();
    private transient StatMap stats = new StatMap();
    private ItemDescriptor item;

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

    @Override public Map<String, CalibreComponentSlot> getSlots() { return slots; }
    public void setSlots(Map<String, CalibreComponentSlot> slots) { this.slots = slots; }

    public Map<Class<? extends CalibreSystem>, CalibreSystem> getSystems() { return systems; }
    public void setSystems(Map<Class<? extends CalibreSystem>, CalibreSystem> systems) { this.systems = systems; }

    public StatMap getStats() { return stats; }
    public void setStats(StatMap stats) { this.stats = stats; }

    public ItemDescriptor getItem() { return item; }
    public void setItem(ItemDescriptor item) { this.item = item; }

    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        if (context.getJson() == null) return;
        JsonAdapter util = JsonAdapter.INSTANCE;
        JsonObject json = util.assertObject(context.getJson());

        // Load base
        if (json.has("base")) {
            String baseId = json.get("base").getAsString();
            CalibreComponent base = context.get(baseId, CalibreComponent.class);
            if (base == null) throw new ResolutionException("Invalid base component " + baseId);
            extendBase(base);
            load(context, json, context.getGson());
        }
    }

    /**
     * Loads systems and stats. For internal use.
     * @param context The {@link ResolutionContext}.
     * @param json The {@link JsonObject}.
     * @param gson The {@link Gson}.
     */
    public void load(ResolutionContext context, JsonObject json, Gson gson) {
        JsonAdapter util = JsonAdapter.INSTANCE;

        // Load systems
        // 1. Generate the systems
        JsonObject object = util.assertObject(util.get(json, "systems", new JsonObject()));
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String systemId = entry.getKey();
            CalibreSystem system = context.get(systemId, CalibreSystem.class);
            if (system == null) throw new ResolutionException(TextUtils.format("System {id} does not exist", "id", systemId));
            system = gson.fromJson(entry.getValue(), system.getClass());
            systems.put(system.getClass(), system);
        }
        // 2. Prepare the systems
        systems.forEach((type, system) -> {
            Collection<Class<? extends CalibreSystem>> conflicts = system.getConflicts();
            Collection<Class<? extends CalibreSystem>> dependencies = new ArrayList<>(system.getDependencies());
            systems.forEach((type2, system2) -> {
                if (conflicts.contains(type2))
                    throw new ResolutionException(
                            TextUtils.format("System {id} is not compatible with system type {type}",
                                    "id", system.getId(),
                                    "type", type2.getSimpleName()
                            )
                    );
                dependencies.remove(type2);
            });
            if (dependencies.size() > 0)
                throw new ResolutionException(
                        TextUtils.format("System {id} has not had all dependencies fulfilled: {deps}",
                                "id", system.getId(),
                                "deps", String.join(", ", dependencies.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")))
                        )
                );
            system.acceptSystems(systems);
        });

        // Load stats
        // 1. Combine all system stats into one stat map
        Map<String, Stat<?>> stats = new LinkedHashMap<>();
        systems.forEach((id, system) -> {
            Map<String, Stat<?>> toAdd = system.getDefaultStats();
            if (toAdd != null) stats.putAll(toAdd);
        });
        // 2. Load stats with that map
        statMapAdapter.setStats(stats);
        this.stats.addAll(statMapGson.fromJson(util.get(json, "stats", new JsonObject()), StatMap.class));
    }

    /**
     * Makes this component inherit the same:
     * <ul>
     *     <li>slots</li>
     *     <li>systems</li>
     *     <li>stats</li>
     * </ul>
     * as the provided base. Existing fields will be overwritten.
     * All collections involved are deep cloned. This may be an intensive operation.
     * @param base The base component.
     */
    public void extendBase(CalibreComponent base) {
        if (base == null) return;

        slots.putAll(base.copySlots());
        systems.putAll(base.systems.entrySet().stream().peek(entry -> entry.setValue(entry.getValue().copy())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        stats.addAll(base.stats.copy());
    }

    @Override public String getItemType() { return ITEM_TYPE; }

    @Override
    public ItemStack createItem(@Nullable Player player, int amount) throws ItemCreationException {
        if (item == null) throw new ItemCreationException("Item is null");
        ItemStack result = item.create();
        result.setAmount(amount);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(getLocalizedName(player));
        result.setItemMeta(meta);
        return result;
    }

    @Override public @Nullable String getShortInfo(CommandSender sender) { return getLocalizedName(sender); }

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
                                systems.entrySet().stream().map(entry -> {
                                    CalibreSystem system = entry.getValue();
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
                                "type", entry.getValue().getStat().getValueType(),
                                "value", entry.getValue().valueToString(),
                                "default", entry.getValue().getStat().getDefaultValue())).collect(Collectors.joining("\n"))
                        , prefix),
                "item", TextUtils.prefixLines(item == null ? "null" : item.getLongInfo(plugin, sender), prefix)
                );
    }

    @Override public CalibreComponent clone() { try { return (CalibreComponent) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override
    public CalibreComponent copy() {
        CalibreComponent copy = clone();
        copy.slots = copySlots();
        copy.systems = systems.entrySet().stream().peek(entry -> entry.setValue(entry.getValue().copy())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        copy.stats = stats.copy();
        return copy;
    }
}
