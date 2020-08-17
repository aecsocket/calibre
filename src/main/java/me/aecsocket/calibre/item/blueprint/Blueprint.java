package me.aecsocket.calibre.item.blueprint;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.ComponentDescriptor;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.unifiedframework.item.Item;
import me.aecsocket.unifiedframework.registry.Identifiable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a premade structure of components.
 */
public class Blueprint implements Identifiable, Item, AcceptsCalibrePlugin {
    public static final String ITEM_TYPE = "blueprint";

    private transient CalibrePlugin plugin;
    private String id;
    private ComponentDescriptor root;
    private Map<String, ComponentDescriptor> components;

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }

    public ComponentDescriptor getRoot() { return root; }
    public void setRoot(ComponentDescriptor root) { this.root = root; }

    public Map<String, ComponentDescriptor> getComponents() { return components; }
    public void setComponents(Map<String, ComponentDescriptor> components) { this.components = components; }

    @Override public String getItemType() { return ITEM_TYPE; }

    @Override
    public ItemStack createItem(@Nullable Player player, int i) {
        return null;
    }
}
