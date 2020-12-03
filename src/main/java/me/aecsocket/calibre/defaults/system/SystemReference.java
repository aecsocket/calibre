package me.aecsocket.calibre.defaults.system;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.component.ComponentHolder;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.serialization.json.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Objects;

public abstract class SystemReference<S extends CalibreSystem, T> {
    public static abstract class Adapter<S extends CalibreSystem, T> implements JsonSerializer<SystemReference<S, T>>, JsonDeserializer<SystemReference<S, T>>, JsonAdapter {
        @Override
        public JsonElement serialize(SystemReference src, Type typeOfSrc, JsonSerializationContext context) {
            return buildArray()
                    .add(src.getPath())
                    .add(src.getIndex())
                    .get();
        }
    }

    private final String path;
    private final int index;

    public SystemReference(String path, int index) {
        this.path = path;
        this.index = index;
    }

    public SystemReference(SystemReference<S, T> o) {
        path = o.path;
        index = o.index;
    }

    public String getPath() { return path; }
    public int getIndex() { return index; }

    protected abstract String getSystemId();
    protected abstract T getMapped(S system);

    public CalibreComponent fromPath(ComponentHolder<?> holder) {
        ComponentHolder<?> target = null;
        if (path == null)
            target = holder;
        else {
            ComponentSlot<?> slot = holder.getSlot(path);
            if (slot == null) return null;
            if (slot.get() instanceof ComponentHolder<?>)
                target = (ComponentHolder<?>) slot.get();
        }

        if (!(target instanceof CalibreComponent)) return null;
        return (CalibreComponent) target;
    }

    @SuppressWarnings("unchecked")
    public T getMapped(ComponentHolder<?> holder) {
        CalibreComponent component = fromPath(holder);
        if (component == null) return null;
        CalibreSystem sys = component.getSystem(getSystemId());
        return sys == null ? null : getMapped((S) sys);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        SystemReference<S, T> that = (SystemReference<S, T>) o;
        return index == that.index &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, index);
    }

    @Override public String toString() { return path + "@" + index; }
}
