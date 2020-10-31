package me.aecsocket.calibre.defaults.service;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class CalibreComponentSupplier {
    public interface Service {
        CalibreComponent supply(CalibreComponentSlot slot, ItemUser user, CalibreSystem requester, boolean consume);
    }

    public static class Provider implements Service {
        private final CalibrePlugin plugin;

        public Provider(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        private CalibreComponent search(CalibreComponentSlot slot, ItemStack stack, boolean consume) {
            CalibreComponent component = plugin.fromItem(stack);
            if (component == null) return null;
            if (slot.isCompatible(component)) {
                if (consume)
                    stack.subtract();
                return component;
            }
            return null;
        }

        @Override
        public CalibreComponent supply(CalibreComponentSlot slot, ItemUser user, CalibreSystem requester, boolean consume) {
            if (!(user instanceof PlayerItemUser)) return null;
            PlayerInventory inv = ((PlayerItemUser) user).getEntity().getInventory();

            CalibreComponent result = search(slot, inv.getItemInOffHand(), consume);
            if (result != null) return result;
            for (ItemStack stack : inv.getStorageContents()) {
                result = search(slot, stack, consume);
                if (result != null) return result;
            }

            return null;
        }
    }

    private CalibreComponentSupplier() {}
}
