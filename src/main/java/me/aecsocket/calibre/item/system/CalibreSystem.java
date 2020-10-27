package me.aecsocket.calibre.item.system;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.unifiedframework.stat.Stat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A system which interacts with a {@link CalibreComponent}, such as listening for item events.
 */
public interface CalibreSystem extends CalibreIdentifiable {
    class Adapter implements TypeAdapterFactory {
        private CalibreComponent parent;

        public CalibreComponent getParent() { return parent; }
        public void setParent(CalibreComponent parent) { this.parent = parent; }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!CalibreSystem.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException { delegate.write(out, value); }

                @Override
                public T read(JsonReader in) throws IOException {
                    T result = delegate.read(in);
                    if (parent != null) {
                        // We replace all fields with @LoadTimeOnly with the parent component's same systems' copy
                        // of the value.
                        CalibreSystem sys = (CalibreSystem) result;
                        Class<? extends CalibreSystem> type = sys.getClass();
                        for (Field field : type.getDeclaredFields()) {
                            if (field.isAnnotationPresent(LoadTimeOnly.class)) {
                                CalibreSystem parentCopy = parent.getSystem(sys.getId());
                                if (parentCopy != null) {
                                    field.setAccessible(true);
                                    try {
                                        field.set(sys, field.get(parentCopy));
                                    } catch (IllegalAccessException ignore) {}
                                }
                            }
                        }
                    }
                    return result;
                }
            };
        }
    }

    CalibreComponent getParent();
    void initialize(CalibreComponent parent, ComponentTree tree) throws SystemInitializationException;

    default Map<String, Stat<?>> getDefaultStats() { return Collections.emptyMap(); }

    CalibreSystem copy();
    /*
    One possible approach: let the system decide if it wants to handle its own deserialization or the tree
    deserializes for it.
    true = "handle this for me", false = "I can handle this myself"
    default boolean merge(CalibreSystem system) { return true; }
     */

    @Override default String getNameKey() { return "system." + getId(); }

    @Override default String getLongInfo(String locale) {
        return getPlugin().gen(locale, "info.system",
                "localized_name", getLocalizedName(locale),
                "fields", Stream.of(getClass().getDeclaredFields())
                        .map(field -> {
                            if (
                                    Modifier.isStatic(field.getModifiers())
                                    || Modifier.isTransient(field.getModifiers())
                            ) return null;
                            try {
                                field.setAccessible(true);
                                return getPlugin().gen(locale, "info.system.field" + (field.isAnnotationPresent(LoadTimeOnly.class) ? ".load_only" : ""),
                                        "field", field.getName(),
                                        "value", field.get(this)
                                );
                            } catch (IllegalAccessException e) { return null; }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining())
        );
    }
}
