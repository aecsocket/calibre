package me.aecsocket.calibre.item.component;

import com.google.gson.annotations.SerializedName;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A slot which can hold a {@link CalibreComponent}.
 */
public class CalibreComponentSlot implements ComponentSlot {
    private CalibreComponent component;
    private boolean required;
    @SerializedName("categories")
    private List<String> compatibleCategories = new ArrayList<>();
    @SerializedName("ids")
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

    @Override public CalibreComponent get() { return component; }

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

    /**
     * Gets all of the compatible categories of component that this slot accepts.
     * To pass compatibility, a component must have at least one of the categories defined here (or it is
     * passed automatically if the list is empty).
     * @return The compatible categories.
     */
    public List<String> getCompatibleCategories() { return compatibleCategories; }
    public void setCompatibleCategories(List<String> compatibleCategories) { this.compatibleCategories = compatibleCategories; }

    /**
     * Gets all of the compatible IDs of component that this slot accepts.
     * To pass compatibility, a component must match at least one of the IDs defined here (or it is
     * passed automatically if the list is empty).
     * @return The compatible IDs.
     */
    public List<String> getCompatibleIds() { return compatibleIds; }
    public void setCompatibleIds(List<String> compatibleIds) { this.compatibleIds = compatibleIds; }

    /**
     * Gets lines of info used by other objects in <code>/calibre info</code>. The string is split
     * by <code>\n</code> to create the line separations. Can be null.
     * @param plugin The CalibrePlugin used for text generation.
     * @param sender The command's sender.
     * @return The info.
     */
    public String getLongInfo(CalibrePlugin plugin, CommandSender sender) {
        return plugin.gen(sender, "chat.info.slot",
                "required", required,
                "categories", String.join(", ", compatibleCategories),
                "ids", String.join(", ", compatibleIds));
    }

    @Override public CalibreComponentSlot clone() { try { return (CalibreComponentSlot) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override
    public CalibreComponentSlot copy() {
        CalibreComponentSlot copy = clone();
        copy.component = component == null ? null : component.copy();
        copy.compatibleCategories = new ArrayList<>(compatibleCategories);
        copy.compatibleIds = new ArrayList<>(compatibleIds);
        return copy;
    }
}
