package me.aecsocket.calibre.item.component;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.descriptor.ComponentCreationException;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.item.ItemAdapter;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;

public class CalibreComponentAdapter implements TypeAdapterFactory, JsonAdapter, ItemAdapter<CalibreComponent> {
    private final CalibrePlugin plugin;
    private final StatMapAdapter statMapAdapter;
    private final Registry registry;

    public CalibreComponentAdapter(CalibrePlugin plugin, StatMapAdapter statMapAdapter) {
        this.plugin = plugin;
        this.statMapAdapter = statMapAdapter;
        registry = plugin.getRegistry();
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public StatMapAdapter getStatMapAdapter() { return statMapAdapter; }

    @Override public String getItemType() { return CalibreComponent.ITEM_TYPE; }

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

    @Override
    public CalibreComponent deserialize(ItemStack stack, ItemMeta meta, PersistentDataContainer data) {
        String itemData = data.get(plugin.key("data"), PersistentDataType.STRING);
        if (itemData == null) return null;
        CalibreComponent component;
        try {
            component = plugin.getGson().fromJson(itemData, ComponentDescriptor.class).create(registry);
        } catch (ComponentCreationException e) { return null; }

        // Register event listeners
        EventDispatcher dispatcher = new EventDispatcher();
        component.getSystems().values().forEach(system -> system.registerListeners(dispatcher));
        component.setEventDispatcher(dispatcher);

        return component;
    }
}
