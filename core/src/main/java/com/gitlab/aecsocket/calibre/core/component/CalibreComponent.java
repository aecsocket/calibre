package com.gitlab.aecsocket.calibre.core.component;

import com.gitlab.aecsocket.calibre.core.rule.*;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.core.util.CalibreIdentifiable;
import com.gitlab.aecsocket.calibre.core.util.ItemCreationException;
import com.gitlab.aecsocket.calibre.core.util.ItemSupplier;
import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import com.gitlab.aecsocket.unifiedframework.core.component.Component;
import com.gitlab.aecsocket.unifiedframework.core.component.Slot;
import com.gitlab.aecsocket.unifiedframework.core.registry.ResolutionContext;
import com.gitlab.aecsocket.unifiedframework.core.registry.ResolutionException;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A component which can be placed in a {@link CalibreSlot} and take part in a {@link ComponentTree}.
 * @param <I> The item type.
 */
@ConfigSerializable
public abstract class CalibreComponent<I extends Item> implements Component, CalibreIdentifiable, ItemSupplier<I> {
    @ConfigSerializable
    protected static class Dependencies {
        protected Map<String, ConfigurationNode> systems;
        protected List<String> statSystems;
        protected ConfigurationNode stats;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;
    /** This object's ID. */
    protected String id;
    /** The categories this component falls under. */
    protected final List<String> categories;
    /** A map of this component's slots. */
    protected final Map<String, CalibreSlot> slots;
    /** A map of the systems this component stores, linked to their IDs. */
    protected transient final Map<String, CalibreSystem> systems;
    /** This component's stats that it provides during stat building. */
    protected transient RuledStatCollectionList stats;

    /** The tree this component is a part of. */
    protected transient ComponentTree tree;
    /** The parent slot of this component. */
    protected transient Slot parent;

    public CalibreComponent(String id) {
        this.id = id;
        categories = new ArrayList<>();
        slots = new LinkedHashMap<>();
        systems = new HashMap<>();
        stats = new RuledStatCollectionList();
    }

