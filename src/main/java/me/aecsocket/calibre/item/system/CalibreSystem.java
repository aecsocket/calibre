package me.aecsocket.calibre.item.system;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.unifiedframework.stat.Stat;

import java.util.Collections;
import java.util.Map;

/**
 * A system which interacts with a {@link CalibreComponent}, such as listening for item events.
 */
public interface CalibreSystem extends CalibreIdentifiable {
    CalibreComponent getParent();
    void initialize(CalibreComponent parent, ComponentTree tree) throws SystemInitializationException;

    default Map<String, Stat<?>> getDefaultStats() { return Collections.emptyMap(); }

    CalibreSystem copy();

    @Override default String getNameKey() { return "system." + getId(); }
}
