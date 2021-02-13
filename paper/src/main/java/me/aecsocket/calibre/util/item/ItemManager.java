package me.aecsocket.calibre.util.item;

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
    private final ProtobufManager protobuf;
    private long nextInvalidDataError;

    public ItemManager(CalibrePlugin plugin) {
        this.plugin = plugin;
        protobuf = new ProtobufManager(plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    public ProtobufManager protobuf() { return protobuf; }

    public byte[] tree(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(plugin.key("tree"), PersistentDataType.BYTE_ARRAY);
    }

    public PaperComponent raw(ItemStack item) throws ComponentCreationException {
        byte[] tree = tree(item);
        if (tree == null)
            return null;
        return protobuf.read(tree).root();
    }

    public PaperComponent get(ItemStack item) {
        if (item == null)
            return null;
        try {
            PaperComponent component = raw(item);
            if (component != null)
                component.buildTree();
            return component;
        } catch (ComponentCreationException e) {
            long time = System.currentTimeMillis();
            if (time >= nextInvalidDataError) {
                plugin.log(LogLevel.WARN, e, "Could not get component from item %s x %d", item.getType().name(), item.getAmount());
                nextInvalidDataError = time + plugin.setting("invalid_data_error_delay").getLong(60000);
            }
            return null;
        }
    }

    public void set(PersistentDataContainer data, ComponentTree tree) throws ConfigurateException {
        data.set(plugin.key("tree"), PersistentDataType.BYTE_ARRAY, protobuf.write(tree.root()).toByteArray());
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

    public boolean hidden(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Byte result = data.get(plugin.key("hidden"), PersistentDataType.BYTE);
        return result != null && result == 1;
    }
}
