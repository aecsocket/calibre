package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public class RuledStatCollectionList extends ArrayList<RuledStatCollection> {
    public static final class Serializer implements TypeSerializer<RuledStatCollectionList> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Type type, @Nullable RuledStatCollectionList obj, ConfigurationNode node) throws SerializationException {
            node.set(new TypeToken<ArrayList<RuledStatCollection>>(){}, obj);
        }

        @Override
        public RuledStatCollectionList deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new RuledStatCollectionList(node.get(new TypeToken<ArrayList<RuledStatCollection>>(){}, new ArrayList<>()));
        }
    }

    public RuledStatCollectionList(int initialCapacity) { super(initialCapacity); }
    public RuledStatCollectionList() {}
    public RuledStatCollectionList(@NotNull Collection<? extends RuledStatCollection> c) { super(c); }

    public RuledStatCollectionList copy() {
        RuledStatCollectionList result = new RuledStatCollectionList();
        for (var value : this) {
            result.add(new RuledStatCollection(value));
        }
        return result;
    }

    public StatCollection build(CalibreComponent<?> component) {
        StatCollection result = new StatCollection();
        for (var value : this) {
            result.combine(value.forComponent(component));
        }
        return result;
    }
}
