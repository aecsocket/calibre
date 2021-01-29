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
        return plugin().setting("system", id(), path);
    }
}
