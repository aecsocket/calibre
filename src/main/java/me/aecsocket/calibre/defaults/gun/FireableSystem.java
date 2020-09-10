package me.aecsocket.calibre.defaults.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ActionSystem;
import me.aecsocket.calibre.defaults.service.system.MainSystem;
import me.aecsocket.calibre.defaults.service.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.calibre.util.SystemRepresentation;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.VectorStat;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FireableSystem implements CalibreSystem<Void>,
        MainSystem,
        ItemEvents.Interact.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("damage", new NumberStat.Double(0d))

            .init("barrel_offset", new VectorStat(new Vector()))
            .init("chamber_priority", new NumberStat.Int(0))

            .init("projectiles", new NumberStat.Int(1))
            .init("projectile_velocity", new NumberStat.Double(0d))
            .init("projectile_bounce", new NumberStat.Double(0d))
            .init("projectile_drag", new NumberStat.Double(0d))
            .init("projectile_gravity", new NumberStat.Double(Projectile.GRAVITY))
            .init("projectile_expansion", new NumberStat.Double(0d))
            .init("projectile_trail", new DataStat.Particle())

            .init("fire_delay", new NumberStat.Long(1L))
            .init("fire_sound", new DataStat.Sound())
            .init("fire_particles", new DataStat.Particle())
            .init("fire_animation", new AnimationStat())

            .init("empty_delay", new NumberStat.Long(1L))
            .init("empty_sound", new DataStat.Sound())
            .init("empty_particles", new DataStat.Particle())
            .init("empty_animation", new AnimationStat())
            .get();

    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private transient ActionSystem actionSystem;

    public FireableSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public ActionSystem getActionSystem() { return actionSystem; }

    @Override public String getId() { return "fireable"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) {
        this.parent = parent;
        actionSystem = getSystem(ActionSystem.class);
    }

    @Override
    public @NotNull Collection<Class<?>> getDependencies() {
        return Arrays.asList(
                ActionSystem.class
        );
    }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.Interact.class, this, 0);
    }

    public Location getBarrelLocation(LivingEntity entity) {
        Location location = Utils.getFacingRelative(entity, stat("barrel_offset"));
        return Utils.isObstructed(entity, location) ? null : location;
    }

    public void fire(Events.PreFire<?> event) {
        if (callEvent(event).cancelled) return;

        LivingEntity shooter = event.shooter;
        Location barrelLocation = getBarrelLocation(shooter);

        if (barrelLocation == null) return;

        Collection<SystemRepresentation<ProjectileProviderSystem>> chambers = getAllSystems(ProjectileProviderSystem.class, stat("chamber_priority"));
        if (chambers.size() == 0) {
            empty(new Events.Empty<>(
                    event.getItemStack(), event.getSlot(), event.system, event.shooter,
                    barrelLocation
            ));
            return;
        }
        SystemRepresentation<ProjectileProviderSystem> chamberRep = chambers.iterator().next();
        ProjectileProviderSystem chamber = chamberRep.getSystem();
        CalibreComponentSlot chamberSlot = chamberRep.getSlot();

        Events.Fire<?> event2 = new Events.Fire<>(
                event.getItemStack(), event.getSlot(), event.system, event.shooter,
                barrelLocation.clone(), chamber, chamberSlot
        );
        if (callEvent(event2).isCancelled()) return;

        barrelLocation = event2.barrelLocation;
        chamber = event2.chamber;
        chamberSlot = event2.chamberSlot;

        chamberSlot.set(null);

        for (int i = 0; i < (int) stat("projectiles"); i++) {
            Projectile projectile = chamber.create(
                    new ProjectileData(
                            barrelLocation, barrelLocation.getDirection().multiply((double) stat("projectile_velocity")),
                            stat("projectile_bounce"), stat("projectile_drag"), stat("projectile_gravity"), stat("projectile_expansion"),
                            stat("projectile_trail")
                    )
            ).inEntity(shooter);
            plugin.getSchedulerLoop().registerTickable(projectile);
        }

        actionSystem.startAction(
                stat("fire_delay"),
                barrelLocation, stat("fire_sound"), stat("fire_particles"),
                shooter, event.getSlot(), stat("fire_animation"));
        updateItem(shooter, event.getSlot(), stat("fire_animation"));

        /*Projectile projectile = new Projectile(barrelLocation, barrelLocation.getDirection().multiply(200), 1, 0.2) {
            private double stepSize = 0.5;
            private double travelled;

            @Override
            protected void step(TickContext tickContext, Vector from, Vector delta) {
                World world = getLocation().getWorld();

                double distance = delta.length();
                if (distance < 0.02 && getLocation().clone().subtract(0, 0.01, 0).getBlock().getType() != Material.AIR) {
                    tickContext.remove();
                    return;
                }

                travelled += distance;
                while (travelled > stepSize) {
                    double percent = travelled / distance;
                    world.spawnParticle(Particle.END_ROD,
                            from.clone().add(delta.clone().multiply(percent)).toLocation(world),
                            0, 0, 0, 0, 0, null, true);
                    travelled -= stepSize;
                }
            }

            @Override
            protected void successHit(TickContext tickContext, RayTraceResult ray) {
                if (getBounce() > 0) {
                    Vector v2 = Utils.getReflectionDirection(ray.getHitBlockFace());
                    if (v2 == null) return;
                    double dot = getVelocity().dot(v2);
                    if (dot > -80 && dot < 80) {
                        Utils.reflect(getVelocity(), ray.getHitBlockFace());
                    } else {
                        tickContext.remove();
                    }
                }
            }

            @Override
            protected void hitBlock(TickContext tickContext, RayTraceResult ray, Block block) {
                getLocation().getWorld().spawnParticle(Particle.REDSTONE, getLocation(), 0, new Particle.DustOptions(Color.RED, 3f));
            }

            @Override
            protected void hitEntity(TickContext tickContext, RayTraceResult ray, Entity entity) {
                getLocation().getWorld().spawnParticle(Particle.REDSTONE, getLocation(), 0, new Particle.DustOptions(Color.BLUE, 3f));
            }
        }.inEntity(shooter);
        plugin.getSchedulerLoop().registerTickable(projectile);*/
    }

    public void empty(Events.Empty<?> event) {
        if (callEvent(event).isCancelled()) return;

        LivingEntity shooter = event.getShooter();
        Location location = event.barrelLocation;

        actionSystem.startAction(
                stat("empty_delay"),
                location, stat("empty_sound"), stat("empty_particles"),
                shooter, event.getSlot(), stat("empty_animation"));
        updateItem(shooter, event.getSlot(), stat("empty_animation"));
    }

    @Override
    public void onEvent(ItemEvents.Interact<?> event) {
        if (!isCompleteRoot()) return;
        if (event.isRightClick()) return;
        if (!actionSystem.isAvailable()) return;

        fire(new Events.PreFire<>(
                event.getItemStack(),
                event.getSlot(),
                this,
                event.getPlayer()
        ));
    }

    @Override public FireableSystem clone() { try { return (FireableSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public CalibreSystem<Void> copy() { return clone(); }

    public static class ProjectileData extends ProjectileProviderSystem.GenericData {
        public ProjectileData(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, ParticleData[] trail) {
            super(location, velocity, bounce, drag, gravity, expansion, trail);
        }
    }

    public static final class Events {
        private Events() {}

        public static class PreFire<L extends PreFire.Listener> extends ItemEvents.Event<L> implements ItemEvents.SystemEvent<FireableSystem>, Cancellable {
            public interface Listener { void onEvent(PreFire<?> event); }

            private final FireableSystem system;
            private final LivingEntity shooter;
            private boolean cancelled;

            public PreFire(ItemStack itemStack, EquipmentSlot slot, FireableSystem system, LivingEntity shooter) {
                super(itemStack, slot);
                this.system = system;
                this.shooter = shooter;
            }

            @Override public FireableSystem getSystem() { return system; }
            public LivingEntity getShooter() { return shooter; }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            @Override public void call(Listener listener) { listener.onEvent(this); }
        }

        public static class Empty<L extends Empty.Listener> extends PreFire<L> {
            public interface Listener extends PreFire.Listener { void onEvent(Empty<?> event); }

            private Location barrelLocation;

            public Empty(ItemStack itemStack, EquipmentSlot slot, FireableSystem system, LivingEntity shooter, Location barrelLocation) {
                super(itemStack, slot, system, shooter);
                this.barrelLocation = barrelLocation;
            }

            public Location getBarrelLocation() { return barrelLocation; }
            public void setBarrelLocation(Location barrelLocation) { this.barrelLocation = barrelLocation; }

            @Override public void call(Listener listener) { listener.onEvent(this); }
        }

        public static class Fire<L extends Fire.Listener> extends PreFire<L> {
            public interface Listener extends PreFire.Listener { void onEvent(Fire<?> event); }

            private Location barrelLocation;
            private ProjectileProviderSystem chamber;
            private CalibreComponentSlot chamberSlot;

            public Fire(ItemStack itemStack, EquipmentSlot slot, FireableSystem system, LivingEntity shooter, Location barrelLocation, ProjectileProviderSystem chamber, CalibreComponentSlot chamberSlot) {
                super(itemStack, slot, system, shooter);
                this.barrelLocation = barrelLocation;
                this.chamber = chamber;
                this.chamberSlot = chamberSlot;
            }

            public Location getBarrelLocation() { return barrelLocation; }
            public void setBarrelLocation(Location barrelLocation) { this.barrelLocation = barrelLocation; }

            public ProjectileProviderSystem getChamber() { return chamber; }
            public void setChamber(ProjectileProviderSystem chamber) { this.chamber = chamber; }

            public CalibreComponentSlot getChamberSlot() { return chamberSlot; }
            public void setChamberSlot(CalibreComponentSlot chamberSlot) { this.chamberSlot = chamberSlot; }

            @Override public void call(Listener listener) { listener.onEvent(this); }
        }
    }
}
