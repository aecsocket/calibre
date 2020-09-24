package me.aecsocket.calibre.defaults.gun.sight;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.io.IOException;

public class SimpleSightSystemAdapter implements TypeAdapterFactory, JsonAdapter {
    private final CalibrePlugin plugin;

    public SimpleSightSystemAdapter(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!SimpleSightSystem.class.isAssignableFrom(type.getRawType())) return null;
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException { delegate.write(out, value); }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonObject tree = assertObject(Streams.parse(in));
                T result = delegate.fromJsonTree(tree);
                SimpleSightSystem system = (SimpleSightSystem) result;
                system.jSights = tree.has("sights") ? tree.get("sights") : null;
                return result;
            }
        };
    }
}
