package me.aecsocket.calibre.item.component;

import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.StatInstance;
import me.aecsocket.unifiedframework.stat.StatMap;

/**
 * Represents the context of a component in a tree with other components. One instance should be shared
 * across all components in the tree.
 */
public class ComponentTree {
    private EventDispatcher eventDispatcher;
    private CalibreComponent root;
    private StatMap stats;
    private boolean complete;

    public ComponentTree(EventDispatcher eventDispatcher, CalibreComponent root, StatMap stats) {
        this.eventDispatcher = eventDispatcher;
        this.root = root;
        this.stats = stats;
    }

    public ComponentTree(CalibreComponent root) {
        this.eventDispatcher = new EventDispatcher();
        this.root = root;
        this.stats = new StatMap();
    }

    public ComponentTree() {
        this.eventDispatcher = new EventDispatcher();
        this.stats = new StatMap();
    }

    public EventDispatcher getEventDispatcher() { return eventDispatcher; }
    public void setEventDispatcher(EventDispatcher eventDispatcher) { this.eventDispatcher = eventDispatcher; }

    public CalibreComponent getRoot() { return root; }
    public void setRoot(CalibreComponent root) { this.root = root; }

    public StatMap getStats() { return stats; }
    public void setStats(StatMap stats) { this.stats = stats; }

    /**
     * Gets a stat value from this instance's StatMap.
     * @param key The key of the stat.
     * @param <T> The stat's value's type.
     * @return The stat value.
     */
    public <T> T stat(String key) { return stats.getValue(key); }

    /**
     * Gets if all required component slots have a component in them,
     * or if there are no required slots, returns true.
     * @return The result.
     */
    public boolean isComplete() { return complete; }

    /**
     * Recursively initializes components and systems in the tree.
     * Also updates {@link ComponentTree#complete}.
     */
    public void rebuild() {
        eventDispatcher.unregisterAll();
        stats = new StatMap();
        complete = true;
        rebuild(root);
    }

    private void rebuild(CalibreComponent parent) {
        parent.setTree(this);
        parent.modifyTree(this);

        for (CalibreSystem<?> system : parent.getSystems().values()) {
            system.acceptParent(parent);
            system.acceptTree(this);
        }

        parent.getSlots().forEach((name, slot) -> {
            CalibreComponent child = slot.get();
            if (child == null) {
                if (slot.isRequired()) complete = false;
                return;
            };
            child.setParent(parent);
            rebuild(child);
        });
    }

    @Override
    public String toString() {
        return "ComponentTree{" +
                "root=" + root +
                ", complete=" + complete +
                '}';
    }

    /**
     * Creates a simple component tree for a premade component. Is automatically built.
     * @param root The premade root component.
     * @return The tree.
     */
    public static ComponentTree of(CalibreComponent root) {
        ComponentTree tree = new ComponentTree(root);
        tree.rebuild();
        return tree;
    }
}
