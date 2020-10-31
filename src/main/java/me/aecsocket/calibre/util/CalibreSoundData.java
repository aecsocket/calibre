package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.SoundCategory;
import org.bukkit.plugin.Plugin;

public class CalibreSoundData extends SoundData implements AcceptsCalibrePlugin {
    public CalibreSoundData(Plugin plugin, String sound, SoundCategory category, float volume, float pitch, double dropoff, double range, double speed, long delay) {
        super(plugin, sound, category, volume, pitch, dropoff, range, speed, delay);
    }

    public CalibreSoundData() {
    }

    @Override public CalibrePlugin getPlugin() { return (CalibrePlugin) super.getPlugin(); }
    @Override public void setPlugin(CalibrePlugin plugin) { super.setPlugin(plugin); }
}
