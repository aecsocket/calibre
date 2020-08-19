package me.aecsocket.calibre.item.component;

import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A slot which can hold a {@link CalibreComponent}.
 */
public class CalibreComponentSlot implements ComponentSlot {
    private CalibreComponent component;
    private boolean required;
    private List<String> compatibleCategories = new ArrayList<>();
    private List<String> compatibleIds = new ArrayList<>();

    public CalibreComponentSlot(CalibreComponent component, boolean required, List<String> compatibleCategories, List<String> compatibleIds) {
        this.component = component;
        this.required = required;
        this.compatibleCategories = compatibleCategories;
        this.compatibleIds = compatibleIds;
    }

    public CalibreComponentSlot(CalibreComponent component, boolean required, List<String> compatibleCategories) {
        this.component = component;
        this.required = required;
        this.compatibleCategories = compatibleCategories;
    }

    public CalibreComponentSlot(CalibreComponent component, boolean required) {
        this.component = component;
        this.required = required;
    }

    public CalibreComponentSlot(CalibreComponent component) {
        this.component = component;
    }

    public CalibreComponentSlot() {}

    @Override public Component get() { return component; }

    @Override
    public void set(Component component) throws IncompatibleComponentException {
        if (!isCompatible(component)) throw new IncompatibleComponentException();
        this.component = (CalibreComponent) component;
    }

    @Override
    public boolean isCompatible(Component raw) {
        if (raw == null) return true;
        if (!(raw instanceof CalibreComponent)) return false;
        CalibreComponent component = (CalibreComponent) raw;
        return
                (compatibleIds.isEmpty() && compatibleCategories.isEmpty())
                || compatibleIds.contains(component.getId())
                || !Collections.disjoint(compatibleCategories, component.getCategories()
        );
    }

    @Override public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public List<String> getCompatibleCategories() { return compatibleCategories; }
    public List<String> getCompatibleIds() { return compatibleIds; }
}
