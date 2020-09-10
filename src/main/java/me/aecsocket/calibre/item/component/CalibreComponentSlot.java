package me.aecsocket.calibre.item.component;

import com.google.gson.annotations.SerializedName;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A slot which can hold a {@link CalibreComponent}.
 */
public class CalibreComponentSlot implements ComponentSlot, AcceptsCalibrePlugin {
    private transient CalibrePlugin plugin;
    private CalibreComponent component;
    private boolean required;
    @SerializedName("categories")
    private List<String> compatibleCategories = new ArrayList<>();
    @SerializedName("ids")
    private List<String> compatibleIds = new ArrayList<>();
    private GUIVector offset = new GUIVector();
    private boolean canFieldModify;
    private int priority;

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

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
     * Gets the offset that this is displayed at in a {@link me.aecsocket.calibre.defaults.gui.SlotViewGUI}.
     * @return The offset.
     */
    public GUIVector getOffset() { return offset; }
    public void setOffset(GUIVector offset) { this.offset = offset; }

    /**
     * Gets if this slot can be field modified in a slot view GUI.
     * @return The result.
     */
    public boolean canFieldModify() { return canFieldModify; }
    public void setCanFieldModify(boolean canFieldModify) { this.canFieldModify = canFieldModify; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    /**
     * Creates an icon used in a {@link me.aecsocket.calibre.defaults.gui.SlotViewGUI}.
     * @param player The player to create the icon for.
     * @param name The slot name.
     * @param pickedComponent The component to check for compatibility, and to change the icon accordingly.
     * @return The icon.
     */
    public ItemStack createIcon(Player player, String name, @Nullable CalibreComponent pickedComponent) {
        return Utils.modMeta(
                plugin.setting("slot_view.slot." + (
                        pickedComponent == null
                                ? required ? "required" : "normal"
                                : isCompatible(pickedComponent) ? "compatible" : "incompatible"
                ), ItemDescriptor.class, new ItemDescriptor(Material.GRAY_STAINED_GLASS_PANE)).create(),
                meta -> {
                    meta.setDisplayName(plugin.gen(player, "slot." + name));
                    if (canFieldModify)
                        meta.setLore(Collections.singletonList(plugin.gen(player, "gui.slot_view.can_field_modify")));
                });
    }

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
