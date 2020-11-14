package me.aecsocket.calibre.item.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;

public class DependenciesAdapter implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException { delegate.write(out, value); }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement tree = Streams.parse(in);
                T result = delegate.fromJsonTree(tree);
                // We replace all fields with @Dependencies with the result of deserializing it,
                // but using *this* JSON tree.
                if (result != null) {
                    Class<?> type = result.getClass();
                    for (Field field : type.getDeclaredFields()) {
                        if (field.isAnnotationPresent(LoadTimeDependencies.class)) {
                            field.setAccessible(true);
                            try {
                                field.set(result, gson.fromJson(tree, field.getType()));
                            } catch (IllegalAccessException ignore) {}
                        }
                    }
                }
                return result;
            }
        };
    }
}
