package me.aecsocket.calibre.system;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

public interface PaperSystem extends CalibreSystem {
    CalibrePlugin plugin();

    @Override
    default Component gen(String locale, String key, Object... args) {
        return plugin().gen(locale, key, args);
    }

    @Override
    default ConfigurationNode setting(Object... path) {
        Object[] fullPath = new Object[path.length + 2];
        fullPath[0] = "system";
        fullPath[1] = id();
        System.arraycopy(path, 0, fullPath, 2, path.length);

        return plugin().setting(fullPath);
    }
}
