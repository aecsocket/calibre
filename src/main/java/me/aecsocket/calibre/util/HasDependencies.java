package me.aecsocket.calibre.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

public interface HasDependencies<D> {
    class Adapter implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!HasDependencies.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException { delegate.write(out, value); }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonElement tree = Streams.parse(in);
                    T value = delegate.fromJsonTree(tree);
                    setDependencies((HasDependencies<?>) value, gson, tree);
                    return value;
                }

                private <D> void setDependencies(HasDependencies<D> object, Gson gson, JsonElement tree) {
                    object.setLoadDependencies(gson.fromJson(tree, object.getLoadDependenciesType()));
                }
            };
        }
    }

    D getLoadDependencies();
    void setLoadDependencies(D dependencies);
    Type getLoadDependenciesType();
}
