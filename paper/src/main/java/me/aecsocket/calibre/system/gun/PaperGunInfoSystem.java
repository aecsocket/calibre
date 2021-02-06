package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.PaperSystem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperGunInfoSystem extends GunInfoSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperGunInfoSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperGunInfoSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperGunInfoSystem(PaperGunInfoSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override
    protected Component bar(String locale, String key, double percent) {
        return plugin.bar(locale, key, percent, 0d, setting("stamina_bar_width").getInt(50));
    }

    @Override public PaperGunInfoSystem copy() { return new PaperGunInfoSystem(this); }
}
