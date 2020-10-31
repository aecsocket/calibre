package me.aecsocket.calibre.util.stat;

import me.aecsocket.calibre.util.CalibreParticleData;
import me.aecsocket.unifiedframework.stat.impl.ArrayStat;

import java.lang.reflect.Type;

public class ParticleStat extends ArrayStat<CalibreParticleData> {
    public ParticleStat(CalibreParticleData[] defaultValue) { super(defaultValue); }
    public ParticleStat() {}

    @Override protected CalibreParticleData[] newArray(int i) { return new CalibreParticleData[i]; }
    @Override public Type getValueType() { return CalibreParticleData[].class; }
}
