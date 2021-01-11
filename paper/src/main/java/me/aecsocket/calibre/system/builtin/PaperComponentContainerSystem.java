package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.wrapper.BukkitItem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperComponentContainerSystem extends ComponentContainerSystem<BukkitItem> {
    private transient final CalibrePlugin plugin;

    public PaperComponentContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperComponentContainerSystem() { this(CalibrePlugin.getInstance()); }

    public PaperComponentContainerSystem(ComponentContainerSystem<BukkitItem> o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperComponentContainerSystem(PaperComponentContainerSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override protected int listenerPriority() { return plugin.setting("system", ID, "component_container").getInt(1500); }

    @Override public PaperComponentContainerSystem copy() { return new PaperComponentContainerSystem(this); }
}
