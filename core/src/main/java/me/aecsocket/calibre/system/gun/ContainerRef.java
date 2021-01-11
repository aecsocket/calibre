package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.unifiedframework.serialization.configurate.ConfigurateSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class ContainerRef<T> {
    public static abstract class Serializer implements TypeSerializer<ContainerRef<?>>, ConfigurateSerializer {
        @Override
        public void serialize(Type type, @Nullable ContainerRef<?> obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.appendListNode().set(obj.path);
                node.appendListNode().set(obj.index);
            }
        }

        protected abstract ContainerRef<?> provide(String[] path, int index, Type type, ConfigurationNode node);

        @Override
        public ContainerRef<?> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            List<? extends ConfigurationNode> nodes = asList(node, type, "path", "index");
            return provide(
                    nodes.get(0).get(String[].class),
                    nodes.get(1).getInt(),
                    type, node
            );
        }
    }

    protected final String[] path;
    protected final int index;

    public ContainerRef(String[] path, int index) {
        this.path = path;
        this.index = index;
    }

    public String[] path() { return path; }
    public int index() { return index; }

    protected abstract T fromComponent(CalibreComponent<?> component);

    public T get(CalibreComponent<?> base) {
        CalibreComponent<?> atPath = base.component(path);
        if (atPath == null)
            return null;
        return fromComponent(atPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerRef<?> that = (ContainerRef<?>) o;
        return index == that.index && Arrays.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(index);
        result = 31 * result + Arrays.hashCode(path);
        return result;
    }

    @Override public String toString() { return Arrays.toString(path) + " @ " + index; }
}
