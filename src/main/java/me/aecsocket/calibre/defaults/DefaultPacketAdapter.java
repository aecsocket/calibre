package me.aecsocket.calibre.defaults;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class DefaultPacketAdapter extends PacketAdapter {
    private final CalibrePlugin plugin;

    public DefaultPacketAdapter(CalibrePlugin plugin) {
        super(plugin,
                PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.WINDOW_ITEMS);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();

        if (type == PacketType.Play.Server.SET_SLOT) {
            if (isHidden(packet.getItemModifier().read(0)))
                event.setCancelled(true);
        }
        if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            for (ItemStack item : packet.getItemListModifier().read(0)) {
                if (isHidden(item)) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    private boolean isHidden(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta()
                .getPersistentDataContainer()
                .has(plugin.key("hidden"), PersistentDataType.BYTE);
    }
}
