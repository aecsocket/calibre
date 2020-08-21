package me.aecsocket.calibre.item.system;

import me.aecsocket.unifiedframework.stat.BooleanStat;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

// TODO remove from prod
public class TestSystem implements CalibreSystem {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("number_stat", new NumberStat.Int(3))
            .init("bool_stat", new BooleanStat(false))
            .get();

    @Override public String getId() { return "test"; }

    @Override
    public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public TestSystem clone() { try { return (TestSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public CalibreSystem copy() { return clone(); }
}
