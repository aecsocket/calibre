package me.aecsocket.calibre.defaults;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibreHook;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.CalibreDamage;
import me.aecsocket.calibre.defaults.service.CalibrePenetration;
import me.aecsocket.calibre.defaults.system.ComponentStorageSystem;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.gun.AmmoContainerSystem;
import me.aecsocket.calibre.defaults.system.gun.BulletSystem;
import me.aecsocket.calibre.defaults.system.gun.GunSystem;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireModeReference;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireModeSystem;
import me.aecsocket.calibre.defaults.system.gun.sight.SightReference;
import me.aecsocket.calibre.defaults.system.gun.sight.SightSystem;
import me.aecsocket.calibre.defaults.system.melee.MeleeSystem;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.unifiedframework.util.CollectionInit;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.json.QuantifierAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import java.util.Collection;
import java.util.HashSet;

/**
 * A default hook loaded into the {@link CalibrePlugin}.
 */
public class DefaultCalibreHook implements CalibreHook {
    private CalibrePlugin plugin;

    public CalibrePlugin getPlugin() { return plugin; }

    @Override
    public void acceptPlugin(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(CalibreDamage.Service.class, new CalibreDamage.Provider(), plugin, ServicePriority.Lowest);
        servicesManager.register(CalibrePenetration.Service.class, new CalibrePenetration.Provider(plugin), plugin, ServicePriority.Lowest);
    }

    @Override
    public void registerTypeAdapters(GsonBuilder builder) {
        builder
                .registerTypeAdapter(new TypeToken<Quantifier<ComponentTree>>(){}.getType(), new QuantifierAdapter<ComponentTree>())
                .registerTypeAdapter(FireModeReference.class, new FireModeReference.Adapter())
                .registerTypeAdapter(SightReference.class, new SightReference.Adapter())
                .registerTypeAdapterFactory(new ComponentStorageSystem.Adapter(plugin));
    }

    @Override
    public Collection<CalibreIdentifiable> getPreRegisters() {
        return CollectionInit.of(new HashSet<CalibreIdentifiable>())
                .init(new ItemSystem(plugin))
                .init(new ComponentStorageSystem(plugin))

                .init(new MeleeSystem(plugin))

                .init(new GunSystem(plugin))
                .init(new BulletSystem(plugin))
                .init(new AmmoContainerSystem(plugin))
                .init(new FireModeSystem(plugin))
                .init(new SightSystem(plugin))
                .get();
    }
}
