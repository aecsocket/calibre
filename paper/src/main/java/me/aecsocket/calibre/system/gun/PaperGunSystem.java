package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperGunSystem extends GunSystem {
    private transient final CalibrePlugin plugin;

    public PaperGunSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperGunSystem() { this(CalibrePlugin.getInstance()); }

    public PaperGunSystem(GunSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperGunSystem(PaperGunSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override public PaperGunSystem copy() { return new PaperGunSystem(this); }
}
