package me.aecsocket.calibre.item.system;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deserializes lists of {@link CalibreSystem}s in the format:
 * <code>
 * "systems": {
 *   "system_id": {
 *      "system_field": 3.14,
 *      "other_field": ...
 *   }
 * }
 * </code>
 */
public class SystemListAdapter implements TypeAdapterFactory, JsonAdapter {
    private static final TypeToken<List<CalibreSystem>> type = new TypeToken<>(){};

    private Registry registry;

    public SystemListAdapter(Registry registry) {
        this.registry = registry;
    }

    public Registry getRegistry() { return registry; }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!SystemListAdapter.type.equals(type)) return null;
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override public void write(JsonWriter json, T t) throws IOException { delegate.write(json, t); }

            @Override
            @SuppressWarnings("unchecked")
            public T read(JsonReader json) throws IOException {
                JsonObject object = assertObject(Streams.parse(json));
                List<CalibreSystem> list = new ArrayList<>();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    String systemId = entry.getKey();
                    CalibreSystem system = registry.getRaw(systemId, CalibreSystem.class);
                    if (system == null) throw new JsonParseException(TextUtils.format("System {id} does not exist", "id", systemId));
                    list.add(gson.fromJson(entry.getValue(), system.getClass()));
                }
                return (T) list;
            }
        };
    }
}
