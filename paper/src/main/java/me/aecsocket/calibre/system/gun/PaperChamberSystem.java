package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.PaperSystem;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperChamberSystem extends ChamberSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    public PaperChamberSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperChamberSystem() {}

    public PaperChamberSystem(PaperChamberSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override public PaperChamberSystem copy() { return new PaperChamberSystem(this); }
}