    public CalibreComponent(CalibreComponent<I> o) {
        id = o.id;
        categories = new ArrayList<>(o.categories);
        slots = CalibreSlot.copySlots(o.slots);
        systems = CalibreSystem.copySystems(o.systems);
        stats = o.stats.copy();

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

    public List<String> categories() { return categories; }

    @Override @SuppressWarnings("unchecked") public @NotNull <S extends Slot> Map<String, S> slots() { return (Map<String, S>) slots; }

    @Override @SuppressWarnings("unchecked") public <S extends Slot> S slot(String key) { return (S) slots.get(key); }
    public CalibreComponent<I> slot(String key, CalibreSlot slot) {
        CalibreSlot old = slots.put(key, slot.parent(this, key));
        if (old != null)
            old.parent(null, null);
        return this;
    }

    @SuppressWarnings("unchecked") public <S extends CalibreSystem> Map<String, S> systems() { return new HashMap<>((Map<String, S>) systems); }

    public <T extends CalibreSystem> T system(String id) {
        @SuppressWarnings("unchecked")
        T system = (T) systems.get(id);
        return system;
    }

    /**
     * Gets a system on this component by its type.
     * The type does not have to extend {@link CalibreSystem}, however only systems can be stored
     * on a component.
     * @param type The type.
     * @param <T> The type.
     * @return The system, or null.
     */
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

    /**
     * Associated a system with this component.
     * @param system The system.
     * @return This instance.
     */
    public CalibreComponent<I> system(CalibreSystem system) { systems.put(system.id(), system); return this; }

    public RuledStatCollectionList stats() { return stats; }
    public StatCollection getStats() { return stats.build(this); }

    /**
     * Builds this component's stats.
     * @return The stats.
     */
    public StatCollection buildStats() {
        StatCollection result = getStats();
        systems.values().forEach(sys -> sys.buildStats(result));
        return result;
    }

    public ComponentTree tree() { return tree; }
    public CalibreComponent<I> tree(ComponentTree tree) { this.tree = tree; return this; }

    /**
     * Builds a new tree for this component.
     * @return This instance.
     */
    public CalibreComponent<I> buildTree() {
        tree = new ComponentTree(this).build();
        return this;
    }

    @Override @SuppressWarnings("unchecked") public <S extends Slot> S parent() { return (S) parent; }
    @Override public CalibreComponent<I> parent(Slot parent) { this.parent = parent; return this; }

    /**
     * Provides the stat types that this component provides to configurations.
     * @return The stat types.
     */
    public abstract Map<String, Stat<?>> statTypes();

    /**
     * Provides the rule types that this component provides to configurations.
     * @return The rule types.
     */
    public Map<String, Class<? extends Rule>> ruleTypes() { return Rule.DEFAULT_RULE_TYPES; }

    /**
     * Prepares the stat deserializer used to deserialize the specified map of stat originals.
     * <p>
     * If using a {@link com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.StatMapSerializer}, the
     * {@link com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.StatMapSerializer#originals(Map)} method should
     * be used.
     * @param statTypes The originals, or stat types.
     * @param ruleTypes The rule types.
     */
    protected abstract void prepareStatDeserialization(Map<String, Stat<?>> statTypes, Map<String, Class<? extends Rule>> ruleTypes);

    protected <T> T deserialize(ConfigurationNode node, TypeToken<T> type, String fieldName) {
        if (node == null)
            return null;
        try {
            return node.get(type);
        } catch (SerializationException e) {
            throw new ResolutionException("Could not set up " + fieldName, e);
        }
    }

    /**
     * Adds the specified originals map to an existing originals map.
     * @param toAdd The originals to add.
     * @param statTypes The existing originals map.
     */
    private void addToDefaults(Map<String, Stat<?>> toAdd, Map<String, Stat<?>> statTypes) {
        for (var statEntry : toAdd.entrySet()) {
            String key = statEntry.getKey();
            statTypes.put(key, statEntry.getValue());
        }
    }

    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        Map<String, Stat<?>> statTypes = new LinkedHashMap<>(statTypes());
        Map<String, Class<? extends Rule>> ruleTypes = new HashMap<>(ruleTypes());
        systems.clear();
        if (dependencies.systems != null) {
            for (var entry : dependencies.systems.entrySet()) {
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

                Map<String, Stat<?>> sysStatTypes = system.statTypes();
                if (sysStatTypes == null)
                    throw new ResolutionException(String.format("System %s returns no stat types: must at least return empty map", sysId));
                addToDefaults(sysStatTypes, statTypes);

                Map<String, Class<? extends Rule>> sysRuleTypes = system.ruleTypes();
                if (sysRuleTypes == null)
                    throw new ResolutionException(String.format("System %s returns no rule types: must at least return empty map", sysId));
                ruleTypes.putAll(sysRuleTypes);
            }
        }

        if (dependencies.statSystems != null) {
            for (String sysId : dependencies.statSystems) {
                CalibreSystem system = context.getResolve(sysId, CalibreSystem.class);
                Map<String, Stat<?>> sysStatTypes = system.statTypes();
                if (sysStatTypes == null)
                    throw new ResolutionException(String.format("System %s for stats returns no default stats: must at least return empty map", sysId));
                addToDefaults(sysStatTypes, statTypes);
            }
        }

        prepareStatDeserialization(statTypes, ruleTypes);
        // Initialize systems AFTER creation, and default stats created
        // This lets systems deserialize their own stats
        for (CalibreSystem system : systems.values()) {
            try {
                system.setup(this);
            } catch (SystemSetupException e) {
                throw new ResolutionException(String.format("System %s could not be set up", system.id()), e);
            }
        }

        stats = deserialize(dependencies.stats, new TypeToken<>(){}, "stats");

        dependencies = null;
        buildTree();
    }

    @Override
    public net.kyori.adventure.text.Component name(Locale locale) {
        net.kyori.adventure.text.Component result = ItemSupplier.super.name(locale);
        return tree.call(new Events.NameCreate<>(this, locale, result)).result;
    }

