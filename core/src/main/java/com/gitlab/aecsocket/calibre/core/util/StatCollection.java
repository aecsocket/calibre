package com.gitlab.aecsocket.calibre.core.util;

import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StatCollection extends HashMap<Integer, StatMap> {
    public static class Serializer implements TypeSerializer<StatCollection> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Type type, @Nullable StatCollection obj, ConfigurationNode node) throws SerializationException {
            node.set(new TypeToken<Map<Integer, StatMap>>(){}, obj);
        }

        @Override
        public StatCollection deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new StatCollection(node.get(new TypeToken<Map<Integer, StatMap>>(){}));
        }
    }

    public StatCollection(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public StatCollection(int initialCapacity) { super(initialCapacity); }
    public StatCollection() {}
    public StatCollection(Map<? extends Integer, ? extends StatMap> m) { super(m); }
    public StatCollection(StatCollection o) {
        o.forEach((priority, map) -> put(priority, new StatMap(map)));
    }

    public StatCollection add(int priority, StatMap map) {
        if (containsKey(priority))
            get(priority).modAll(map);
        else
            put(priority, new StatMap(map));
        return this;
    }

    public StatCollection combine(Map<? extends Integer, ? extends StatMap> other) {
        if (other != null)
            other.forEach(this::add);
        return this;
    }

    public Map<Integer, StatMap> ordered() {
        TreeMap<Integer, StatMap> result = new TreeMap<>((a, b) -> b >= 0 ? a - b : b - a);
        result.putAll(this);
        return result;
    }

    public StatMap flatten() {
        StatMap result = new StatMap();
        for (var entry : ordered().entrySet()) {
            result.modAll(entry.getValue());
        }
        return result;
    }
}
