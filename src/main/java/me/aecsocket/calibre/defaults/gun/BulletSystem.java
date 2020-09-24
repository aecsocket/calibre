package me.aecsocket.calibre.defaults.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.loop.TickContext;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * Implementation of {@link ProjectileProviderSystem} for {@link FireableSystem}s.
 */
public class BulletSystem implements CalibreSystem<Void>,
        ProjectileProviderSystem {
    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;

    public static class Projectile extends ProjectileProviderSystem.Projectile {
        public Projectile(CalibrePlugin plugin, Data data) {
            super(plugin, data);
        }

        @Override
        protected void successHit(TickContext tickContext, RayTraceResult ray) {
            tickContext.remove(); // todo
        }
    }

    public BulletSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "bullet"; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) { this.parent = parent; }

    @Override
    public @Nullable Collection<Class<?>> getServiceTypes() {
        return Arrays.asList(ProjectileProviderSystem.class);
    }

    @Override
    public Projectile create(Data data) {
        return new Projectile(plugin, data);
    }

    public BulletSystem clone() { try { return (BulletSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public BulletSystem copy() { return clone(); }
}
