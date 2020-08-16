package me.aecsocket.calibre;

import com.google.gson.GsonBuilder;

/**
 * A hook onto {@link CalibrePlugin} which can modify things at enable-time.
 */
public interface CalibreHook {
    /**
     * Allows you to register custom type adapters to the plugin's {@link com.google.gson.Gson} instance.
     * @param builder The {@link GsonBuilder} to modify.
     */
    default void initializeGson(GsonBuilder builder) {}
}
