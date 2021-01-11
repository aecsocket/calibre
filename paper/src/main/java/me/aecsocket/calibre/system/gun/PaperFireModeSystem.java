package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperFireModeSystem extends FireModeSystem {
    private transient final CalibrePlugin plugin;

    public PaperFireModeSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperFireModeSystem() { this(CalibrePlugin.getInstance()); }

    public PaperFireModeSystem(FireModeSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperFireModeSystem(PaperFireModeSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override public PaperFireModeSystem copy() { return new PaperFireModeSystem(this); }
}
