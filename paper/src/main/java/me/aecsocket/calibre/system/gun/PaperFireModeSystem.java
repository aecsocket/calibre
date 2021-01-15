package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.builtin.StatDisplaySystem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperFireModeSystem extends FireModeSystem {
    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public PaperFireModeSystem(CalibrePlugin plugin, StatDisplaySystem statDisplay) {
        super(statDisplay);
        this.plugin = plugin;
    }

    public PaperFireModeSystem() {}

    public PaperFireModeSystem(FireModeSystem o, CalibrePlugin plugin, StatDisplaySystem statDisplay) {
        super(o);
        this.plugin = plugin;
        this.statDisplay = statDisplay;
    }

    public PaperFireModeSystem(PaperFireModeSystem o) {
        this(o, o.plugin, o.statDisplay);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override
    protected int listenerPriority() {
        return plugin.setting("system", ID, "listener_priority").getInt(1250);
    }

    @Override public PaperFireModeSystem copy() { return new PaperFireModeSystem(this); }
}
