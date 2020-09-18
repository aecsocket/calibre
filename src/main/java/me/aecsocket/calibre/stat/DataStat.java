package me.aecsocket.calibre.stat;

import me.aecsocket.calibre.util.CalibreSoundData;
import me.aecsocket.unifiedframework.stat.ArrayStat;
import me.aecsocket.unifiedframework.util.data.ParticleData;

import java.lang.reflect.Type;

/**
 * A stat which holds arrays of Calibre data classes. This supports the same operations as {@link ArrayStat}.
 * @param <E> The array type.
 */
public abstract class DataStat<E> extends ArrayStat<E> {
    public DataStat(E[] defaultValue) { super(defaultValue); }
    public DataStat() {}

    public static class Particle extends DataStat<ParticleData> {
        public Particle(ParticleData[] defaultValue) { super(defaultValue); }
        public Particle() {}
        @Override public Type getValueType() { return ParticleData[].class; }
        @Override protected ParticleData[] newArray(int length) { return new ParticleData[length]; }
    }

    public static class Sound extends DataStat<CalibreSoundData> {
        public Sound(CalibreSoundData[] defaultValue) { super(defaultValue); }
        public Sound() {}
        @Override public Type getValueType() { return CalibreSoundData[].class; }
        @Override protected CalibreSoundData[] newArray(int length) { return new CalibreSoundData[length]; }
    }
}
