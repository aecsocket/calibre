package me.aecsocket.calibre.item.component;

import me.aecsocket.unifiedframework.event.EventDispatcher;
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
     * Gets if all required component slots have a component in them,
     * or if there are no required slots, returns true.
     * @return The result.
     */
    public boolean isComplete() { return complete; }

    /**
     * Recursively sets all components' parents to their proper value, to rebuild the tree.
     * Also updates {@link ComponentTree#complete}.
     */
    public void rebuild() { rebuild(root); }

    private void rebuild(CalibreComponent parent) {
        complete = true;
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
}
