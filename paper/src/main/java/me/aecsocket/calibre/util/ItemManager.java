package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.spongepowered.configurate.ConfigurateException;

public class ItemManager {
    private final CalibrePlugin plugin;
    private long nextInvalidDataError;

    public ItemManager(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin plugin() { return plugin; }

    public PaperComponent rawComponent(ItemStack item) throws ComponentCreationException {
        if (item == null || !item.hasItemMeta())
            return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        String tree = container.get(plugin.key("tree"), PersistentDataType.STRING);
        if (tree == null)
            return null;
        try {
            return ComponentTree.deserialize(tree, plugin.configOptions()).root();
        } catch (ConfigurateException e) {
            throw new ComponentCreationException(e, tree);
        }
    }

    public PaperComponent component(ItemStack item) {
        if (item == null)
            return null;
        try {
            return rawComponent(item);
        } catch (ComponentCreationException e) {
            long time = System.currentTimeMillis();
            if (time >= nextInvalidDataError) {
                plugin.log(LogLevel.WARN, e, "Could not get component from item %s x %d\nTree: %s", item.getType().name(), item.getAmount(), e.tree());
                nextInvalidDataError = time + plugin.setting("invalid_data_error_delay").getLong(60000);
            }
            return null;
        }
    }

    public ItemStack hide(ItemStack item, boolean hidden) {
        return BukkitUtils.modMeta(item, meta -> {
            PersistentDataContainer data = meta.getPersistentDataContainer();
            if (hidden)
                data.set(plugin.key("hidden"), PersistentDataType.BYTE, (byte) 1);
            else
                data.remove(plugin.key("hidden"));
        });
    }

    public boolean hide(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Byte result = data.get(plugin.key("hidden"), PersistentDataType.BYTE);
        return result != null && result == 1;
    }
}
