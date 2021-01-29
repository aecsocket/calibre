package me.aecsocket.calibre.component;

import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.ItemCreationException;
import me.aecsocket.calibre.util.ItemSupplier;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.Slot;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.stat.Stat;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@ConfigSerializable
public abstract class CalibreComponent<I extends Item> implements Component, CalibreIdentifiable, ItemSupplier<I> {
    @ConfigSerializable
    protected static class Dependencies {
        protected Map<String, ConfigurationNode> systems;
        protected List<String> statSystems;
        protected ConfigurationNode stats;
        protected ConfigurationNode completeStats;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;
    protected String id;
    protected boolean canComplete;
    protected final List<String> categories;
    protected final Map<String, CalibreSlot> slots;
    protected transient final Map<String, CalibreSystem> systems;
    protected transient final StatCollection stats;
    protected transient final StatCollection completeStats;

    protected transient ComponentTree tree;
    protected transient Slot parent;

    public CalibreComponent(String id) {
        this.id = id;
        categories = new ArrayList<>();
        slots = new LinkedHashMap<>();
        systems = new HashMap<>();
        stats = new StatCollection();
        completeStats = new StatCollection();
    }

    public CalibreComponent(CalibreComponent<I> o) throws SerializationException {
        id = o.id;
        canComplete = o.canComplete;
        categories = new ArrayList<>(o.categories);
        slots = CalibreSlot.copySlots(o.slots);
        systems = CalibreSystem.copySystems(o.systems);
        stats = new StatCollection(o.stats);
        completeStats = new StatCollection(o.completeStats);

        tree = o.tree;
        parent = o.parent;
    }

    @Override public String id() { return id; }
    @Override public void id(String id) { this.id = id; }

    @Override public Collection<String> dependencies() {
        List<String> result = new ArrayList<>(dependencies.systems.keySet());
        result.addAll(dependencies.statSystems);
        return result;
    }

    public boolean canComplete() { return canComplete; }
    public void canComplete(boolean canComplete) { this.canComplete = canComplete; }

    public List<String> categories() { return categories; }

    @Override @SuppressWarnings("unchecked") public @NotNull <S extends Slot> Map<String, S> slots() { return (Map<String, S>) slots; }

    @Override @SuppressWarnings("unchecked") public <S extends Slot> S slot(String key) { return (S) slots.get(key); }
    public CalibreComponent<I> slot(String key, CalibreSlot slot) {
        CalibreSlot old = slots.put(key, slot.parent(this, key));
        if (old != null)
            old.parent(null, null);
        return this;
    }

    public Map<String, CalibreSystem> systems() { return new HashMap<>(systems); }

    public <T extends CalibreSystem> T system(String id) {
        @SuppressWarnings("unchecked")
        T system = (T) systems.get(id);
        return system;
    }
    public <T> T system(Class<T> type) {
        for (CalibreSystem system : systems.values()) {
            if (type.isInstance(system)) {
                @SuppressWarnings("unchecked")
                T t = (T) system;
                return t;
            }
        }
        return null;
    }
    public CalibreComponent<I> system(CalibreSystem system) { systems.put(system.id(), system); return this; }

    public StatCollection stats() { return stats; }

    public StatCollection completeStats() { return completeStats; }

    public StatCollection buildStats() {
        StatCollection result = new StatCollection();

        result.combine(stats);
        if (tree.complete())
            result.combine(completeStats);
        systems.values().forEach(system -> result.combine(system.buildStats()));
        return result;
    }

    public ComponentTree tree() { return tree; }
    public CalibreComponent<I> tree(ComponentTree tree) { this.tree = tree; return this; }
    public CalibreComponent<I> buildTree() {
        tree = new ComponentTree(this).build();
        return this;
    }

    @Override @SuppressWarnings("unchecked") public <S extends Slot> S parent() { return (S) parent; }
    @Override public CalibreComponent<I> parent(Slot parent) { this.parent = parent; return this; }

    public abstract Map<String, Stat<?>> defaultStats();
    protected abstract void prepareStatDeserialization(Map<String, Stat<?>> originals);

    protected void deserializeStats(ConfigurationNode node, String fieldName, Consumer<StatCollection> consumer) {
        if (node == null)
            return;
        try {
            StatCollection deserialized = node.get(StatCollection.class);
            if (deserialized != null)
                consumer.accept(deserialized);
        } catch (SerializationException e) {
            throw new ResolutionException("Could not set up " + fieldName, e);
        }
    }

