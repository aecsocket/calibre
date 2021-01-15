package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.builtin.StatDisplaySystem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperSightSystem extends SightSystem {
    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public PaperSightSystem(CalibrePlugin plugin, StatDisplaySystem statDisplay) {
        super(statDisplay);
        this.plugin = plugin;
    }

    public PaperSightSystem() {}

    public PaperSightSystem(SightSystem o, CalibrePlugin plugin, StatDisplaySystem statDisplay) {
        super(o);
        this.plugin = plugin;
        this.statDisplay = statDisplay;
    }

    public PaperSightSystem(PaperSightSystem o) {
        this(o, o.plugin, o.statDisplay);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override
    protected int listenerPriority() {
        return plugin.setting("system", ID, "listener_priority").getInt(1300);
    }

    @Override public PaperSightSystem copy() { return new PaperSightSystem(this); }
}
