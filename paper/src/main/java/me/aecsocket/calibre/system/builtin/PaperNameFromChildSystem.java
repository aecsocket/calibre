package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperNameFromChildSystem extends NameFromChildSystem {
    private transient final CalibrePlugin plugin;

    public PaperNameFromChildSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperNameFromChildSystem() { this(CalibrePlugin.getInstance()); }

    public PaperNameFromChildSystem(NameFromChildSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperNameFromChildSystem(PaperNameFromChildSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override protected int listenerPriority() { return plugin.setting("system", ID, "listener_priority").getInt(1500); }

    @Override public PaperNameFromChildSystem copy() { return new PaperNameFromChildSystem(this); }
}
