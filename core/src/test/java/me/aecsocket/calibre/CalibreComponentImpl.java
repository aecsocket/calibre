package me.aecsocket.calibre;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.util.ItemCreationException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.stat.Stat;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.Map;

public class CalibreComponentImpl extends CalibreComponent<Item> {
    public CalibreComponentImpl(String id) {
        super(id);
    }

    public CalibreComponentImpl(CalibreComponent<Item> o) throws SerializationException {
        super(o);
    }

    @Override
    public net.kyori.adventure.text.Component localize(String locale, String key, Object... args) {
        return net.kyori.adventure.text.Component.text("");
    }

    @Override public Map<String, Stat<?>> defaultStats() { return Collections.emptyMap(); }
    @Override protected void prepareStatDeserialization(Map<String, Stat<?>> originals) {}
    @Override public CalibreComponent<Item> getComponent(Item item) { return null; }

    @Override public CalibreComponent<Item> copy() throws SerializationException { return new CalibreComponentImpl(this); }

    @Override
    public Item createInitial(int amount) throws ItemCreationException { return null; }
}
