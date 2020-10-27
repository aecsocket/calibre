package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.projectile.CalibreProjectile;
import me.aecsocket.calibre.defaults.system.projectile.ProjectileProviderSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.component.search.SlotSearchOptions;
import me.aecsocket.calibre.item.component.search.SystemSearchOptions;
import me.aecsocket.calibre.item.component.search.SystemSearchResult;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.item.util.damagecause.DamageCause;
import me.aecsocket.calibre.item.util.slot.EquipmentItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.user.EntityItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.stat.ItemAnimationStat;
import me.aecsocket.calibre.util.stat.ParticleStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.Loop;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.BooleanStat;
import me.aecsocket.unifiedframework.stat.impl.EnumStat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.stat.impl.VectorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Location;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FireableSystem extends BaseSystem {
    public enum ChamberHandling {
        /** Only fire if there is an available chamber (closed-bolt gun).*/
        NORMAL,
        /** Fail if there is an extra chamber (double feed in an open-bolt gun). */
        FAIL,
        /** Discard any previous chamber and automatically chamber a new chamber. */
        DISCARD,
        /** Only chamber if there is none already, and continue. */
        CONDITIONAL_CHAMBER
    }

    public static final String ID = "fireable";
    public static final DamageCause GUN_DAMAGE_CAUSE = new DamageCause(){};
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("damage", new NumberStat.Double(0d))
            .init("muzzle_velocity", new NumberStat.Double(0d))
            .init("auto_chamber", new BooleanStat(true))
            .init("chamber_handling", new EnumStat<>(ChamberHandling.class, ChamberHandling.NORMAL))
            .init("barrel_offset", new VectorStat(new Vector()))
            .init("shots", new NumberStat.Int(1))
            .init("shot_delay", new NumberStat.Long(0L))

            .init("sighting_range", new NumberStat.Double(0d))
            .init("converge_range", new NumberStat.Double(0d))
            .init("dropoff", new NumberStat.Double(0d))
            .init("range", new NumberStat.Double(0d))

            .init("base_spread", new NumberStat.Double(0d))
            .init("shot_spread", new NumberStat.Double(0d))
            .init("shot_spread_recovery", new NumberStat.Double(0.995d))
            .init("projectile_spread", new NumberStat.Double(0d))

            .init("projectiles", new NumberStat.Int(1))
            .init("projectile_bounce", new NumberStat.Double(0d))
            .init("projectile_drag", new NumberStat.Double(0d))
            .init("projectile_gravity", new NumberStat.Double(Projectile.GRAVITY))
            .init("projectile_expansion", new NumberStat.Double(0d))
            .init("projectile_trail", new ParticleStat())
            .init("projectile_trail_step", new NumberStat.Double(0.5d))

            .init("max_hits", new NumberStat.Int(0))
            .init("block_penetration", new NumberStat.Double(0d))
            .init("entity_penetration", new NumberStat.Double(0d))
            .init("armor_penetration", new NumberStat.Double(0d))

            .init("chamber_priority", new NumberStat.Int(0))
            .init("ammo_priority", new NumberStat.Int(0))

            .init("fire_delay", new NumberStat.Long(0L))
            .init("fire_sound", new SoundStat())
            .init("fire_particle", new ParticleStat())
            .init("fire_animation", new ItemAnimationStat())

            .init("chamber_delay", new NumberStat.Long(0L))
            .init("chamber_sound", new SoundStat())
            .init("chamber_animation", new ItemAnimationStat())
            .get();

    public static class ProjectileData extends CalibreProjectile.Data {
        private double blockPenetration;
        private double entityPenetration;
        private double dropoff;
        private double range;
        private Events.FireOnce event; // todo change?

        public ProjectileData(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, ParticleData[] trail, double trailStep, int maxHits, ItemUser damager, double damage, DamageCause damageCause, double blockPenetration, double entityPenetration, double armorPenetration, double dropoff, double range, Events.FireOnce event) {
            super(location, velocity, bounce, drag, gravity, expansion, trail, trailStep, maxHits, damager, damage, armorPenetration, damageCause);
            this.blockPenetration = blockPenetration;
            this.entityPenetration = entityPenetration;
            this.dropoff = dropoff;
            this.range = range;
            this.event = event;
        }

        public ProjectileData(ProjectileProviderSystem.Data o) {
            super(o);
        }

        public double getBlockPenetration() { return blockPenetration; }
        public void setBlockPenetration(double blockPenetration) { this.blockPenetration = blockPenetration; }

        public double getEntityPenetration() { return entityPenetration; }
        public void setEntityPenetration(double entityPenetration) { this.entityPenetration = entityPenetration; }

        public double getDropoff() { return dropoff; }
        public void setDropoff(double dropoff) { this.dropoff = dropoff; }

        public double getRange() { return range; }
        public void setRange(double range) { this.range = range; }

        public Events.FireOnce getEvent() { return event; }
        public void setEvent(Events.FireOnce event) { this.event = event; }
    }

    @LoadTimeOnly private boolean usable;
    private transient ItemSystem itemSystem;
    private long[] shootAt;
    private double shotSpread;
    private long shotSpreadTime;

    public FireableSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    public boolean isUsable() { return usable; }
    public void setUsable(boolean usable) { this.usable = usable; }

    public double getShotSpread() { return shotSpread; }
    public void setShotSpread(double shotSpread) { this.shotSpread = shotSpread; }

    public long getShotSpreadTime() { return shotSpreadTime; }
    public void setShotSpreadTime(long shotSpreadTime) { this.shotSpreadTime = shotSpreadTime; }

    public double calculateShotSpread() {
        long time = System.currentTimeMillis();
        shotSpread *= Math.pow(stat("shot_spread_recovery"), time - shotSpreadTime);
        shotSpreadTime = time;
        return shotSpread;
    }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        itemSystem = parent.getSystemService(ItemSystem.class);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemEvents.Equip.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Interact.class, this::onEvent, 0);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    public SystemSearchResult<ProjectileProviderSystem> getChamber() {
        return Utils.atOr(parent.collectSystems(
                new SystemSearchOptions<>(ProjectileProviderSystem.class)
                        .slotTag("chamber")
                        .targetPriority(stat("chamber_priority"))
        ), 0);
    }

    public SystemSearchResult<AmmoStorageSystem> getAmmo() {
        return Utils.atOr(parent.collectSystems(
                new SystemSearchOptions<>(AmmoStorageSystem.class)
                        .slotTag("ammo")
                        .targetPriority(stat("ammo_priority"))
        ), 0);
    }

    private void onEvent(ItemEvents.Equip event) {
        if (!(event.getTickContext().getLoop() instanceof SchedulerLoop)) return;
        if (event.getSlot() instanceof EquipmentItemSlot && ((EquipmentItemSlot) event.getSlot()).getEquipmentSlot() != EquipmentSlot.HAND) return;
        while (shootAt != null && shootAt.length > 0 && System.currentTimeMillis() >= shootAt[0]) {
            long[] newShootAt = new long[shootAt.length - 1];
            System.arraycopy(shootAt, 1, newShootAt, 0, newShootAt.length);
            shootAt = newShootAt;
            if (!fireOnce(new Events.FireOnce(
                    event.getStack(),
                    event.getSlot(),
                    event.getUser(),
                    this
            ))) {
                shootAt = null;
                event.updateItem(this);
            }
        }
    }

    private void onEvent(ItemEvents.Interact event) {
        fire(new Events.Fire(
                event.getStack(),
                event.getSlot(),
                event.getUser(),
                this
        ));
    }

    public void fire(Events.Fire event) {
        if (!usable) return;
        if (!itemSystem.isAvailable()) return;
        if (callEvent(event).cancelled) return;

        int shots = stat("shots");
        long shotDelay = stat("shot_delay");
        shootAt = new long[shots];
        for (int i = 0; i < shots; i++)
            shootAt[i] = System.currentTimeMillis() + (shotDelay * i);
        event.updateItem(this);
    }

    public boolean fireOnce(Events.FireOnce event) {
        if (callEvent(event).cancelled) return false;
        ItemUser user = event.getUser();

        SystemSearchResult<AmmoStorageSystem> ammoResult = getAmmo();
        // Grab chamber
        SystemSearchResult<ProjectileProviderSystem> chamberResult;
        switch ((ChamberHandling) stat("chamber_handling")) {
            // Emulates open-bolt gun
            case FAIL:
                event.updateItem(this);
                return false;
            case DISCARD:
                chamber(new Events.PreChamber(
                        event.getStack(), event.getSlot(), event.getUser(), this
                ));
                // TODO also eject bullet if discarded
                chamberResult = getChamber();
                break;
            case CONDITIONAL_CHAMBER:
                chamberResult = getChamber();
                if (chamberResult == null) {
                    chamber(new Events.PreChamber(
                            event.getStack(), event.getSlot(), event.getUser(), this
                    ));
                    chamberResult = getChamber();
                }
                break;
            // Emulates closed-bolt gun
            default:
                chamberResult = getChamber();
        }
        if (chamberResult == null) {
            event.updateItem(this);
            return false;
        }
        CalibreComponentSlot chamberSlot = chamberResult.getSlot();
        ProjectileProviderSystem chamber = chamberResult.getSystem();

        // Get location
        Vector barrelOffset = stat("barrel_offset");
        if (user instanceof PlayerItemUser && ((PlayerItemUser) user).getEntity().getMainHand() == MainHand.LEFT)
            barrelOffset = barrelOffset.clone().setX(-barrelOffset.getX());
        Location location = Utils.getFacingRelative(user.getLocation(), barrelOffset);
        if (user instanceof EntityItemUser && Utils.isObstructed(((EntityItemUser) user).getEntity(), location)) {
            event.updateItem(this);
            return false;
        }

        double muzzleVelocity = stat("muzzle_velocity");
        double gravity = stat("projectile_gravity");

        // Sight in
        double sightingRange = stat("sighting_range");
        if (sightingRange > 0)
            location = sightIn(location, muzzleVelocity, sightingRange, 0, gravity).orElse(location);

        // Converge
        double convergeRange = stat("converge_range");
        if (convergeRange > 0) {
            Location ahead = user.getLocation().clone().add(location.getDirection().multiply(convergeRange));
            location.setDirection(ahead.subtract(location).toVector());
        }

        // Spread
        double spread = stat("base_spread");
        spread += calculateShotSpread();
        shotSpread += (double) stat("shot_spread");

        Vector velocity = location.getDirection().normalize().multiply(muzzleVelocity);
        randomRotate(velocity, spread);

        Location fLocation = location;
        for (int i = 0; i < (int) stat("projectiles"); i++) {
            Loop loop = plugin.getSchedulerLoop();
            loop.onNextTick(() -> loop.registerTickable(chamber.createProjectile(new ProjectileData(
                    fLocation,
                    randomRotate(velocity.clone(), stat("projectile_spread")),
                    stat("projectile_bounce"), stat("projectile_drag"), gravity, stat("projectile_expansion"),
                    stat("projectile_trail"), stat("projectile_trail_step"),
                    stat("max_hits"),
                    user, stat("damage"), GUN_DAMAGE_CAUSE,
                    stat("block_penetration"), stat("entity_penetration"), stat("armor_penetration"),
                    stat("dropoff"), stat("range"),
                    event
            )).inEntity(user instanceof EntityItemUser ? ((EntityItemUser) user).getEntity() : null)));
        }

        // Chamber
        chamberResult.getSlot().set(null);
        if (stat("auto_chamber")) {
            if (ammoResult != null) {
                CalibreComponent nextChamber = ammoResult.getSystem().next();
                if (chamberSlot.isCompatible(nextChamber))
                    chamberSlot.set(nextChamber);
            }
        }

        itemSystem.doAction(this, "fire", user, event.getSlot(), location);
        event.updateItem(this);
        return true;
    }

    public void chamber(Events.PreChamber event) {
        if (callEvent(event).cancelled) return;
        AmmoStorageSystem[] ammo = new AmmoStorageSystem[]{null};
        boolean[] search = new boolean[]{true};
        Map<CalibreComponentSlot, CalibreComponent> loadedChambers = new HashMap<>();
        parent.searchSlots(
                new SlotSearchOptions()
                        .slotTag("chamber")
                        .targetPriority(stat("chamber_priority")),
                slot -> {
                    if (!search[0]) return;
                    if (ammo[0] == null || !ammo[0].hasNext()) {
                        SystemSearchResult<AmmoStorageSystem> result = getAmmo();
                        if (result == null) {
                            search[0] = false;
                            return;
                        }
                        ammo[0] = result.getSystem();
                    }

                    CalibreComponent nextChamber = ammo[0].peek();
                    if (slot.isCompatible(nextChamber)) {
                        nextChamber = ammo[0].next();
                        loadedChambers.put(slot, nextChamber);
                    }
                }
        );

        Events.Chamber event2 = new Events.Chamber(
                event.getStack(), event.getSlot(), event.getUser(), this,
                loadedChambers
        );
        callEvent(event2);

        if (loadedChambers.size() > 0) {
            loadedChambers.forEach(CalibreComponentSlot::set);
            itemSystem.doAction(this, "chamber", event.getUser(), event.getSlot());
        }
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public FireableSystem clone() { return (FireableSystem) super.clone(); }
    @Override public FireableSystem copy() { return clone(); }

    /**
     * Modifies a location to be sighted in to shoot at a particular position along the plane of the view model.
     * @param location The original position. WIll be copied.
     * @param v The speed of the projectile.
     * @param x The distance between the final point and the origin, in meters.
     * @param y The height between the final point and the origin, in meters.
     * @param g The gravity in this space, in m/s.
     * @return The sighted in position. If the point is unable to be reached, an empty Optional is returned.
     */
    public static Optional<Location> sightIn(Location location, double v /* speed */, double x /* dist */, double y /* height */, double g /* gravity */) {
        location = location.clone();
        double theta = (float)(
                Math.atan((Math.pow(v, 2) - Math.sqrt(Math.pow(v, 4) - (g * (g*Math.pow(x, 2) + 2*y*Math.pow(v, 2))))) / (g*x))
        );
        if (!Double.isFinite(theta))
            return Optional.empty();
        location.setPitch(location.getPitch() + (float)-Math.toDegrees(theta));
        return Optional.of(location);
    }

    public static double randomSpread(double spread) { return Math.toRadians(ThreadLocalRandom.current().nextGaussian() * spread); }

    public static Vector randomRotate(Vector direction, double spread) {
        direction.rotateAroundX(randomSpread(spread));
        direction.rotateAroundY(randomSpread(spread));
        direction.rotateAroundZ(randomSpread(spread));
        return direction;
    }

    public static final class Events {
        private Events() {}

        public static class Event extends ItemEvents.SystemEvent<FireableSystem> {
            public Event(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }
        }

        public static class Fire extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public Fire(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }


        public static class FireOnce extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public FireOnce(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static class PreChamber extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public PreChamber(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static class Chamber extends Event {
            private final Map<CalibreComponentSlot, CalibreComponent> loadedChambers;

            public Chamber(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system, Map<CalibreComponentSlot, CalibreComponent> loadedChambers) {
                super(stack, slot, user, system);
                this.loadedChambers = loadedChambers;
            }

            public Map<CalibreComponentSlot, CalibreComponent> getLoadedChambers() { return loadedChambers; }
        }
    }
}
