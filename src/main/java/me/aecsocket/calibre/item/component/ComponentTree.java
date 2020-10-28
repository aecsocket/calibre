package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.util.OrderedStatMap;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.StatInstance;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;

public class ComponentTree {
    /**
     * A GSON type adapter for this class.
     */
    public static class Adapter implements JsonSerializer<ComponentTree>, JsonDeserializer<ComponentTree>, JsonAdapter {
        private final CalibrePlugin plugin;

        public Adapter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        @Override
        public JsonElement serialize(ComponentTree tree, Type type, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            CalibreComponent root = tree.root;

            object.add("id", new JsonPrimitive(root.getId()));

            root.getSystems().forEach((id, sys) -> {
                JsonElement serialized = context.serialize(sys);
                if (serialized != null && (!serialized.isJsonObject() || serialized.getAsJsonObject().size() > 0)) {
                    if (!object.has("systems"))
                        object.add("systems", new JsonObject());
                    object.get("systems").getAsJsonObject().add(sys.getId(), serialized);
                }
            });

            root.getSlots().forEach((name, slot) -> {
                if (slot.get() == null) return;
                if (!object.has("slots"))
                    object.add("slots", new JsonObject());
                object.get("slots").getAsJsonObject().add(name, context.serialize(slot.get().withSimpleTree().getTree()));
            });

            return object.size() == 1 ? new JsonPrimitive(root.getId()) : object;
        }

        @Override
        public ComponentTree deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            String id;
            JsonObject object = null;

            if (json.isJsonPrimitive())
                id = json.getAsString();
            else {
                object = assertObject(json);
                id = get(object, "id").getAsString();
            }

            CalibreComponent root = plugin.fromRegistry(id, CalibreComponent.class);
            if (root == null)
                throw new JsonParseException("Could not find component with ID " + id);
            root = root.copy();

            ComponentTree tree = new ComponentTree(root);
            root.setTree(tree);

            if (!json.isJsonPrimitive()) {
                boolean preserveInvalidData = plugin.setting("preserve_invalid_data", boolean.class, true);

                // Systems
                if (object.has("systems")) {
                    for (Map.Entry<String, JsonElement> entry : assertObject(get(object, "systems")).entrySet()) {
                        String sId = entry.getKey();
                        CalibreSystem sys = root.getSystem(sId);
                        if (sys == null) {
                            if (preserveInvalidData)
                                throw new JsonParseException(TextUtils.format("Could not find system on {root} with ID {sys}",
                                        "root", root.getId(), "sys", sId));
                            else
                                continue;
                        }
                        // We set and null instantly so that future errors are not caused from wrong parent comps being
                        // used for system fields.
                        plugin.getSystemAdapter().setParent(root);
                        root.getSystems().put(sId, plugin.getGson().fromJson(entry.getValue(), sys.getClass()));
                        plugin.getSystemAdapter().setParent(null);
                    }
                }
                // Slots
                if (object.has("slots")) {
                    for (Map.Entry<String, JsonElement> entry : assertObject(get(object, "slots")).entrySet()) {
                        String name = entry.getKey();
                        CalibreComponentSlot slot = root.getSlots().get(name);
                        if (slot == null) {
                            if (preserveInvalidData)
                                throw new JsonParseException(TextUtils.format("Could not find slot on {root} with name {slot}",
                                        "root", root.getId(), "slot", name));
                            else
                                continue;
                        }
                        ComponentTree subtree = context.deserialize(entry.getValue(), ComponentTree.class);
                        if (subtree == null) {
                            if (preserveInvalidData)
                                throw new JsonParseException(TextUtils.format("Could not create component on {root} for slot {slot}",
                                        "root", root.getId(), "slot", name));
                            else
                                continue;
                        }
                        slot.set(subtree.root);
                    }
                }
            }

            try {
                tree.build();
            } catch (SystemInitializationException e) {
                throw new JsonParseException(e);
            }
            return tree;
        }
    }

    private final CalibreComponent root;
    private final EventDispatcher eventDispatcher;
    private StatMap stats;
    private boolean complete;
    private OrderedStatMap extraStats;

    public ComponentTree(CalibreComponent root, EventDispatcher eventDispatcher) {
        this.root = root;
        this.eventDispatcher = eventDispatcher;
    }

    public ComponentTree(CalibreComponent root) {
        this(root, new EventDispatcher());
    }

    public CalibreComponent getRoot() { return root; }
    public EventDispatcher getEventDispatcher() { return eventDispatcher; }

    /**
     * Gets the combined stats of this tree, or creates them if they do not exist yet.
     * @return The stats of this tree.
     */
    public StatMap getStats() {
        if (stats != null)
            return stats;
        return buildStats();
    }
    public void setStats(StatMap stats) { this.stats = stats; }
    public boolean hasStats() { return stats != null; }

    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }

    public OrderedStatMap getExtraStats() { return extraStats; }
    public void setExtraStats(OrderedStatMap extraStats) { this.extraStats = extraStats; }
    public void combineStat(int order, StatMap map) { extraStats.combine(order, map); }
    public <T> void combineStat(int order, String key, StatInstance<T> value) {
        extraStats.computeIfAbsent(order, __ -> new StatMap()).modify(key, value);
    }
    public <T> void combineStat(int order, String key, Function<T, T> function) {
        extraStats.computeIfAbsent(order, __ -> new StatMap()).modify(key, function);
    }

    public void build() throws SystemInitializationException {
        complete = true;
        build(root);
    }

    private void build(CalibreComponent parent) throws SystemInitializationException {
        parent.setTree(this);
        for (CalibreComponentSlot slot : parent.getSlots().values()) {
            CalibreComponent child = slot.get();
            if (child != null) {
                child.setParent(parent);
                build(child);
            } else if (slot.isRequired())
                complete = false;
        }
        parent.getSystems().forEach((id, sys) -> {
            try {
                sys.initialize(parent, this);
            } catch (SystemInitializationException e) {
                throw new SystemInitializationException(TextUtils.format(
                        "Could not initialize system {system} for {id}: {msg}",
                        "system", id,
                        "id", parent.getId(),
                        "msg", e.getMessage()
                ), e);
            }
        });
    }

    private void addStat(Function<CalibreComponent, OrderedStatMap> statGetter, CalibreComponent component, OrderedStatMap stats) {
        stats.combine(statGetter.apply(component));
    }

    private OrderedStatMap buildStats(Function<CalibreComponent, OrderedStatMap> statGetter) {
        OrderedStatMap stats = new OrderedStatMap();

        addStat(statGetter, root, stats);
        root.walk(data -> data.getComponent().ifPresent(o -> {
            if (o instanceof CalibreComponent)
                addStat(statGetter, (CalibreComponent) o, stats);
        }));

        return stats;
    }

    public StatMap buildStats() {
        OrderedStatMap stats = buildStats(CalibreComponent::getStats);
        if (complete)
            stats.combine(buildStats(CalibreComponent::getCompleteStats));
        if (extraStats != null)
            stats.combine(extraStats);

        this.stats = stats.flatten();
        return this.stats;
    }

    //region Utils

    public <T> T stat(String key) { return getStats().getValue(key); }
    public <T> T callEvent(T event) { eventDispatcher.call(event); return event; }

    //endregion

    public static ComponentTree createAndBuild(CalibreComponent root) throws SystemInitializationException {
        ComponentTree result = new ComponentTree(root);
        result.build();
        return result;
    }
}
