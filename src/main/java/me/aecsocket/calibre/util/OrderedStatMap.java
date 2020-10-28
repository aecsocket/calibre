package me.aecsocket.calibre.util;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.stat.StatMap;

import java.util.*;
import java.util.stream.Collectors;

public class OrderedStatMap extends LinkedHashMap<Integer, StatMap> {
    private boolean dirty;

    public OrderedStatMap(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public OrderedStatMap(int initialCapacity) { super(initialCapacity); }
    public OrderedStatMap() {}
    public OrderedStatMap(Map<? extends Integer, ? extends StatMap> m) { super(m); }

    public boolean isDirty() { return dirty; }

    @Override
    public StatMap put(Integer key, StatMap value) {
        dirty = true;
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends StatMap> m) {
        dirty = true;
        super.putAll(m);
    }

    public OrderedStatMap ordered() {
        if (!dirty) return this;
        List<Map.Entry<Integer, StatMap>> sorted = new ArrayList<>(entrySet());
        sorted.sort(Comparator.comparingInt(Map.Entry::getKey));
        clear();
        for (Map.Entry<Integer, StatMap> entry : sorted)
            put(entry.getKey(), entry.getValue());
        dirty = false;
        return this;
    }

    public String getInfo(CalibrePlugin plugin, String locale) {
        return ordered().entrySet().stream()
                .map(entry -> plugin.gen(locale, "info.stat_map",
                        "order", entry.getKey(),
                        "map", entry.getValue().entrySet().stream()
                                .filter(stat -> stat.getValue().getValue() != null)
                                .map(stat -> plugin.gen(locale, "info.stat_map.stat",
                                        "info", plugin.gen(locale, "info.stat",
                                                "name", stat.getKey(),
                                                "class", TypeToken.get(stat.getValue().getStat().getValueType()).getRawType().getSimpleName(),
                                                "value", stat.getValue().valueToString(),
                                                "default", stat.getValue().defaultToString()
                                        )
                                ))
                                .collect(Collectors.joining())
                ))
                .collect(Collectors.joining("\n"));
    }

    public StatMap flatten() {
        StatMap result = new StatMap();
        ordered().forEach((order, map) -> result.modifyAll(map));
        return result;
    }

    public OrderedStatMap combine(int order, StatMap map) {
        if (containsKey(order))
            get(order).modifyAll(map);
        else
            put(order, map.copy());
        return this;
    }

    public OrderedStatMap combine(OrderedStatMap o) {
        o.forEach(this::combine);
        return this;
    }

    public OrderedStatMap copy() {
        OrderedStatMap result = new OrderedStatMap();
        forEach((order, stats) -> result.put(order, stats.copy()));
        return result;
    }
}
