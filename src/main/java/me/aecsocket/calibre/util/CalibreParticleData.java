package me.aecsocket.calibre.util;

import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

public class CalibreParticleData extends ParticleData {
    public CalibreParticleData(Particle particle, int count, Vector size, double speed, Object data) {
        super(particle, count, size, speed, data);
    }

    public CalibreParticleData() {}
}
