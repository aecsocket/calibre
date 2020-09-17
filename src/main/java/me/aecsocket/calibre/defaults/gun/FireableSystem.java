package me.aecsocket.calibre.defaults.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ActionSystem;
import me.aecsocket.calibre.defaults.service.system.MainSystem;
import me.aecsocket.calibre.defaults.service.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemSearchOptions;
import me.aecsocket.calibre.item.system.SystemSearchResult;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.calibre.util.itemuser.EntityItemUser;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.BooleanStat;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.VectorStat;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.*;
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
            .init("ammo_priority", new NumberStat.Int(0))
            .init("auto_chamber", new BooleanStat(true))
            .init("damage", new NumberStat.Double(0d))

            .init("projectiles", new NumberStat.Int(1))
            .init("projectile_velocity", new NumberStat.Double(0d))
            .init("projectile_bounce", new NumberStat.Double(0d))
            .init("projectile_drag", new NumberStat.Double(0d))
            .init("projectile_gravity", new NumberStat.Double(Projectile.GRAVITY))
            .init("projectile_expansion", new NumberStat.Double(0d))
            .init("projectile_trail", new DataStat.Particle())
            .init("projectile_trail_step_size", new NumberStat.Double(0.5))

            .init("fire_delay", new NumberStat.Long(1L))
            .init("fire_sound", new DataStat.Sound())
            .init("fire_particles", new DataStat.Particle())
            .init("fire_animation", new AnimationStat())

            .init("chamber_delay", new NumberStat.Long(1L))
            .init("chamber_sound", new DataStat.Sound())
            .init("chamber_particles", new DataStat.Particle())
            .init("chamber_animation", new AnimationStat())

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

    public Location getBarrelLocation(ItemUser user) {
        Location location = Utils.getFacingRelative(user.getBasePosition(), stat("barrel_offset"));
        if (user instanceof EntityItemUser)
            return Utils.isObstructed(((EntityItemUser) user).getEntity(), location) ? null : location;
        else
            return location;
    }

    public void fire(Events.PreFire<?> event) {
        if (callEvent(event).cancelled) return;

        ItemUser shooter = event.shooter;
        Location barrelLocation = getBarrelLocation(shooter);

        if (barrelLocation == null) return;

        List<SystemSearchResult<ProjectileProviderSystem>> chambers = new ArrayList<>();
        searchSystems(
                SystemSearchOptions.of("chamber", stat("chamber_priority"), ProjectileProviderSystem.class),
                chambers::add
        );
        if (chambers.size() == 0) {
            chamber(new Events.PreChamber<>(
                    event.getItemStack(), event.getSlot(), event.system, event.shooter,
                    barrelLocation
            ));
            return;
        }

        // Uses 1 chamber at a time
        SystemSearchResult<ProjectileProviderSystem> chamber = chambers.get(0);
        ProjectileProviderSystem chamberSystem = chamber.getSystem();
        CalibreComponentSlot chamberSlot = chamber.getSlot();

        Events.Fire<?> event2 = new Events.Fire<>(
                event.getItemStack(), event.getSlot(), event.system, event.shooter,
                barrelLocation.clone(), chamberSystem, chamberSlot
        );
        if (callEvent(event2).isCancelled()) return;

        barrelLocation = event2.barrelLocation;
        chamberSystem = event2.chamber;
        chamberSlot = event2.chamberSlot;
        chamberSlot.set(null);

        for (int i = 0; i < (int) stat("projectiles"); i++) {
            Projectile projectile = chamberSystem.create(
                    new ProjectileData(
                            barrelLocation, barrelLocation.getDirection().multiply((double) stat("projectile_velocity")),
                            stat("projectile_bounce"), stat("projectile_drag"), stat("projectile_gravity"), stat("projectile_expansion"),
                            stat("projectile_trail"), stat("projectile_trail_step_size"),
                            shooter, stat("damage"), event.getItemStack(), this
                    )
            ).inEntity(shooter instanceof EntityItemUser ? ((EntityItemUser) shooter).getEntity() : null);
            plugin.getSchedulerLoop().registerTickable(projectile);
        }

        if (stat("auto_chamber")) {
            CalibreComponentSlot fChamberSlot = chamberSlot;
            searchSystems(
                    SystemSearchOptions.of("ammo", stat("ammo_priority"), AmmoProviderSystem.class),
                    result -> {
                        CalibreComponent peek = result.getSystem().peek();
                        if (fChamberSlot.isCompatible(peek)) {
                            fChamberSlot.set(result.getSystem().pop());
                            return true;
                        }
                        return false;
                    }
            );
        }

        actionSystem.startAction(
                stat("fire_delay"),
                barrelLocation, stat("fire_sound"), stat("fire_particles"),
                shooter, event.getSlot(), stat("fire_animation"));

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

    public void chamber(Events.PreChamber<?> event) {
        if (callEvent(event).isCancelled()) return;

        ItemUser shooter = event.getShooter();
        Location location = event.barrelLocation;

        // Chambers all chamber slots if they are empty
        // For each empty chamber slot, finds an ammo provider which has a component compatible with the slot.
        // If at least one chamber is loaded, this is considered successful
        boolean[] success = {false};

        parent.walk(data -> {
            if (!(data.getSlot() instanceof CalibreComponentSlot)) return;
            CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
            if (slot.get() != null) return;
            if (!slot.getTags().contains("chamber")) return;
            if (slot.getPriority() != (int) stat("chamber_priority")) return;

            AmmoProviderSystem ammo = findAmmoSystem(slot);
            if (ammo == null) return;
            slot.set(ammo.pop());
            success[0] = true;
        });

        if (success[0])
            actionSystem.startAction(
                    stat("chamber_delay"),
                    location, stat("chamber_sound"), stat("chamber_particles"),
                    shooter, event.getSlot(), stat("chamber_animation"));
        else
            actionSystem.startAction(
                    stat("empty_delay"),
                    location, stat("empty_sound"), stat("empty_particles"),
                    shooter, event.getSlot(), stat("empty_animation"));
    }

    private AmmoProviderSystem findAmmoSystem(CalibreComponentSlot slot) {
        AmmoProviderSystem[] ammo = {null};
        searchSystems(SystemSearchOptions.of("ammo", stat("ammo_priority"), AmmoProviderSystem.class), result -> {
            AmmoProviderSystem system = result.getSystem();
            CalibreComponent next = system.peek();
            if (next == null) return false;
            if (!slot.isCompatible(next)) return false;
            ammo[0] = system;
            return false;
        });
        return ammo[0];
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
                event.getUser()
        ));
    }

    @Override public FireableSystem clone() { try { return (FireableSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public CalibreSystem<Void> copy() { return clone(); }

    public static class ProjectileData extends ProjectileProviderSystem.Data {
        private final FireableSystem system;

        public ProjectileData(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, ParticleData[] trail, double trailStepSize, ItemUser shooter, double damage, ItemStack item, FireableSystem system) {
            super(location, velocity, bounce, drag, gravity, expansion, trail, trailStepSize, shooter, damage, item);
            this.system = system;
        }

        public FireableSystem getSystem() { return system; }
    }

    public static final class Events {
        private Events() {}

        public static class PreFire<L extends PreFire.Listener> extends ItemEvents.Event<L> implements ItemEvents.SystemEvent<FireableSystem>, Cancellable {
            public interface Listener { void onEvent(PreFire<?> event); }

            private final FireableSystem system;
            private final ItemUser shooter;
            private boolean cancelled;

            public PreFire(ItemStack itemStack, EquipmentSlot slot, FireableSystem system, ItemUser shooter) {
                super(itemStack, slot);
                this.system = system;
                this.shooter = shooter;
            }

            @Override public FireableSystem getSystem() { return system; }
            public ItemUser getShooter() { return shooter; }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            @Override public void call(Listener listener) { listener.onEvent(this); }
        }

        public static class PreChamber<L extends PreChamber.Listener> extends PreFire<L> {
            public interface Listener extends PreFire.Listener { void onEvent(PreChamber<?> event); }

            private Location barrelLocation;

            public PreChamber(ItemStack itemStack, EquipmentSlot slot, FireableSystem system, ItemUser shooter, Location barrelLocation) {
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

            public Fire(ItemStack itemStack, EquipmentSlot slot, FireableSystem system, ItemUser shooter, Location barrelLocation, ProjectileProviderSystem chamber, CalibreComponentSlot chamberSlot) {
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
