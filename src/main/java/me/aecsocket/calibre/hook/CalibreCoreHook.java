package me.aecsocket.calibre.hook;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptorAdapter;
import me.aecsocket.calibre.item.system.SystemListAdapter;
import me.aecsocket.calibre.util.AcceptsCalibrePluginAdapter;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.util.json.DefaultedRuntimeTypeAdapterFactory;

/**
 * The core {@link CalibreHook}. This provides core GSON adapters and capability to deserialize the most
 * basic Calibre class. This does not provide adapters for default types.
 */
public class CalibreCoreHook implements CalibreHook {
    private final DefaultedRuntimeTypeAdapterFactory<ComponentDescriptor> componentDescriptorAdapter = DefaultedRuntimeTypeAdapterFactory.of(ComponentDescriptor.class);

    public DefaultedRuntimeTypeAdapterFactory<ComponentDescriptor> getComponentDescriptorAdapter() { return componentDescriptorAdapter; }

    @Override
    public void initializeGson(CalibrePlugin plugin, GsonBuilder builder) {
        builder
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new AcceptsCalibrePluginAdapter(plugin))
                .registerTypeAdapterFactory(new SystemListAdapter(plugin.getRegistry()))

                .registerTypeAdapterFactory(componentDescriptorAdapter)
                .registerTypeAdapter(ComponentDescriptor.class, new ComponentDescriptorAdapter());
    }
}
