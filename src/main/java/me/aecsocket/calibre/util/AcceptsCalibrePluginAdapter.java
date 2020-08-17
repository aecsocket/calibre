package me.aecsocket.calibre.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.CalibrePlugin;

import java.io.IOException;

/**
 * Type adapter for providing {@link AcceptsCalibrePlugin} with plugin fields during deserialization.
 */
public class AcceptsCalibrePluginAdapter implements TypeAdapterFactory {
    private CalibrePlugin plugin;

    public AcceptsCalibrePluginAdapter(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!AcceptsCalibrePlugin.class.isAssignableFrom(type.getRawType())) return null;
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override public void write(JsonWriter json, T t) throws IOException { delegate.write(json, t); }

            @Override
            public T read(JsonReader json) throws IOException {
                T result = delegate.read(json);
                ((AcceptsCalibrePlugin) result).setPlugin(plugin);
                return result;
            }
        };
    }
}
