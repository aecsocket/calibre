package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;

/**
 * Represents an object which accepts a plugin field of type {@link CalibrePlugin}.
 * <p>
 * This is mainly used to provide a plugin reference during JSON deserialization.
 * Extend this yourself if you are making a deserializable class and want it to take the plugin.
 * @see AcceptsCalibrePluginAdapter
 */
public interface AcceptsCalibrePlugin {
    /**
     * Gets the {@link CalibrePlugin} instance.
     * @return The plugin.
     */
    CalibrePlugin getPlugin();

    /**
     * Sets the {@link CalibrePlugin} instance.
     * @param plugin The plugin.
     */
    void setPlugin(CalibrePlugin plugin);
}
