package me.aecsocket.calibre.component;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import me.aecsocket.unifiedframework.component.Slot;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.StatMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.*;

public class ComponentTree {
    public static abstract class AbstractSerializer implements TypeSerializer<ComponentTree> {
        protected abstract <T extends CalibreIdentifiable> T byId(String id, Class<T> type);
        protected abstract boolean preserveInvalidData();

        @Override
        public void serialize(Type type, @Nullable ComponentTree obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                CalibreComponent<?> root = obj.root;

                node.node("id").set(root.id());

                for (CalibreSystem system : root.systems().values()) {
                    ConfigurationNode settings = BasicConfigurationNode.root(node.options()).set(system);
                    if (settings.isMap() && settings.childrenMap().size() == 0)
                        // the settings are equivalent to `{}`, or an empty node
                        // these are the default settings anyway
                        continue;
                    node.node("systems", system.id()).set(settings);
                }

                Map<String, CalibreSlot> slots = root.slots;
                for (Map.Entry<String, CalibreSlot> entry : slots.entrySet()) {
                    String key = entry.getKey();
                    CalibreSlot slot = entry.getValue();
                    CalibreComponent<?> child = slot.get();
                    if (child == null)
                        continue;
                    node.node("slots", key).set(child.copy().buildTree().tree);
                }
            }
        }

        private <I extends Item> ComponentTree internalDeserialize(Type type, ConfigurationNode node, boolean preserveInvalidData) throws SerializationException {
            String id;
            Map<String, ConfigurationNode> systems = null;
            Map<String, ConfigurationNode> slots = null;
            if (node.isMap()) {
                id = node.node("id").getString();
                systems = node.node("systems").get(new TypeToken<>(){});
                slots = node.node("slots").get(new TypeToken<>(){});
            } else
                id = node.getString();

            @SuppressWarnings("unchecked")
            CalibreComponent<I> root = byId(id, CalibreComponent.class);
            if (root == null)
                throw new SerializationException(node, type, "No component with ID [" + id + "]");
            root = root.copy();

            if (systems != null) {
                for (Map.Entry<String, ConfigurationNode> entry : systems.entrySet()) {
                    String sysId = entry.getKey();
                    CalibreSystem parentSystem = root.system(sysId);
                    if (parentSystem == null) {
                        if (preserveInvalidData) throw new SerializationException(node, type, "System [" + sysId + "] is not present on this component");
                        else continue;
                    }

                    CalibreSystem system = entry.getValue().get(parentSystem.getClass());
                    if (system == null) {
                        if (preserveInvalidData) throw new SerializationException(node, type, "Could not create system [" + sysId + "]");
                        else continue;
                    }
                    system.inherit(parentSystem, false);
                    root.system(system);
                }
            }

            if (slots != null) {
                for (Map.Entry<String, ConfigurationNode> entry : slots.entrySet()) {
                    String key = entry.getKey();
                    if (root.slot(key) == null) {
                        if (preserveInvalidData) throw new SerializationException(node, type, "Slot [" + key + "] is not present on this component");
                        else continue;
                    }

                    ComponentTree childTree;
                    try {
                        childTree = internalDeserialize(type, entry.getValue(), preserveInvalidData);
                    } catch (SerializationException e) {
                        if (preserveInvalidData) throw new SerializationException(node, type, "Could not create component for slot [" + key + "]", e);
                        else continue;
                    }
                    if (childTree.root == null) {
                        if (preserveInvalidData) throw new SerializationException(node, type, "Did not create component for slot [" + key + "]");
                        else continue;
                    }

                    try {
                        ((CalibreSlot) (root.slot(key))).set(childTree.root);
                    } catch (IncompatibleComponentException e) {
                        if (preserveInvalidData)
                            throw new SerializationException(node, type, e);
                    }
                }
            }

            return new ComponentTree(root);
        }

        @Override
        public ComponentTree deserialize(Type type, ConfigurationNode node) throws SerializationException {
            boolean preserveInvalidData = preserveInvalidData();
            return internalDeserialize(type, node, preserveInvalidData).build();
        }
    }

    private final CalibreComponent<?> root;
    private final EventDispatcher events = new EventDispatcher();
    private final StatMap stats = new StatMap();
    private boolean complete = true;

    public ComponentTree(CalibreComponent<?> root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public <C extends CalibreComponent<?>> C root() { return (C) root; }
    public EventDispatcher events() { return events; }
    public StatMap stats() { return stats; }

    public boolean complete() { return complete; }
    public void complete(boolean complete) { this.complete = complete; }

    private <I extends Item> void build(Slot parent, CalibreComponent<I> component) {
        component.tree(this);
        component.parent(parent);
        component.systems().values().forEach(system -> system.parentTo(this, component));
        component.slots.forEach((key, slot) -> {
            slot.parent(component, key);
            CalibreComponent<I> child = slot.get();
            if (child == null) {
                if (slot.required())
                    complete = false;
            } else {
                build(slot, child);
            }
        });
    }

    public ComponentTree build() {
        buildTree();
        buildStats();
        return this;
    }

    public ComponentTree buildTree() {
        stats.clear();
        events.unregisterAll();
        complete = root.canComplete;
        build(null, root);
        return this;
    }

    public ComponentTree buildStats() {
        List<StatCollection> collectedStats = new ArrayList<>();
        root.<CalibreComponent<?>>forWalkAndThis((component, path) -> {
            if (component != null)
                collectedStats.add(component.buildStats());
        });

        Map<Integer, List<StatMap>> collected = new TreeMap<>(Comparator.comparingInt(i -> i));
        collectedStats.forEach(collection -> collection.forEach((order, map) ->
                collected.computeIfAbsent(order, __ -> new ArrayList<>()).add(map)));
        // grab the positive ordered stat maps, and combine them in order
        collected.forEach((priority, collection) -> collection.forEach(map -> {
            if (priority >= 0) {
                stats.modAll(map);
            }
        }));

        // grab the negative ordered stat maps, and combine them in reverse order
        Map<Integer, List<StatMap>> reverseCollected = new TreeMap<>((a, b) -> b - a);
        reverseCollected.putAll(collected);
        reverseCollected.forEach((priority, collection) -> {
            Collections.reverse(collection);
            collection.forEach(map -> {
                if (priority < 0) {
                    stats.modAll(map);
                }
            });
        });

        return this;
    }

    public <T> T stat(String key) { return stats.val(key); }
    public <E> E call(E event) { events.call(event); return event; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentTree that = (ComponentTree) o;
        return root.equals(that.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(root);
    }

    @Override public String toString() { return "tree<" + root + ">"; }

    public String serialize(ConfigurationOptions options, boolean prettyPrinting) throws ConfigurateException {
        StringWriter writer = new StringWriter();
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .sink(() -> new BufferedWriter(writer))
                .prettyPrinting(prettyPrinting)
                .build();
        loader.save(BasicConfigurationNode.root(options).set(this));
        return writer.toString();
    }

    public static ComponentTree deserialize(String json, ConfigurationOptions options) throws ConfigurateException {
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .source(() -> new BufferedReader(new StringReader(json)))
                .build();
        return loader.load(options).get(ComponentTree.class);
    }
}
