package com.gitlab.aecsocket.calibre.paper.system.gun;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunInfoSystem;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperGunInfoSystem extends GunInfoSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;
    @FromMaster private final int staminaBarWidth;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperGunInfoSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
        staminaBarWidth = 50;
    }

    /**
     * Used for deserialization.
     */
    public PaperGunInfoSystem() {
        plugin = null;
        staminaBarWidth = 50;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperGunInfoSystem(PaperGunInfoSystem o) {
        super(o);
        plugin = o.plugin;
        staminaBarWidth = o.staminaBarWidth;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override
    protected Component bar(String locale, String key, double percent) {
        return plugin.bar(locale, key, percent, 0d, staminaBarWidth);
    }

    @Override public PaperGunInfoSystem copy() { return new PaperGunInfoSystem(this); }
}
