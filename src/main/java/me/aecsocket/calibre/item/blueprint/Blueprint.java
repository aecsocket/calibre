package me.aecsocket.calibre.item.blueprint;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.descriptor.ComponentCreationException;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * Represents a premade structure of components.
 */
public class Blueprint extends CalibreItem {
    public static final String ITEM_TYPE = "blueprint";

    private transient CalibrePlugin plugin;
    private String id;
    private ComponentDescriptor root;
    private LinkedHashMap<String, ComponentDescriptor> components;

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }

    public ComponentDescriptor getRoot() { return root; }
    public void setRoot(ComponentDescriptor root) { this.root = root; }

    public LinkedHashMap<String, ComponentDescriptor> getComponents() { return components; }
    public void setComponents(LinkedHashMap<String, ComponentDescriptor> components) { this.components = components; }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (root == null) throw new ValidationException("No root provided");
    }

    @Override public String getItemType() { return ITEM_TYPE; }

    /**
     * Creates a component tree from this instance's fields.
     * @return The component tree.
     * @throws BlueprintCreationException If there was an error when creating a component.
     */
    public CalibreComponent createComponent() throws BlueprintCreationException {
        Registry registry = plugin.getRegistry();
        CalibreComponent root;
        try {
            root = this.root.create(registry);
        } catch (ComponentCreationException e) {
            throw new BlueprintCreationException(TextUtils.format("Failed to create root component: {msg}", "msg", e.getMessage()), e);
        }
        if (components != null) {
            components.forEach((path, descriptor) -> {
                ComponentSlot slot = root.getSlot(path);
                if (slot == null)
                    throw new BlueprintCreationException(TextUtils.format("Failed to find slot at path {path}", "path", path));
                CalibreComponent component = descriptor.create(registry);
                if (!slot.isCompatible(component))
                    throw new BlueprintCreationException(TextUtils.format("Slot at path {path} is not compatible with {id}", "path", path, "id", component.getId()));
                slot.set(component);
            });
        }
        return root;
    }

    @Override
    public ItemStack createItem(@Nullable Player player, int amount) { return createComponent().createItem(player, amount); }

    @Override public @Nullable String getShortInfo(CommandSender sender) { return getLocalizedName(sender); }

    @Override
    public @Nullable String getLongInfo(CommandSender sender) {
        return "TODO"; // TODO
    }
}
