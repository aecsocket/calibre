package me.aecsocket.calibre.defaults.system.gun.sight;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import me.aecsocket.calibre.defaults.system.SystemReference;

import java.lang.reflect.Type;
import java.util.List;

public class SightReference extends SystemReference<SightSystem, Sight> {
    public static class Adapter extends SystemReference.Adapter<SightSystem, Sight> {
        @Override
        public SystemReference<SightSystem, Sight> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray arr = assertArray(json);
            return new SightReference(
                    arr.get(0).isJsonNull() ? null : arr.get(0).getAsString(),
                    arr.get(1).getAsInt()
            );
        }
    }

    public SightReference(String path, int index) {
        super(path, index);
    }

    public SightReference(SystemReference<SightSystem, Sight> o) { super(o); }

    @Override protected Class<SightSystem> getSystemType() { return SightSystem.class; }
    @Override
    protected Sight getMapped(SightSystem system) {
        List<Sight> modes = system.getSights();
        return getIndex() >= modes.size()
                ? null : modes.get(getIndex());
    }
}
