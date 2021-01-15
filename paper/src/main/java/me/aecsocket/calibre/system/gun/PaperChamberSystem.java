package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromParent;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperChamberSystem extends ChamberSystem {
    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public PaperChamberSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperChamberSystem() {}

    public PaperChamberSystem(PaperChamberSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperChamberSystem(PaperChamberSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override public PaperChamberSystem copy() { return new PaperChamberSystem(this); }
}
