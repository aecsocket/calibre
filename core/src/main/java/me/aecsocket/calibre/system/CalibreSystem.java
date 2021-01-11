package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.unifiedframework.stat.Stat;
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

    void setup(CalibreComponent<?> parent) throws SystemSetupException;
    void parentTo(ComponentTree tree, CalibreComponent<?> parent);

    CalibreSystem copy();
    default void inherit(CalibreSystem child) {}

    static Map<String, CalibreSystem> copySystems(Map<String, CalibreSystem> existing) {
        Map<String, CalibreSystem> result = new HashMap<>();
        for (Map.Entry<String, CalibreSystem> entry : existing.entrySet())
            result.put(entry.getKey(), entry.getValue().copy());
        return result;
    }
}
