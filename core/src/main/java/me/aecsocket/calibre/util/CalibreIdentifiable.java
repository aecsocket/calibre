package me.aecsocket.calibre.util;

import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.ValidationException;
import net.kyori.adventure.text.Component;

public interface CalibreIdentifiable extends Identifiable {
    String VALID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_.";

    @Override
    default void validate() throws ValidationException {
        id().chars().forEach(i -> {
            String c = Character.toString(i);
            if (!VALID_CHARS.contains(c))
                throw new ValidationException("ID contains illegal character '" + c + "', valid characters: [" + VALID_CHARS + "]");
        });
    }

    Component localize(String locale, String key, Object... args);

    default Component name(String locale) { return localize(locale, "object." + id()); }
    default Component[] info(String locale) { return null; }
}
