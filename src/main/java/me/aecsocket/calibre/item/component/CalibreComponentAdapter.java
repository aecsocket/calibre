package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.io.IOException;
import java.util.*;

public class CalibreComponentAdapter implements TypeAdapterFactory, JsonAdapter {
    private final Registry registry;
    private final StatMapAdapter statMapAdapter;

    public CalibreComponentAdapter(Registry registry, StatMapAdapter statMapAdapter) {
        this.registry = registry;
        this.statMapAdapter = statMapAdapter;
    }

    public Registry getRegistry() { return registry; }
    public StatMapAdapter getStatMapAdapter() { return statMapAdapter; }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!CalibreComponent.class.isAssignableFrom(type.getRawType())) return null;
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter json, T t) throws IOException { delegate.write(json, t); }

            @Override
            public T read(JsonReader json) throws IOException {
                JsonObject root = assertObject(Streams.parse(json));
                T result = delegate.fromJsonTree(root);
                CalibreComponent component = (CalibreComponent) result;

                // Load systems
                JsonObject object = assertObject(get(root, "systems", new JsonObject()));
                List<CalibreSystem> systems = component.getSystems();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    String systemId = entry.getKey();
                    CalibreSystem system = registry.getRaw(systemId, CalibreSystem.class);
                    if (system == null) throw new JsonParseException(TextUtils.format("System {id} does not exist", "id", systemId));
                    systems.add(gson.fromJson(entry.getValue(), system.getClass()));
                }

                // Load stats
                // 1. Combine all system stats into one stat map
                Map<String, Stat<?>> stats = new LinkedHashMap<>();
                systems.forEach(system -> {
                    Map<String, Stat<?>> toAdd = system.getDefaultStats();
                    if (toAdd != null) stats.putAll(toAdd);
                });
                // 2. Load stats with that map
                statMapAdapter.setStats(stats);
                component.setStats(gson.fromJson(get(root, "stats", new JsonObject()), StatMap.class));

                return result;
            }
        };
    }
}
