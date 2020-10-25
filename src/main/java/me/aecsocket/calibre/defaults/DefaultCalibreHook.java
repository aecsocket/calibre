package me.aecsocket.calibre.defaults;

import me.aecsocket.calibre.CalibreHook;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.gun.BulletSystem;
import me.aecsocket.calibre.defaults.system.gun.FireableSystem;
import me.aecsocket.calibre.defaults.system.melee.MeleeSystem;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.unifiedframework.util.CollectionInit;

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
    public Collection<CalibreIdentifiable> getPreRegisters() {
        return CollectionInit.of(new HashSet<CalibreIdentifiable>())
                .init(new ItemSystem(plugin))

                .init(new MeleeSystem(plugin))

                .init(new FireableSystem(plugin))
                .init(new BulletSystem(plugin))
                .get();
    }
}
