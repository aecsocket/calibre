package me.aecsocket.calibre.item.component;

import com.google.gson.annotations.JsonAdapter;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemListAdapter;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.item.Item;
import me.aecsocket.unifiedframework.registry.Identifiable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A component, which can be nested to create component trees. The building block of an item.
 */
public class CalibreComponent implements Component, ComponentHolder, Identifiable, Item, AcceptsCalibrePlugin {
    public static final String ITEM_TYPE = "component";

    private transient CalibrePlugin plugin;
    private String id;
    private List<String> categories;
    private Map<String, CalibreComponentSlot> slots;
    private List<CalibreSystem> systems;

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }
    public List<String> getCategories() { return categories; }
    @Override public Map<String, CalibreComponentSlot> getSlots() { return slots; }
    public List<CalibreSystem> getSystems() { return systems; }

    @Override public String getItemType() { return ITEM_TYPE; }

    @Override
    public ItemStack createItem(@Nullable Player player, int i) {
        return null;
    }
}
