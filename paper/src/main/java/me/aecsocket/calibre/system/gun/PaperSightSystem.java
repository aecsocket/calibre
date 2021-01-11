package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperSightSystem extends SightSystem {
    private transient final CalibrePlugin plugin;

    public PaperSightSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperSightSystem() { this(CalibrePlugin.getInstance()); }

    public PaperSightSystem(SightSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperSightSystem(PaperSightSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override public PaperSightSystem copy() { return new PaperSightSystem(this); }
}
