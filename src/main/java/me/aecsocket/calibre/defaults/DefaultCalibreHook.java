package me.aecsocket.calibre.defaults;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibreHook;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ComponentStorageSystem;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.gun.AmmoContainerSystem;
import me.aecsocket.calibre.defaults.system.gun.BulletSystem;
import me.aecsocket.calibre.defaults.system.gun.FireableSystem;
import me.aecsocket.calibre.defaults.system.melee.MeleeSystem;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.unifiedframework.util.CollectionInit;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.json.QuantifierAdapter;

import java.util.Collection;
import java.util.HashSet;

/**
 * A default hook loaded into the {@link CalibrePlugin}.
 */
public class DefaultCalibreHook implements CalibreHook {
    private CalibrePlugin plugin;

    @Override
    public void acceptPlugin(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerTypeAdapters(GsonBuilder builder) {
        builder
                .registerTypeAdapter(new TypeToken<Quantifier<ComponentTree>>(){}.getType(), new QuantifierAdapter<ComponentTree>())
                .registerTypeAdapterFactory(new ComponentStorageSystem.Adapter(plugin));
    }

    @Override
    public Collection<CalibreIdentifiable> getPreRegisters() {
        return CollectionInit.of(new HashSet<CalibreIdentifiable>())
                .init(new ItemSystem(plugin))
                .init(new ComponentStorageSystem(plugin))

                .init(new MeleeSystem(plugin))

                .init(new FireableSystem(plugin))
                .init(new BulletSystem(plugin))

                .init(new BulletSystem(plugin))
                .init(new AmmoContainerSystem(plugin))
                .get();
    }
}
