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
    // TODO make clear the distinction:
    // #initialize is for stuff that's run ONCE on load, e.g. system services, stat deserialization
    // and CAN fail, e.g. if dependency is missing (that's why the exception's there)
    // #treeInitialize is for stuff that's run on every tree creation, e.g. registering listeners
    void initialize(CalibreComponent parent) throws SystemInitializationException;
    void treeInitialize(CalibreComponent parent, ComponentTree tree);

    default Map<String, Stat<?>> getDefaultStats() { return Collections.emptyMap(); }

    CalibreSystem copy();

    @Override default String getNameKey() { return "system." + getId(); }
}
