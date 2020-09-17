package me.aecsocket.calibre.util.componentlist;

import com.google.gson.*;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.lang.reflect.Type;

public class CalibreComponentListAdapter implements JsonSerializer<CalibreComponentList>, JsonDeserializer<CalibreComponentList>, JsonAdapter {
    private final Registry registry;

    public CalibreComponentListAdapter(Registry registry) {
        this.registry = registry;
    }

    public Registry getRegistry() { return registry; }

    private void addEntry(JsonArray array, ComponentDescriptor descriptor, int amount, JsonSerializationContext context) {
        if (descriptor == null) return;
        if (amount == 1)
            array.add(context.serialize(descriptor));
        else
            array.add(buildArray().add(amount).add(context.serialize(descriptor)).get());
    }

    @Override
    public JsonElement serialize(CalibreComponentList list, Type type, JsonSerializationContext context) {
        JsonArray arr = new JsonArray();
        ComponentDescriptor descriptor = null;
        int amount = 0;
        for (CalibreComponent component : list) {
            ComponentDescriptor cDescriptor = ComponentDescriptor.of(component);
            if (cDescriptor.equals(descriptor))
                ++amount;
            else {
                addEntry(arr, descriptor, amount, context);
                descriptor = cDescriptor;
                amount = 1;
            }
        }
        addEntry(arr, descriptor, amount, context);
        return arr;
    }

    @Override
    public CalibreComponentList deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        CalibreComponentList list = new CalibreComponentList();
        for (JsonElement elem : assertArray(json)) {
            int amount = 1;
            JsonElement descElem = elem;
            if (elem.isJsonArray()) {
                JsonArray arr = elem.getAsJsonArray();
                amount = arr.get(0).getAsInt();
                descElem = arr.get(1);
            }

            ComponentDescriptor descriptor = context.deserialize(descElem, ComponentDescriptor.class);
            for (int i = 0; i < amount; i++)
                list.add(descriptor.create(registry));
        }
        return list;
    }
}
