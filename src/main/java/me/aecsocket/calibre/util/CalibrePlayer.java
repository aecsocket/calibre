package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CalibrePlayer {
    private final CalibrePlugin plugin;
    private final Player player;

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }
}
