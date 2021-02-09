package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.unifiedframework.event.EventDispatcher;

import java.util.Objects;

public abstract class NameFromChildSystem extends AbstractSystem {
    public static final String ID = "name_from_child";
    public static final int LISTENER_PRIORITY = 100000;
    @FromMaster private String child;

    /**
     * Used for registration + deserialization.
     */
    public NameFromChildSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public NameFromChildSystem(NameFromChildSystem o) {
        super(o);
        child = o.child;
    }

    @Override public String id() { return ID; }

    public String child() { return child; }
    public void child(String child) { this.child = child; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        if (child == null)
            throw new SystemSetupException("No child specified");
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.NameCreate.class, this::onEvent, listenerPriority);
    }

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        if (parent != event.component()) return;
        CalibreSlot slot = parent.slot(child);
        if (slot == null)
            return;
        CalibreComponent<?> child = slot.get();
        if (child != null)
            event.result(child.name(event.locale()));
    }

    @Override public abstract NameFromChildSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameFromChildSystem that = (NameFromChildSystem) o;
        return child.equals(that.child);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child);
    }
}