    private void addToDefaults(Map<String, Stat<?>> toAdd, Map<String, Stat<?>> defaultStats) {
        for (Map.Entry<String, Stat<?>> statEntry : toAdd.entrySet()) {
            String key = statEntry.getKey();
            if (defaultStats.containsKey(key))
                throw new ResolutionException(String.format("Duplicate stat %s", key));
            defaultStats.put(key, statEntry.getValue());
        }
    }

    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        Map<String, Stat<?>> defaultStats = new LinkedHashMap<>(defaultStats());
        systems.clear();
        if (dependencies.systems != null) {
            for (Map.Entry<String, ConfigurationNode> entry : dependencies.systems.entrySet()) {
                String sysId = entry.getKey();
                ConfigurationNode options = entry.getValue();

                CalibreSystem registeredSystem = context.getResolve(sysId, CalibreSystem.class);
                CalibreSystem system;
                try {
                    system = options.get(registeredSystem.getClass());
                } catch (SerializationException e) {
                    throw new ResolutionException(String.format("System %s could not be created", sysId), e);
                }
                if (system == null)
                    throw new ResolutionException(String.format("System %s was not created for whatever reason (is it @ConfigSerializable?)", sysId));

                system.inherit(registeredSystem, true);
                systems.put(sysId, system);

                Map<String, Stat<?>> sysDefaultStats = system.defaultStats();
                if (sysDefaultStats == null)
                    throw new ResolutionException(String.format("System %s returns no default stats: must at least return empty map", sysId));
                addToDefaults(sysDefaultStats, defaultStats);
            }
        }

        if (dependencies.statSystems != null) {
            for (String sysId : dependencies.statSystems) {
                CalibreSystem system = context.getResolve(sysId, CalibreSystem.class);
                Map<String, Stat<?>> sysDefaultStats = system.defaultStats();
                if (sysDefaultStats == null)
                    throw new ResolutionException(String.format("System %s for stats returns no default stats: must at least return empty map", sysId));
                addToDefaults(sysDefaultStats, defaultStats);
            }
        }

        stats.clear();
        prepareStatDeserialization(defaultStats);
        // Initialize systems AFTER creation, and default stats created
        // This lets systems deserialize their own stats
        for (CalibreSystem system : systems.values()) {
            try {
                system.setup(this);
            } catch (SystemSetupException e) {
                throw new ResolutionException(String.format("System %s could not be set up", system.id()), e);
            }
        }

        deserializeStats(dependencies.stats, "stats", stats::putAll);
        deserializeStats(dependencies.completeStats, "completeStats", completeStats::putAll);

        dependencies = null;
        buildTree();
    }

    @Override
    public net.kyori.adventure.text.Component name(String locale) {
        net.kyori.adventure.text.Component result = ItemSupplier.super.name(locale);
        return tree.call(new Events.NameCreate<>(this, locale, result)).result;
    }

    public abstract CalibreComponent<I> getComponent(I item);

    protected abstract I createInitial(int amount) throws ItemCreationException;

    @Override
    public I create(String locale, int amount) throws ItemCreationException {
        I result = createInitial(amount);
        result.saveTree(tree);
        result.name(name(locale));
        tree.call(new Events.ItemCreate<>(this, locale, result));
        return result;
    }

    public CalibreSlot combine(CalibreComponent<I> toAdd, boolean limited) {
        AtomicReference<CalibreSlot> candidate = new AtomicReference<>();
        walk(data -> {
            Slot raw = data.slot();
            if (!(raw instanceof CalibreSlot))
                return;
            CalibreSlot slot = (CalibreSlot) raw;
            if (
                    slot.get() == null
                    && slot.isCompatible(toAdd)
                    && (!limited || slot.fieldModifiable())
            ) {
                candidate.set(slot);
                data.stop();
            }
        });
        if (candidate.get() == null)
            return null;
        return candidate.get().set(toAdd);
    }

    public List<CalibreSlot> collectSlots(String tag) {
        List<CalibreSlot> result = new ArrayList<>();
        walk(data -> {
            CalibreSlot slot = data.slot();
            if (!slot.tags.contains(tag))
                return;
            result.add(slot);
        });
        return result;
    }

    public <S extends CalibreSystem> List<S> fromSlots(List<CalibreSlot> slots, Class<S> type) {
        List<S> result = new ArrayList<>();
        slots.forEach(slot -> slot.<CalibreComponent<?>>getOpt().ifPresent(component -> {
            S system = component.system(type);
            if (system != null)
                result.add(system);
        }));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalibreComponent<?> that = (CalibreComponent<?>) o;
        return canComplete == that.canComplete && id.equals(that.id) && categories.equals(that.categories) && slots.equals(that.slots) && systems.equals(that.systems) && stats.equals(that.stats) && completeStats.equals(that.completeStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, canComplete, categories, slots, systems, stats, completeStats, tree, parent);
    }

    @Override public String toString() { return id + slots.toString(); }

    public abstract CalibreComponent<I> copy() throws SerializationException;

    public static final class Events {
        private Events() {}

        public static class NameCreate<I extends Item> {
            private final CalibreComponent<I> component;
            private final String locale;
            private net.kyori.adventure.text.Component result;

            public NameCreate(CalibreComponent<I> component, String locale, net.kyori.adventure.text.Component result) {
                this.component = component;
                this.locale = locale;
                this.result = result;
            }

            public CalibreComponent<I> component() { return component; }
            public String locale() { return locale; }

            public net.kyori.adventure.text.Component result() { return result; }
            public void result(net.kyori.adventure.text.Component result) { this.result = result; }
        }

        public static class ItemCreate<I extends Item> {
            private final CalibreComponent<I> component;
            private final String locale;
            private final I item;

            public ItemCreate(CalibreComponent<I> component, String locale, I item) {
                this.component = component;
                this.locale = locale;
                this.item = item;
            }

            public String locale() { return locale; }
            public I item() { return item; }
            public CalibreComponent<I> component() { return component; }
        }
    }
}
