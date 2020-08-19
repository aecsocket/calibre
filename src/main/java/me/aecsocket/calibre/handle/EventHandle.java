package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.event.Listener;

public class EventHandle implements Listener {
    private final CalibrePlugin plugin;

    public EventHandle(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }
}
