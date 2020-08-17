package me.aecsocket.calibre;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.item.system.SystemListAdapter;
import me.aecsocket.calibre.util.AcceptsCalibrePluginAdapter;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;

/**
 * The default {@link CalibreHook}.
 */
public class CalibreDefaultHook implements CalibreHook {
    @Override
    public void initializeGson(CalibrePlugin plugin, GsonBuilder builder) {
        builder
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new AcceptsCalibrePluginAdapter(plugin))
                .registerTypeAdapterFactory(new SystemListAdapter(plugin.getRegistry()));
    }

    @Override
    public void preLoadRegister(CalibrePlugin plugin, Registry registry, LocaleManager localeManager, Settings settings) {}
}
