package me.aecsocket.calibre.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.entity.Player;

public class CalibrePacketAdapter extends PacketAdapter {
    private final CalibrePlugin plugin;

    public CalibrePacketAdapter(CalibrePlugin plugin) {
        super(plugin, PacketType.Play.Server.SET_SLOT);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        if (type == PacketType.Play.Server.SET_SLOT) {
            if (plugin.itemManager().hide(packet.getItemModifier().read(0)))
                event.setCancelled(true);
        }
    }
}
