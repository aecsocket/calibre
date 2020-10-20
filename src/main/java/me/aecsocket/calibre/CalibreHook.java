package me.aecsocket.calibre;

import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.util.CalibreIdentifiable;

import java.util.Collection;
import java.util.Collections;

/**
 * Allows hooking into {@link CalibrePlugin} events.
 */
public interface CalibreHook {
    /**
     * Accepts a provided {@link CalibrePlugin}.
     * @param plugin The plugin.
     */
    void acceptPlugin(CalibrePlugin plugin);

    /**
     * Called when the plugin is enabled.
     */
    default void onEnable() {}

    /**
     * Called when the plugin is disabled.
     */
    default void onDisable() {}

    /**
     * Called when pre register {@link CalibreIdentifiable}s (such as systems) can be registered.
     * @return A collection of pre register objects.
     */
    default Collection<CalibreIdentifiable> getPreRegisters() { return Collections.emptySet(); }

    /**
     * Called when extra type adapters can be registered on a {@link GsonBuilder}.
     * @param builder The builder.
     */
    default void registerTypeAdapters(GsonBuilder builder) {}
}
