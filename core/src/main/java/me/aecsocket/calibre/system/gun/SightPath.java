package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import org.spongepowered.configurate.ConfigurationNode;

import java.lang.reflect.Type;

public class SightPath extends ContainerPath<Sight> {
    public static class Serializer extends ContainerPath.Serializer {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        protected SightPath provide(String[] path, int index, Type type, ConfigurationNode node) {
            return new SightPath(path, index);
        }
    }

    public SightPath(String[] path, int index) {
        super(path, index);
    }

    @Override
    protected Sight fromComponent(CalibreComponent<?> component) {
        SightSystem system = component.system(SightSystem.class);
        if (system == null)
            return null;
        return index < system.sights.size()
                ? system.sights.get(index)
                : null;
    }
}
