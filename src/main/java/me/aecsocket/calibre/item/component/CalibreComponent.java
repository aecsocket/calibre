package me.aecsocket.calibre.item.component;

import com.google.gson.annotations.Expose;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.stat.StatMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A component, which can be nested to create component trees. The building block of an item.
 */
public class CalibreComponent implements CalibreItem, Component, ComponentHolder, AcceptsCalibrePlugin {
    public static final String ITEM_TYPE = "component";

    private transient CalibrePlugin plugin;
    private String id;
    private List<String> categories = new ArrayList<>();
    private Map<String, CalibreComponentSlot> slots = new HashMap<>();
    private transient List<CalibreSystem> systems = new ArrayList<>();
    private transient StatMap stats = new StatMap();
    private ItemDescriptor item;

    public CalibreComponent(CalibrePlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public CalibreComponent() {}

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    @Override public Map<String, CalibreComponentSlot> getSlots() { return slots; }
    public void setSlots(Map<String, CalibreComponentSlot> slots) { this.slots = slots; }

    public List<CalibreSystem> getSystems() { return systems; }
    public void setSystems(List<CalibreSystem> systems) { this.systems = systems; }

    public StatMap getStats() { return stats; }
    public void setStats(StatMap stats) { this.stats = stats; }

    public ItemDescriptor getItem() { return item; }
    public void setItem(ItemDescriptor item) { this.item = item; }

    @Override
    public void validate() throws ValidationException {
        CalibreItem.super.validate();
        if (item == null) throw new ValidationException("No item provided");
    }

    @Override public String getItemType() { return ITEM_TYPE; }

    @Override
    public ItemStack createItem(@Nullable Player player, int amount) {
        ItemStack result = item.create();
        result.setAmount(amount);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(getLocalizedName(player));
        result.setItemMeta(meta);
        return result;
    }
}
