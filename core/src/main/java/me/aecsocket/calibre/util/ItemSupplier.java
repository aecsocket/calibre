package me.aecsocket.calibre.util;

public interface ItemSupplier<I> extends CalibreIdentifiable {
    I create(String locale, int amount) throws ItemCreationException;
    default I create(String locale) throws ItemCreationException { return create(locale, 1); }
}
