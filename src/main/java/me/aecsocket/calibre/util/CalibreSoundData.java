package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.data.RealSoundData;
import org.bukkit.SoundCategory;

public class CalibreSoundData extends RealSoundData {
    private final CalibrePlugin plugin;

    public CalibreSoundData(String sound, SoundCategory category, float volume, float pitch, double dropoff, double range, CalibrePlugin plugin, double speed) {
        super(sound, category, volume, pitch, dropoff, range, plugin, speed);
        this.plugin = plugin;
    }

    public CalibreSoundData(String sound, float volume, float pitch, double dropoff, double range, CalibrePlugin plugin, double speed) {
        super(sound, volume, pitch, dropoff, range, plugin, speed);
        this.plugin = plugin;
    }

    public CalibreSoundData(String sound, SoundCategory category, double dropoff, double range, CalibrePlugin plugin, double speed) {
        super(sound, category, dropoff, range, plugin, speed);
        this.plugin = plugin;
    }

    public CalibreSoundData(String sound, double dropoff, double range, CalibrePlugin plugin, double speed) {
        super(sound, dropoff, range, plugin, speed);
        this.plugin = plugin;
    }

    public CalibreSoundData(double dropoff, double range, CalibrePlugin plugin, double speed) {
        super(dropoff, range, plugin, speed);
        this.plugin = plugin;
    }

    public CalibreSoundData(CalibrePlugin plugin, double speed) {
        super(plugin, speed);
        this.plugin = plugin;
    }

    public CalibreSoundData(CalibrePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
}
