package me.aecsocket.calibre.wrapper;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.util.ItemCreationException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurateException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface BukkitItem extends Item {
    ItemStack item();

    @Override
    default void saveTree(ComponentTree tree) {
        BukkitUtils.modMeta(item(), meta -> {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            CalibrePlugin plugin = CalibrePlugin.instance();
            try {
                plugin.itemManager().set(container, tree);
            } catch (ConfigurateException e) {
                throw new ItemCreationException("Could not serialize tree", e);
            }
        });
    }

    @Override
    default Component name() {
        // TODO wait for Paper to update
        return LegacyComponentSerializer.legacySection().deserialize(item().getItemMeta().getDisplayName());
    }

    @Override
    default void name(Component component) {
        // TODO wait for Paper to update
        BukkitUtils.modMeta(item(), meta -> meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(component)));
    }

    @Override
    default void addInfo(Collection<Component> components) {
        BukkitUtils.modMeta(item(), meta -> {
            List<String> lore = meta.getLore();
            if (lore == null)
                lore = new ArrayList<>();
            else
                lore.add("");

            for (Component line : components)
                // TODO wait for Paper to update
                lore.add(LegacyComponentSerializer.legacySection().serialize(line));
            meta.setLore(lore);
        });
    }

    @Override default int amount() { return item().getAmount(); }

    @Override default boolean add() {
        if (amount() >= item().getMaxStackSize())
            return false;
        item().add();
        return true;
    }
    @Override default void subtract(int amount) { item().subtract(amount); }

    static BukkitItem of(ItemStack item) { return () -> item; }
}
