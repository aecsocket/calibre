package me.aecsocket.calibre.hook;

import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;

/**
 * A hook onto {@link CalibrePlugin} which can modify things at enable-time.
 */
public interface CalibreHook {
    /**
     * Register custom type adapters to the plugin's {@link com.google.gson.Gson} instance.
     * @param plugin The plugin.
     * @param builder The {@link GsonBuilder} to modify.
     */
    default void initializeGson(CalibrePlugin plugin, GsonBuilder builder) {}

    /**
     * Register custom items to the plugin's {@link Registry} instance, before the configuration items have been registered.
     * @param plugin The plugin.
     * @param registry The {@link Registry} to modify.
     * @param localeManager The {@link LocaleManager} to modify.
     * @param settings The {@link Settings} to modify.
     */
    default void preLoadRegister(CalibrePlugin plugin, Registry registry, LocaleManager localeManager, Settings settings) {}

    /**
     * Register custom items to the plugin's {@link Registry} instance, after all configuration items have been registered.
     * <p>
     * This can be done after the plugin has loaded, but it's neater to put it here.
     * @param plugin The plugin.
     * @param registry The {@link Registry} to modify.
     * @param localeManager The {@link LocaleManager} to modify.
     * @param settings The {@link Settings} to modify.
     */
    default void postLoadRegister(CalibrePlugin plugin, Registry registry, LocaleManager localeManager, Settings settings) {}
}
