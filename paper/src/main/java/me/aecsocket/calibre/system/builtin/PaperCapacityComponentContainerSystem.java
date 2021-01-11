package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.wrapper.BukkitItem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperCapacityComponentContainerSystem extends CapacityComponentContainerSystem<BukkitItem> {
    private transient final CalibrePlugin plugin;

    public PaperCapacityComponentContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperCapacityComponentContainerSystem() { this(CalibrePlugin.getInstance()); }

    public PaperCapacityComponentContainerSystem(CapacityComponentContainerSystem<BukkitItem> o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperCapacityComponentContainerSystem(PaperCapacityComponentContainerSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override protected int listenerPriority() { return plugin.setting("system", ID, "capacity_component_container").getInt(1500); }

    @Override public PaperCapacityComponentContainerSystem copy() { return new PaperCapacityComponentContainerSystem(this); }
}
