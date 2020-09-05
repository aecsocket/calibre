package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.data.RealSoundData;
import org.bukkit.SoundCategory;
import org.bukkit.plugin.Plugin;

public class CalibreSoundData extends RealSoundData implements AcceptsCalibrePlugin {
    public CalibreSoundData(String sound, SoundCategory category, float volume, float pitch, double dropoff, double range, Plugin plugin, double speed) {
        super(sound, category, volume, pitch, dropoff, range, plugin, speed);
    }

    public CalibreSoundData(String sound, float volume, float pitch, double dropoff, double range, Plugin plugin, double speed) {
        super(sound, volume, pitch, dropoff, range, plugin, speed);
    }

    public CalibreSoundData(String sound, SoundCategory category, double dropoff, double range, Plugin plugin, double speed) {
        super(sound, category, dropoff, range, plugin, speed);
    }

    public CalibreSoundData(String sound, double dropoff, double range, Plugin plugin, double speed) {
        super(sound, dropoff, range, plugin, speed);
    }

    public CalibreSoundData(double dropoff, double range, Plugin plugin, double speed) {
        super(dropoff, range, plugin, speed);
    }

    public CalibreSoundData(Plugin plugin, double speed) {
        super(plugin, speed);
    }

    public CalibreSoundData(Plugin plugin) {
        super(plugin);
    }

    public CalibreSoundData() {}

    @Override public CalibrePlugin getPlugin() { return (CalibrePlugin) super.getPlugin(); }
    @Override public void setPlugin(CalibrePlugin plugin) { super.setPlugin(plugin); }
}
