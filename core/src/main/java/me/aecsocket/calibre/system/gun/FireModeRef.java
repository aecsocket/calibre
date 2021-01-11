package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import org.spongepowered.configurate.ConfigurationNode;

import java.lang.reflect.Type;

public class FireModeRef extends ContainerRef<FireMode> {
    public static class Serializer extends ContainerRef.Serializer {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        protected FireModeRef provide(String[] path, int index, Type type, ConfigurationNode node) {
            return new FireModeRef(path, index);
        }
    }

    public FireModeRef(String[] path, int index) {
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
