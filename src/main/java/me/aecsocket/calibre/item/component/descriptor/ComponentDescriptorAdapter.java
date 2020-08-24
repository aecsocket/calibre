package me.aecsocket.calibre.item.component.descriptor;

import com.google.gson.*;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ComponentDescriptorAdapter implements JsonSerializer<ComponentDescriptor>, JsonDeserializer<ComponentDescriptor>, JsonAdapter {
    private final Registry registry;

    public ComponentDescriptorAdapter(Registry registry) {
        this.registry = registry;
    }

    public Registry getRegistry() { return registry; }

    @Override
    public JsonElement serialize(ComponentDescriptor descriptor, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.add("id", new JsonPrimitive(descriptor.getId()));
        descriptor.getSystems().forEach((name, sysDescriptor) -> {
            if (!object.has("systems")) object.add("systems", new JsonObject());
            object.get("systems").getAsJsonObject().add(name, context.serialize(sysDescriptor));
        });
        descriptor.getSlots().forEach((name, child) -> {
            if (!object.has("slots")) object.add("slots", new JsonObject());
            object.get("slots").getAsJsonObject().add(name, context.serialize(child));
        });
        return object.size() == 1 && object.has("id") ? new JsonPrimitive(object.get("id").getAsString()) : object;
    }

    @Override
    public ComponentDescriptor deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) return new ComponentDescriptor(json.getAsString());
        JsonObject object = assertObject(json);
        String id = get(object, "id").getAsString();

        Map<String, Object> systems = new HashMap<>();
        assertObject(get(object, "systems", new JsonObject())).entrySet().forEach(entry -> {
            String name = entry.getKey();
            CalibreSystem<?> system = registry.getRaw(name, CalibreSystem.class);
            if (system == null) return;
            systems.put(name, context.deserialize(entry.getValue(), system.getDescriptorType().getType()));
        });

        Map<String, ComponentDescriptor> slots = new HashMap<>();
        assertObject(get(object, "slots", new JsonObject())).entrySet().forEach(entry ->
                slots.put(entry.getKey(), context.deserialize(entry.getValue(), ComponentDescriptor.class)));

        return new ComponentDescriptor(id, systems, slots);
    }
}
