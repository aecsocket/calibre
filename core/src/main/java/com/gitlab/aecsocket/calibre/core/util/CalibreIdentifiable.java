package com.gitlab.aecsocket.calibre.core.util;

import com.gitlab.aecsocket.unifiedframework.core.registry.Identifiable;
import com.gitlab.aecsocket.unifiedframework.core.registry.ValidationException;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public interface CalibreIdentifiable extends Identifiable {
    String VALID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_.";

    @Override
    default void validate() throws ValidationException {
        id().chars().forEach(i -> {
            String c = Character.toString(i);
            if (!VALID_CHARS.contains(c))
                throw new ValidationException(String.format("ID [%s] contains illegal character '%s', valid characters: [%s]", id(), c, VALID_CHARS));
        });
    }

    Component gen(Locale locale, String key, Object... args);

    default Component name(Locale locale) { return gen(locale, "object." + id()); }
    default Component[] info(Locale locale) { return null; }
}
