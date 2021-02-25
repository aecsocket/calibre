package com.gitlab.aecsocket.calibre.paper.wrapper;

import com.gitlab.aecsocket.calibre.core.world.item.FillableItem;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.util.ItemCreationException;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurateException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface BukkitItem extends Item, FillableItem {
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
        return item().getItemMeta().displayName();
    }

    @Override
    default void name(Component component) {
        BukkitUtils.modMeta(item(), meta -> meta.displayName(
                Component.text()
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        .append(component)
                        .build()
        ));
    }

    @Override
    default void addInfo(Collection<Component> components) {
        BukkitUtils.modMeta(item(), meta -> {
            List<Component> lore = meta.lore();
            if (lore == null)
                lore = new ArrayList<>();
            else
                lore.add(Component.empty());

            for (Component component : components) {
                lore.add(
                        Component.text()
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        .append(component)
                        .build()
                );
            }
            meta.lore(lore);
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

    @Override default double filled() {
        ItemStack item = item();
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable))
            return 0;
        return (double) ((Damageable) meta).getDamage() / item.getType().getMaxDurability();
    }

    @Override default void fill(double percentage) {
        ItemStack item = item();
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable))
            return;
        ((Damageable) meta).setDamage((int) ((1 - percentage) * item.getType().getMaxDurability()));
        item.setItemMeta(meta);
    }

    static BukkitItem of(ItemStack item) { return () -> item; }
}
