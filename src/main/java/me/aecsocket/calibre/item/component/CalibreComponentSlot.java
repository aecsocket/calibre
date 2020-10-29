package me.aecsocket.calibre.item.component;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import me.aecsocket.unifiedframework.gui.GUIVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Calibre's implementation of {@link ComponentSlot}.
 */
public class CalibreComponentSlot implements ComponentSlot<CalibreComponent>, Cloneable {
    private CalibreComponent component;
    private boolean required;
    private int priority;
    private ComponentCompatibility compatibility;
    private List<String> tags;
    private GUIVector offset;
    private boolean canFieldModify;

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

    public ComponentCompatibility getCompatibility() { return compatibility; }
    public void setCompatibility(ComponentCompatibility compatibility) { this.compatibility = compatibility; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public GUIVector getOffset() { return offset; }
    public void setOffset(GUIVector offset) { this.offset = offset; }

    public boolean canFieldModify() { return canFieldModify; }
    public void setCanFieldModify(boolean canFieldModify) { this.canFieldModify = canFieldModify; }

    @Override public CalibreComponent get() { return component; }
    @Override
    public void set(CalibreComponent component) throws IncompatibleComponentException {
        if (!isCompatible(component)) throw new IncompatibleComponentException();
        this.component = component;
    }

    @Override
    public boolean isCompatible(@Nullable CalibreComponent component) {
        return component == null || (compatibility == null || compatibility.test(component));
    }

    public String getInfo(CalibrePlugin plugin, String name, String locale) {
        return plugin.gen(locale, "info.slot" + (required ? ".required" : ""),
                "name", name,
                "tags", tags == null || tags.size() == 0
                        ? plugin.gen(locale, "none")
                        : String.join(", ", tags)
        );
    }

    public CalibreComponentSlot clone() { try { return (CalibreComponentSlot) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    public CalibreComponentSlot copy() {
        CalibreComponentSlot copy = clone();
        if (component != null)
            copy.component = component.copy();
        return copy;
    }
}
