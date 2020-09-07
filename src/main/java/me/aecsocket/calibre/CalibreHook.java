package me.aecsocket.calibre;

import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;

/**
 * A hook onto {@link CalibrePlugin} which can modify things at enable-time.
 */
public interface CalibreHook {
    /** Provides this hook instance with an instance of the CalibrePlugin.
     * @param plugin The CalibrePlugin.
     */
    void acceptPlugin(CalibrePlugin plugin);

    /**
     * Runs right after registering hooks.
     */
    default void initialize() {}

    /**
     * Runs right before plugin disabling.
     */
    default void disable() {}

    /**
     * Register custom type adapters to the plugin's {@link com.google.gson.Gson} instance.
     * @param builder The {@link GsonBuilder} to modify.
     */
    default void initializeGson(GsonBuilder builder) {}

    /**
     * Register custom items to the plugin's {@link Registry} instance, before the configuration items have been registered.
     * @param registry The {@link Registry} to modify.
     * @param localeManager The {@link LocaleManager} to modify.
     * @param settings The {@link Settings} to modify.
     */
    default void preLoadRegister(Registry registry, LocaleManager localeManager, Settings settings) {}

    /**
     * Register custom items to the plugin's {@link Registry} instance, after all configuration items have been registered.
     * <p>
     * This can be done after the plugin has loaded, but it's neater to put it here.
     * @param registry The {@link Registry} to modify.
     * @param localeManager The {@link LocaleManager} to modify.
     * @param settings The {@link Settings} to modify.
     */
    default void postLoadRegister(Registry registry, LocaleManager localeManager, Settings settings) {}
}
