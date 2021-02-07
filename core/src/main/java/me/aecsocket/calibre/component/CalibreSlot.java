package me.aecsocket.calibre.component;

import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import me.aecsocket.unifiedframework.component.Slot;
import me.aecsocket.unifiedframework.component.SlotRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

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
    @Setting(nodeFromParent = true)
    protected ComponentCompatibility compatibility = new ComponentCompatibility();
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

    public boolean required() { return required; }
    public void required(boolean required) { this.required = required; }

    public ComponentCompatibility compatibility() { return compatibility; }
    public void compatibility(ComponentCompatibility compatibility) { this.compatibility = compatibility; }

    public boolean fieldModifiable() { return fieldModifiable; }
    public void fieldModifiable(boolean fieldModifiable) { this.fieldModifiable = fieldModifiable; }

    public int type() { return type; }
    public void type(int type) { this.type = type; }

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
        return component instanceof CalibreComponent && compatibility.applies((CalibreComponent<?>) component);
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
