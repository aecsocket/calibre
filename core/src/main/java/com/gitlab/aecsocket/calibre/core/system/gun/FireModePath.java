package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import org.spongepowered.configurate.ConfigurationNode;

import java.lang.reflect.Type;

public class FireModePath extends ContainerPath<FireMode> {
    public static class Serializer extends ContainerPath.Serializer {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        protected FireModePath provide(String[] path, int index, Type type, ConfigurationNode node) {
            return new FireModePath(path, index);
        }
    }

    public FireModePath(String[] path, int index) {
        super(path, index);
    }

    @Override
    protected FireMode fromComponent(CalibreComponent<?> component) {
        FireModeSystem system = component.system(FireModeSystem.class);
        if (system == null)
            return null;
        return index < system.fireModes.size() ? system.fireModes.get(index) : null;
    }
}
