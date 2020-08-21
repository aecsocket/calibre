package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.io.IOException;

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
                ((CalibreComponent) result).load(new Registry.ResolutionContext(registry), root, gson);
                return result;
            }
        };
    }
}
