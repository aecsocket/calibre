package me.aecsocket.calibre.util.stat;

import me.aecsocket.unifiedframework.stat.impl.ArrayStat;
import me.aecsocket.unifiedframework.util.data.ParticleData;

import java.lang.reflect.Type;

public class ParticleStat extends ArrayStat<ParticleData> {
    public ParticleStat(ParticleData[] defaultValue) { super(defaultValue); }
    public ParticleStat() {}

    @Override protected ParticleData[] newArray(int i) { return new ParticleData[i]; }
    @Override public Type getValueType() { return ParticleData[].class; }
}
