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
 * An object which can take a {@link CalibrePlugin} as a field.
 */
public interface AcceptsCalibrePlugin {
    /**
     * A GSON type adapter for this class.
     */
    class Adapter implements TypeAdapterFactory {
        private final CalibrePlugin plugin;

        public Adapter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!AcceptsCalibrePlugin.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException { delegate.write(out, value); }

                @Override
                public T read(JsonReader in) throws IOException {
                    T value = delegate.read(in);
                    ((AcceptsCalibrePlugin) value).setPlugin(plugin);
                    return value;
                }
            };
        }
    }

    CalibrePlugin getPlugin();
    void setPlugin(CalibrePlugin plugin);
}