    /**
     * Gets a component from an {@link I}.
     * @param item The {@link I}.
     * @return The component, or null if it is not a valid component.
     */
    public abstract CalibreComponent<I> getComponent(I item);

    /**
     * Creates the initial {@link I} for {@link #create(Locale, int)}.
     * @param amount The amount of items.
     * @return The initial {@link I}.
     * @throws ItemCreationException If the item could not be created.
     */
    protected abstract I createInitial(int amount) throws ItemCreationException;

    @Override
    public I create(Locale locale, int amount) throws ItemCreationException {
        I result;
        try {
            result = createInitial(amount);
        } catch (ItemCreationException e) {
            throw new ItemCreationException("Could not create initial item for component [" + id + "]", e);
        }
        result.saveTree(tree);
        result.name(name(locale));
        tree.call(new Events.ItemCreate<>(this, locale, result));
        return result;
    }

    /**
     * Combines another component with this tree, as long as it is possible.
     * <p>
     * This walks through slots and only places if:
     * <ul>
     *     <li>the slot is empty</li>
     *     <li>the component is compatible with the slot</li>
     *     <li>the slot is field modifiable OR the {@code limited} parameter is false</li>
     * </ul>
     * @param toAdd The component to add to this tree.
     * @param limited If modification should be limited to {@link CalibreSlot#fieldModifiable()} slots.
     * @return The slot that was modified, or null if the component could not be placed.
     */
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

    /**
     * Collects a list of slots which meet the criteria provided.
     * @param tag The tag that the slot requires. Can be null.
     * @param type The type that the slot requires. Can be null.
     * @return The slots.
     */
    public List<CalibreSlot> collectSlots(String tag, Integer type) {
        List<CalibreSlot> result = new ArrayList<>();
        walk(data -> {
            CalibreSlot slot = data.slot();
            if (tag != null && !slot.tags.contains(tag))
                return;
            if (type != null && slot.type != type)
                return;
            result.add(slot);
        });
        return result;
    }

    /**
     * Gets systems from the components inside a list of slots.
     * @param slots The slots.
     * @param type The system type.
     * @param <S> The system type.
     * @return The list of systems.
     */
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
        return id.equals(that.id) && categories.equals(that.categories) && slots.equals(that.slots) && systems.equals(that.systems) && stats.equals(that.stats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categories, slots, systems, stats, tree, parent);
    }

    @Override public String toString() { return id + slots.toString(); }

    /**
     * Deep copies this component, including all systems, slots, and stats.
     * @return The copy.
     */
    public abstract CalibreComponent<I> copy();

    /**
     * Component events.
     */
    public static final class Events {
        private Events() {}

        /**
         * Runs when the name for a component is created.
         * @param <I> The item type.
         */
        public static class NameCreate<I extends Item> {
            /** The component that is being affected. */
            private final CalibreComponent<I> component;
            /** The locale the name is being generated for. */
            private final Locale locale;
            /** The currently chosen name. */
            private net.kyori.adventure.text.Component result;

            public NameCreate(CalibreComponent<I> component, Locale locale, net.kyori.adventure.text.Component result) {
                this.component = component;
                this.locale = locale;
                this.result = result;
            }

            public CalibreComponent<I> component() { return component; }
            public Locale locale() { return locale; }

            public net.kyori.adventure.text.Component result() { return result; }
            public void result(net.kyori.adventure.text.Component result) { this.result = result; }
        }

        /**
         * Runs when an item is being created for a component.
         * @param <I> The item type.
         */
        public static class ItemCreate<I extends Item> {
            /** The component that is being affected. */
            private final CalibreComponent<I> component;
            /** The locale the name is being generated for. */
            private final Locale locale;
            /** The currently chosen item. */
            private final I item;

            public ItemCreate(CalibreComponent<I> component, Locale locale, I item) {
                this.component = component;
                this.locale = locale;
                this.item = item;
            }

            public Locale locale() { return locale; }
            public I item() { return item; }
            public CalibreComponent<I> component() { return component; }
        }
    }
}
