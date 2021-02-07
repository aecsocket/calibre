package me.aecsocket.calibre.system;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;

public interface PaperSystem extends CalibreSystem {
    CalibrePlugin plugin();

    @Override
    default Component gen(String locale, String key, Object... args) {
        return plugin().gen(locale, key, args);
    }
}
