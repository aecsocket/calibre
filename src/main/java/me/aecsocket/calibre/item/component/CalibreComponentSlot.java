package me.aecsocket.calibre.item.component;

import com.google.gson.annotations.SerializedName;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Calibre's implementation of {@link ComponentSlot}.
 */
public class CalibreComponentSlot implements ComponentSlot<CalibreComponent>, Cloneable {
    private CalibreComponent component;
    private boolean required;
    private int priority;
    @SerializedName("categories")
    private List<String> compatibleCategories;
    @SerializedName("ids")
    private List<String> compatibleIds;
    private List<String> tags;

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

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<String> getCompatibleCategories() { return compatibleCategories; }
    public void setCompatibleCategories(List<String> compatibleCategories) { this.compatibleCategories = compatibleCategories; }

    public List<String> getCompatibleIds() { return compatibleIds; }
    public void setCompatibleIds(List<String> compatibleIds) { this.compatibleIds = compatibleIds; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    @Override public CalibreComponent get() { return component; }
    @Override
    public void set(CalibreComponent component) throws IncompatibleComponentException {
        if (!isCompatible(component)) throw new IncompatibleComponentException();
        this.component = component;
    }

    @Override
    public boolean isCompatible(@Nullable CalibreComponent component) {
        if (component == null) return true;
        if (compatibleIds != null && compatibleIds.size() > 0 && !compatibleIds.contains(component.getId()))
            return false;
        if (compatibleCategories != null && compatibleCategories.size() > 0 && Collections.disjoint(compatibleCategories, component.getCategories()))
            return false;
        return true;
    }

    public CalibreComponentSlot clone() { try { return (CalibreComponentSlot) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    public CalibreComponentSlot copy() {
        CalibreComponentSlot copy = clone();
        if (component != null)
            copy.component = component.copy();
        return copy;
    }

    @Override
    public String toString() {
        return "CalibreComponentSlot{" +
                "component=" + component +
                ", required=" + required +
                ", priority=" + priority +
                ", compatibleCategories=" + compatibleCategories +
                ", compatibleIds=" + compatibleIds +
                ", tags=" + tags +
                '}';
    }
}
