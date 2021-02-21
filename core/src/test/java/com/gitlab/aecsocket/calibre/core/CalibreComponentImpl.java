package com.gitlab.aecsocket.calibre.core;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.world.Item;
import com.gitlab.aecsocket.calibre.core.util.ItemCreationException;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;

import java.util.Collections;
import java.util.Map;

public class CalibreComponentImpl extends CalibreComponent<Item> {
    public CalibreComponentImpl(String id) {
        super(id);
    }

    public CalibreComponentImpl(CalibreComponent<Item> o) {
        super(o);
    }

    @Override
    public net.kyori.adventure.text.Component gen(String locale, String key, Object... args) {
        return net.kyori.adventure.text.Component.text("");
    }

    @Override public Map<String, Stat<?>> defaultStats() { return Collections.emptyMap(); }
    @Override protected void prepareStatDeserialization(Map<String, Stat<?>> originals) {}
    @Override public CalibreComponent<Item> getComponent(Item item) { return null; }

    @Override public CalibreComponent<Item> copy() { return new CalibreComponentImpl(this); }

    @Override
    public Item createInitial(int amount) throws ItemCreationException { return null; }
}
