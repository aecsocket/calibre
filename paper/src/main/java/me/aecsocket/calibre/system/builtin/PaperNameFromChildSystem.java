package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromParent;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperNameFromChildSystem extends NameFromChildSystem {
    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public PaperNameFromChildSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperNameFromChildSystem() {}

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
