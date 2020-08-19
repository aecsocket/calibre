package me.aecsocket.calibre.item.component.descriptor;

import com.google.gson.*;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;

import java.lang.reflect.Type;

public class ComponentDescriptorAdapter implements JsonDeserializer<ComponentDescriptor>, JsonAdapter {
    @Override
    public ComponentDescriptor deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return new ComponentDescriptor(assertStringPrimitive(assertPrimitive(json)));
    }
}
