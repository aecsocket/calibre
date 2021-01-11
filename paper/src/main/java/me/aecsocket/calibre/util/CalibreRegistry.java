package me.aecsocket.calibre.util;

import me.aecsocket.unifiedframework.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CalibreRegistry extends Registry<CalibreIdentifiable> {
    private final List<Consumer<CalibreRegistry>> onLoad = new ArrayList<>();

    public List<Consumer<CalibreRegistry>> onLoad() { return onLoad; }
    public void onLoad(Consumer<CalibreRegistry> supplier) { onLoad.add(supplier); }

    @Override
    public void unregisterAll() {
        super.unregisterAll();
        onLoad.forEach(consumer -> consumer.accept(this));
    }
}
