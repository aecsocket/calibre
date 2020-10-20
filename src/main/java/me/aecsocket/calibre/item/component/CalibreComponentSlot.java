package me.aecsocket.calibre.item.component;

import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import org.jetbrains.annotations.Nullable;

/**
 * Calibre's implementation of {@link ComponentSlot}.
 */
public class CalibreComponentSlot implements ComponentSlot<CalibreComponent>, Cloneable {
    private CalibreComponent component;
    private boolean required;

    public CalibreComponentSlot(CalibreComponent component, boolean required) {
        this.component = component;
        this.required = required;
    }

    public CalibreComponentSlot(CalibreComponent component) {
        this.component = component;
    }

    public CalibreComponentSlot() {}

    @Override public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    @Override public CalibreComponent get() { return component; }
    @Override
    public void set(CalibreComponent component) throws IncompatibleComponentException {
        if (!isCompatible(component)) throw new IncompatibleComponentException();
        this.component = component;
    }

    @Override
    public boolean isCompatible(@Nullable CalibreComponent component) {
        return true;
    }

    public CalibreComponentSlot clone() { try { return (CalibreComponentSlot) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    public CalibreComponentSlot copy() {
        CalibreComponentSlot copy = clone();
        if (component != null)
            copy.component = component.copy();
        return copy;
    }
}
