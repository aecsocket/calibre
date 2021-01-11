package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperSlotDisplaySystem extends SlotDisplaySystem {
    private transient final CalibrePlugin plugin;

    public PaperSlotDisplaySystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperSlotDisplaySystem() { this(CalibrePlugin.getInstance()); }

    public PaperSlotDisplaySystem(SlotDisplaySystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperSlotDisplaySystem(PaperSlotDisplaySystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override protected int listenerPriority() { return plugin.setting("system", ID, "listener_priority").getInt(2000); }

    @Override public PaperSlotDisplaySystem copy() { return new PaperSlotDisplaySystem(this); }
}
