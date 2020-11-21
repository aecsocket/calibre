package me.aecsocket.calibre.defaults;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibreHook;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.*;
import me.aecsocket.calibre.defaults.system.LaserSystem;
import me.aecsocket.calibre.defaults.system.core.ComponentStorageSystem;
import me.aecsocket.calibre.defaults.system.core.ItemSystem;
import me.aecsocket.calibre.defaults.system.core.SlotDisplaySystem;
import me.aecsocket.calibre.defaults.system.gun.GunSystem;
import me.aecsocket.calibre.defaults.system.gun.ammo.AmmoContainerSystem;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireModeReference;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireModeSystem;
import me.aecsocket.calibre.defaults.system.gun.projectile.BulletSystem;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A default hook loaded into the {@link CalibrePlugin}.
 */
public class DefaultCalibreHook implements CalibreHook {
    private CalibrePlugin plugin;
    private final Map<Class<? extends CalibreInbuilt>, CalibreInbuilt> inbuiltServices = new HashMap<>();

    public CalibrePlugin getPlugin() { return plugin; }

    public Map<?, ?> getInbuiltServices() { return inbuiltServices; }

    @Override
    public void acceptPlugin(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        registerService(CalibreDamage.class, new CalibreDamage.Provider());
        registerService(CalibrePenetration.class, new CalibrePenetration.Provider(plugin));
        registerService(CalibreComponentSupplier.class, new CalibreComponentSupplier.Provider(plugin));
        registerService(CalibreRaytracing.class, new CalibreRaytracing.Provider());
        registerService(CalibreSwayStabilization.class, new CalibreSwayStabilization.Provider(plugin));
    }

    private <S extends CalibreInbuilt, P extends S> void registerService(Class<S> serviceType, P provider) {
        inbuiltServices.put(serviceType, provider);
        Bukkit.getServicesManager().register(serviceType, provider, plugin, ServicePriority.Lowest);
        provider.enable();
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
                .init(new SlotDisplaySystem(plugin))
                .init(new ComponentStorageSystem(plugin))

                .init(new MeleeSystem(plugin))

                .init(new GunSystem(plugin))
                .init(new BulletSystem(plugin))
                .init(new AmmoContainerSystem(plugin))
                .init(new FireModeSystem(plugin))
                .init(new SightSystem(plugin))

                .init(new LaserSystem(plugin))
                .get();
    }
}
