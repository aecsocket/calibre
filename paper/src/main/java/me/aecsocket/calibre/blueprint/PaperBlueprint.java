package me.aecsocket.calibre.blueprint;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.wrapper.BukkitItem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperBlueprint extends Blueprint<BukkitItem> {
    protected transient final CalibrePlugin plugin;

    public PaperBlueprint(CalibrePlugin plugin, String id) {
        super(id);
        this.plugin = plugin;
    }

    public PaperBlueprint() { this(CalibrePlugin.instance(), null); }

    @Override public Component gen(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }
}
