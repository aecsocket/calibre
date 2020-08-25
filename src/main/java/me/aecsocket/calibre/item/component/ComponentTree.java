package me.aecsocket.calibre.item.component;

import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.StatMap;

/**
 * Represents the context of a componen
 */
public class ComponentTree {
    private EventDispatcher eventDispatcher;
    private CalibreComponent root;
    private StatMap stats;

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

    public void rebuildTree() {
        rebuildTree(root);
    }

    private void rebuildTree(CalibreComponent parent) {
        parent.getSlots().forEach((name, slot) -> {
            CalibreComponent child = slot.get();
            if (child == null) return;
            child.setParent(parent);
            rebuildTree(child);
        });
    }
}
