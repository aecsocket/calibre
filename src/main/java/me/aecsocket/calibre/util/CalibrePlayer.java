package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

/**
 * Wrapper around {@link Player} for storing Calibre's data.
 */
public class CalibrePlayer implements Tickable {
    private final CalibrePlugin plugin;
    private final Player player;

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }

    @Override
    public void tick(TickContext tickContext) {
        PlayerInventory inv = player.getInventory();
        CalibreItem mainHand = plugin.getItem(inv.getItemInMainHand());

    }
}
