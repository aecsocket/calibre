package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.ItemSlot;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.unifiedframework.stat.Stat;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public interface CalibreSystem extends CalibreIdentifiable {
    default Map<String, Stat<?>> defaultStats() { return Collections.emptyMap(); }
    default StatCollection buildStats() { return new StatCollection(); }

    CalibreComponent<?> parent();
    default ComponentTree tree() { return parent().tree(); }
    ConfigurationNode setting(Object... path);

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
        for (Map.Entry<String, CalibreSystem> entry : existing.entrySet())
            result.put(entry.getKey(), entry.getValue().copy());
        return result;
    }
}
