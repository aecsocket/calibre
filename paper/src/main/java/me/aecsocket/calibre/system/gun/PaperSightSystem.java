package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.PaperSystem;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperSightSystem extends SightSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperSightSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperSightSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperSightSystem(PaperSightSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override public PaperSightSystem copy() { return new PaperSightSystem(this); }
}
