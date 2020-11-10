package me.aecsocket.calibre.defaults.system.gun.firemode;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import me.aecsocket.calibre.defaults.system.SystemReference;

import java.lang.reflect.Type;
import java.util.List;

public class FireModeReference extends SystemReference<FireModeSystem, FireMode> {
    public static class Adapter extends SystemReference.Adapter<FireModeSystem, FireMode> {
        @Override
        public SystemReference<FireModeSystem, FireMode> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray arr = assertArray(json);
            return new FireModeReference(
                    arr.get(0).isJsonNull() ? null : arr.get(0).getAsString(),
                    arr.get(1).getAsInt()
            );
        }
    }

    public FireModeReference(String path, int index) {
        super(path, index);
    }

    public FireModeReference(SystemReference<FireModeSystem, FireMode> o) { super(o); }

    @Override protected String getSystemId() { return FireModeSystem.ID; }
    @Override
    protected FireMode getMapped(FireModeSystem system) {
        List<FireMode> modes = system.getModes();
        return modes == null
                ? null
                : getIndex() >= modes.size()
                ? null
                : modes.get(getIndex());
    }
}
