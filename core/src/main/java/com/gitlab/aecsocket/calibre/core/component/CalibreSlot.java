package com.gitlab.aecsocket.calibre.core.component;

import com.gitlab.aecsocket.calibre.core.rule.Rule;
import com.gitlab.aecsocket.calibre.core.rule.visitor.SlotSetterVisitor;
import com.gitlab.aecsocket.unifiedframework.core.component.Component;
import com.gitlab.aecsocket.unifiedframework.core.component.IncompatibleComponentException;
import com.gitlab.aecsocket.unifiedframework.core.component.Slot;
import com.gitlab.aecsocket.unifiedframework.core.component.SlotRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

/**
 * A part of a component tree which holds child {@link CalibreComponent}s.
 */
@ConfigSerializable
public class CalibreSlot implements Slot {
    /** The held component. */
    protected transient CalibreComponent<?> component;
    /** The parent component this is held inside. */
    protected transient SlotRef<?> parent;
    /** Settings on what components are compatible with this slot. */
    protected Rule compatibility;
    /** If this slot is required to be filled to complete the component tree. */
    protected boolean required;
    /** If this slot can be modified in the field. This is implementation detail. */
    protected boolean fieldModifiable;
    /** The integer type of this slot, used to switch between different slots for different functions at runtime. */
    protected int type;
    /** List of tags that this slot holds. This is implementation detail, but is mainly used for determining
     * for what a slot can be used for. */
    protected final List<String> tags;

    public CalibreSlot(boolean required, boolean fieldModifiable) {
        this.required = required;
        this.fieldModifiable = fieldModifiable;
        tags = new ArrayList<>();
    }

    public CalibreSlot(boolean required) {
        this.required = required;
        tags = new ArrayList<>();
    }

    public CalibreSlot() {
        tags = new ArrayList<>();
    }

    public CalibreSlot(CalibreSlot o) {
        component = o.component == null ? null : o.component.copy();
        parent = o.parent;
        compatibility = o.compatibility;
        required = o.required;
        fieldModifiable = o.fieldModifiable;
        tags = new ArrayList<>(o.tags);
    }

    public Rule compatibility() { return compatibility; }
    public CalibreSlot compatibility(Rule compatibility) { this.compatibility = compatibility; return this; }

    public boolean required() { return required; }
    public CalibreSlot required(boolean required) { this.required = required; return this; }

    public boolean fieldModifiable() { return fieldModifiable; }
    public CalibreSlot fieldModifiable(boolean fieldModifiable) { this.fieldModifiable = fieldModifiable; return this; }

    public int type() { return type; }
    public CalibreSlot type(int type) { this.type = type; return this; }

    public List<String> tags() { return tags; }

    @Override @SuppressWarnings("unchecked") public <C extends Component> SlotRef<C> parent() { return (SlotRef<C>) parent; }
    @Override
    public CalibreSlot parent(Component component, String slotKey) {
        parent = (component == null && slotKey == null) ? null : new SlotRef<>(component, slotKey);
        return this;
    }

    @Override @SuppressWarnings("unchecked") public <C extends Component> C get() { return (C) component; }

    @Override
    public CalibreSlot set(@Nullable Component component) {
        if (component != null && !isCompatible(component))
            throw new IncompatibleComponentException(this, component);

        if (this.component != null)
            this.component.parent(null);
        this.component = (CalibreComponent<?>) component;
        if (component != null)
            component.parent(this);
        return this;
    }

    @Override
    public boolean isCompatible(@NotNull Component component) {
        if (!(component instanceof CalibreComponent))
            return false;
        if (compatibility == null)
            return true;
        CalibreComponent<?> child = (CalibreComponent<?>) component;
        compatibility.visit(new SlotSetterVisitor(child, (CalibreComponent<?>) parent.component()));
        return compatibility.applies(child);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalibreSlot that = (CalibreSlot) o;
        return required == that.required && fieldModifiable == that.fieldModifiable && Objects.equals(component, that.component) && compatibility.equals(that.compatibility) && tags.equals(that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, parent, compatibility, required, fieldModifiable, tags);
    }

    @Override public String toString() { return "<" + component + ">"; }

    /**
     * Deep copies this slot, including deep cloning its held component.
     * @return The copy.
     */
    public CalibreSlot copy() { return new CalibreSlot(this); }

    public static LinkedHashMap<String, CalibreSlot> copySlots(Map<String, CalibreSlot> existing) {
        LinkedHashMap<String, CalibreSlot> result = new LinkedHashMap<>();
        for (var entry : existing.entrySet())
            result.put(entry.getKey(), entry.getValue().copy());
        return result;
    }
}
