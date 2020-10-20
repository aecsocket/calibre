package me.aecsocket.calibre.util.stat;

import me.aecsocket.calibre.util.CalibreSoundData;
import me.aecsocket.unifiedframework.stat.impl.ArrayStat;

import java.lang.reflect.Type;

public class SoundStat extends ArrayStat<CalibreSoundData> {
    public SoundStat(CalibreSoundData[] defaultValue) { super(defaultValue); }
    public SoundStat() {}

    @Override protected CalibreSoundData[] newArray(int i) { return new CalibreSoundData[i]; }
    @Override public Type getValueType() { return CalibreSoundData[].class; }
}
