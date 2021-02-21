package com.gitlab.aecsocket.calibre.core.util;

/**
 * An object which can create and provide an item of type {@link I}.
 * @param <I> The item type.
 */
public interface ItemSupplier<I> extends CalibreIdentifiable {
    /**
     * Creates the item for the specified locale and with the given amount.
     * @param locale The locale to use.
     * @param amount The amount of items to create.
     * @return The item.
     * @throws ItemCreationException If the item could not be created.
     */
    I create(String locale, int amount) throws ItemCreationException;

    /**
     * Creates one item for the specified locale.
     * @param locale The locale to use.
     * @return The item.
     * @throws ItemCreationException If the item could not be created.
     */
    default I create(String locale) throws ItemCreationException { return create(locale, 1); }
}
