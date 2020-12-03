package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.util.OrderedStatMap;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.serialization.json.JsonAdapter;
import me.aecsocket.unifiedframework.stat.StatInstance;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.util.TextUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                        ComponentSlot<?> slot = root.getSlot(name);
                        if (!(slot instanceof CalibreComponentSlot)) {
                            if (preserveInvalidData)
                                throw new JsonParseException(TextUtils.format("Could not find valid slot on {root} with name {slot}",
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
                        ((CalibreComponentSlot) slot).set(subtree.root);
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
    private StatMap extraStats;

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

    public StatMap getExtraStats() { return extraStats; }
    public void setExtraStats(StatMap extraStats) { this.extraStats = extraStats; }
    public <T> void combineStat(String key, StatInstance<T> value) { extraStats.modify(key, value); }
    public <T> void combineStat(String key, Function<T, T> function) { extraStats.modify(key, function); }

    public void build() throws SystemInitializationException {
        complete = root.canComplete();
        build(root);
    }

    private void build(CalibreComponent parent) throws SystemInitializationException {
        parent.setTree(this);
        for (Map.Entry<String, CalibreComponentSlot> entry : parent.getSlots().entrySet()) {
            CalibreComponentSlot slot = entry.getValue();
            CalibreComponent child = slot.get();
            if (child != null) {
                child.setParent(parent);
                child.setParentSlotName(entry.getKey());
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

    /*
    consider:
    root (-1:{item:IRON_HOE},0:{damage:3})
    |_ magazine (-2:{damage:+1},-1:{item:IRON_INGOT},0:{damage:+5})
    order of stat combining, for any call of buildStats:
      1. root:0
      2. magazine:0
      3. magazine:-2
      4. magazine:-1
      5. root:-1
    orders >=0 are called first, being root-first
    orders <0 are called last, being depth-first (reversed ComponentHolder#walk order)

    Full process:
    1) build component stats }
    2) build system stats    } iterate over components
    3) build complete stats  }
    4) combine w/ extra stats

    for ONE call of buildStats:
    1) gather all OrderedStatMaps individually in the tree IN ORDER
    2) for >=0 order maps, combine them in order
    3) for <0 order maps, combine them in reverse order
     */

    private StatMap buildStats(Function<CalibreComponent, OrderedStatMap> statGetter) {
        // step 1
        List<OrderedStatMap> orderedMaps = new ArrayList<>();
        orderedMaps.add(statGetter.apply(root));
        root.walk(data -> data.getComponent().ifPresent(raw -> {
            if (raw instanceof CalibreComponent)
                orderedMaps.add(statGetter.apply((CalibreComponent) raw));
        }));

        StatMap stats = new StatMap();
        // step 2
        OrderedStatMap resultMap = new OrderedStatMap();
        orderedMaps.forEach(resultMap::combine);
        resultMap.forEach((order, map) -> {
            if (order >= 0)
                stats.modifyAll(map);
        });

        // step 3
        Collections.reverse(orderedMaps);
        resultMap = new OrderedStatMap();
        orderedMaps.forEach(resultMap::combine);
        resultMap.forEach((order, map) -> {
            if (order < 0)
                stats.modifyAll(map);
        });

        return stats;
    }

    public StatMap buildStats() {
        stats = buildStats(comp -> {
            OrderedStatMap map = comp.getStats().copy();
            comp.getSystems().values().forEach(sys -> {
                OrderedStatMap sysStats = sys.buildStats();
                if (sysStats != null)
                    map.combine(sysStats);
            });
            return map;
        });
        if (complete)
            stats.modifyAll(buildStats(CalibreComponent::getCompleteStats));
        if (extraStats != null)
            stats.modifyAll(extraStats);

        return stats;
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
