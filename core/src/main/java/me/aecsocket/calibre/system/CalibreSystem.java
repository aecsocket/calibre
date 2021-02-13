package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.slot.ItemSlot;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public interface CalibreSystem extends CalibreIdentifiable {
    class Serializer implements TypeSerializer<CalibreSystem> {
        private final TypeSerializer<CalibreSystem> delegate;
        private final NamingScheme namingScheme;

        public Serializer(TypeSerializer<CalibreSystem> delegate, NamingScheme namingScheme) {
            this.delegate = delegate;
            this.namingScheme = namingScheme;
        }

        public TypeSerializer<CalibreSystem> delegate() { return delegate; }
        public NamingScheme namingScheme() { return namingScheme; }

        @Override
        public void serialize(Type type, @Nullable CalibreSystem obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                for (Field field : Utils.getAllModelFields(obj.getClass())) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;
                    if (field.isAnnotationPresent(FromMaster.class)) continue;
                    try {
                        field.setAccessible(true);
                        node.node(namingScheme.coerce(field.getName())).set(field.get(obj));
                    } catch (IllegalAccessException e) { e.printStackTrace(); }
                }
            }
        }

        @Override
        public CalibreSystem deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return delegate.deserialize(type, node);
        }
    }

    default Map<String, Stat<?>> defaultStats() { return Collections.emptyMap(); }
    default StatCollection buildStats() { return new StatCollection(); }

    CalibreComponent<?> parent();
    default ComponentTree tree() { return parent().tree(); }

    void setup(CalibreComponent<?> parent) throws SystemSetupException;
    void parentTo(ComponentTree tree, CalibreComponent<?> parent);

    CalibreSystem copy();
    default void inherit(CalibreSystem master, boolean fromDefault) {}

    default <I extends Item> void update(ItemUser user, ItemSlot<I> slot, Object cause) {
        tree().build();
        CalibreComponent<I> root = parent().root();
        I item = root.create(user.locale(), slot.get().amount());
        root.tree().call(new ItemEvents.UpdateItem<I>() {
            @Override public Object cause() { return cause; }
            @Override public I item() { return item; }
            @Override public CalibreComponent<I> component() { return root; }
            @Override public ItemUser user() { return user; }
            @Override public ItemSlot<I> slot() { return slot; }
        });
        slot.set(item);
    }
    default <I extends Item> void update(ItemEvents.ItemEvent<I> event) {
        update(event.user(), event.slot(), event);
    }

    static Map<String, CalibreSystem> copySystems(Map<String, CalibreSystem> existing) {
        Map<String, CalibreSystem> result = new HashMap<>();
        for (var entry : existing.entrySet())
            result.put(entry.getKey(), entry.getValue().copy());
        return result;
    }
}
