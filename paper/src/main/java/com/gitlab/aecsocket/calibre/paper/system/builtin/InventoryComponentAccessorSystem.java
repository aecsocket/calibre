package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentAccessorSystem;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.InventoryUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@ConfigSerializable
public class InventoryComponentAccessorSystem extends AbstractSystem implements ComponentAccessorSystem, PaperSystem {
    public static class InventoryResult implements Result {
        private final PaperComponent component;
        private final ItemStack item;

        public InventoryResult(PaperComponent component, ItemStack item) {
            this.component = component;
            this.item = item;
        }

        @Override public PaperComponent component() { return component; }
        public ItemStack item() { return item; }

        @Override
        public void removeItem() {
            item.subtract();
        }
    }

    public static final String ID = "inventory_component_accessor";

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public InventoryComponentAccessorSystem(CalibrePlugin plugin) {
        super(0);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public InventoryComponentAccessorSystem() {
        super(0);
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public InventoryComponentAccessorSystem(InventoryComponentAccessorSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override public String id() { return ID; }

    @Override
    public InventoryResult collectComponent(ItemUser user, Predicate<CalibreComponent<?>> predicate, @Nullable Comparator<CalibreComponent<?>> sorter) {
        if (!(user instanceof InventoryUser)) return null;
        List<InventoryResult> components = new ArrayList<>();
        for (ItemStack item : ((InventoryUser) user).inventory().getStorageContents()) {
            PaperComponent component = plugin.itemManager().get(item);
            if (component == null)
                continue;
            if (!predicate.test(component))
                continue;

            InventoryResult result = new InventoryResult(component, item);
            if (sorter == null)
                return result;
            else
                components.add(result);
        }
        if (sorter == null)
            return null;
        else {
            components.sort((a, b) -> sorter.compare(a.component, b.component));
            return components.size() > 0 ? components.get(0) : null;
        }
    }

    @Override
    public void addComponent(ItemUser user, CalibreComponent<?> component) {
        if (!(user instanceof PlayerUser)) return;
        Player player = ((PlayerUser) user).entity();
        BukkitUtils.giveItem(player, ((BukkitItem) component.create(player.getLocale())).item());
    }

    @Override public InventoryComponentAccessorSystem copy() { return new InventoryComponentAccessorSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
