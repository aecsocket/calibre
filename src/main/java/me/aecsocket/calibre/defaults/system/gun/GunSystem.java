package me.aecsocket.calibre.defaults.system.gun;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.CalibreComponentSupplier;
import me.aecsocket.calibre.defaults.service.CalibreSwayStabilization;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.gun.ammo.AmmoStorageSystem;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireMode;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireModeReference;
import me.aecsocket.calibre.defaults.system.gun.firemode.FireModeSystem;
import me.aecsocket.calibre.defaults.system.gun.projectile.BulletSystem;
import me.aecsocket.calibre.defaults.system.gun.projectile.GunProjectileProviderSystem;
import me.aecsocket.calibre.defaults.system.gun.sight.Sight;
import me.aecsocket.calibre.defaults.system.gun.sight.SightReference;
import me.aecsocket.calibre.defaults.system.gun.sight.SightSystem;
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
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.item.util.damagecause.DamageCause;
import me.aecsocket.calibre.item.util.slot.EquipmentItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.user.EntityItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.LivingEntityItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.CalibreParticleData;
import me.aecsocket.calibre.item.util.LoadTimeDependencies;
import me.aecsocket.calibre.util.OrderedStatMap;
import me.aecsocket.calibre.util.stat.ItemAnimationStat;
import me.aecsocket.calibre.util.stat.ParticleStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.Loop;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.*;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.Vector2;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GunSystem extends BaseSystem {
    /** Handles how to fire a gun if there is already a chamber loaded before firing. */
    public enum ChamberHandling {
        /** Only fire if there is an available chamber (closed-bolt gun). */
        NORMAL,
        /** Fail if there is an extra chamber (double feed in an open-bolt gun). */
        FAIL,
        /** Discard any previous chamber and automatically chamber a new chamber. */
        DISCARD,
        /** Only chamber if there is none already, and continue. */
        CONDITIONAL_CHAMBER
    }

    /** Determines why a gun failed to perform an action. */
    public static final class FailReason {
        private FailReason() {}

        /** If the gun had no loaded chamber available. */
        public static final int EMPTY = 0;
        /**
         * If the gun attempted to load a chamber when there was already one present.
         * @see ChamberHandling#FAIL
         */
        public static final int DOUBLE_FEED = 1;
    }

    public static class ProjectileData extends CalibreProjectile.Data {
        private double blockPenetration;
        private double entityPenetration;
        private double dropoff;
        private double range;
        private Set<Material> breakableBlocks;
        private Events.FireOnce event; // todo change?

        public ProjectileData(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, CalibreParticleData[] trail, CalibreParticleData[] hit, double trailStep, int maxHits, ItemUser damager, double damage, DamageCause damageCause, double blockPenetration, double entityPenetration, double armorPenetration, double dropoff, double range, Set<Material> breakableBlocks, Events.FireOnce event) {
            super(location, velocity, bounce, drag, gravity, expansion, trail, hit, trailStep, maxHits, damager, damage, armorPenetration, damageCause);
            this.blockPenetration = blockPenetration;
            this.entityPenetration = entityPenetration;
            this.dropoff = dropoff;
            this.range = range;
            this.breakableBlocks = breakableBlocks;
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

        public Set<Material> getBreakableBlocks() { return breakableBlocks; }
        public void setBreakableBlocks(Set<Material> breakableBlocks) { this.breakableBlocks = breakableBlocks; }

        public Events.FireOnce getEvent() { return event; }
        public void setEvent(Events.FireOnce event) { this.event = event; }
    }

    public static final String ID = "gun";
    public static final int ITEM_DESPAWN_TIME = 20 * 60 * 5;
    public static final DamageCause GUN_DAMAGE_CAUSE = new DamageCause(){};
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("auto_chamber", new BooleanStat(true))
            .init("can_fire_underwater", new BooleanStat(false))
            .init("chamber_handling", new EnumStat<>(ChamberHandling.class, ChamberHandling.NORMAL))
            .init("barrel_offset", new VectorStat(new Vector()))
            .init("shots", new NumberStat.Int(1))
            .init("shot_start_delay", new NumberStat.Long(0L))
            .init("shot_delay", new NumberStat.Long(0L))
            .init("entity_awareness", new NumberStat.Double(0d))
            .init("breakable_blocks", new BasicArrayStat<>(Material[].class, Material[]::new))
            .init("ejection_port_offset", new VectorStat(new Vector()))
            .init("ejection_port_velocity", new VectorStat(new Vector()))
            .init("ejection_lifetime", new NumberStat.Long(0L))

            .init("sighting_range", new NumberStat.Double(0d))
            .init("converge_range", new NumberStat.Double(0d))
            .init("dropoff", new NumberStat.Double(0d))
            .init("range", new NumberStat.Double(0d))

            .init("base_spread", new NumberStat.Double(0d))
            .init("shot_spread", new NumberStat.Double(0d))
            .init("shot_spread_recovery", new NumberStat.Double(0.995d))
            .init("projectile_spread", new NumberStat.Double(0d))

            .init("recoil", new Vector2Stat(new Vector2()))
            .init("recoil_speed", new NumberStat.Double(1d))
            .init("recoil_recovery", new NumberStat.Double(1d))
            .init("recoil_recovery_after", new NumberStat.Long(0L))
            .init("recoil_recovery_speed", new NumberStat.Double(1d))

            .init("sway", new Vector2Stat(new Vector2()))
            .init("sway_stabilize_multiplier", new NumberStat.Double(0d))
            .init("sway_spread_multiplier", new NumberStat.Double(0d))

            .init("damage", new NumberStat.Double(0d))
            .init("muzzle_velocity", new NumberStat.Double(0d))
            .init("projectiles", new NumberStat.Int(1))
            .init("projectile_bounce", new NumberStat.Double(0d))
            .init("projectile_drag", new NumberStat.Double(0d))
            .init("projectile_gravity", new NumberStat.Double(Projectile.GRAVITY))
            .init("projectile_expansion", new NumberStat.Double(0d))
            .init("projectile_trail", new ParticleStat())
            .init("projectile_hit_particle", new ParticleStat())
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
            .init("chamber_after", new NumberStat.Long(0L))
            .init("chamber_sound", new SoundStat())
            .init("chamber_animation", new ItemAnimationStat())

            .init("aim_in_delay", new NumberStat.Long(0L))
            .init("aim_in_after", new NumberStat.Long(0L))
            .init("aim_in_sound", new SoundStat())
            .init("aim_in_animation", new ItemAnimationStat())

            .init("aim_out_delay", new NumberStat.Long(0L))
            .init("aim_out_after", new NumberStat.Long(0L))
            .init("aim_out_sound", new SoundStat())
            .init("aim_out_animation", new ItemAnimationStat())

            .init("change_sight_delay", new NumberStat.Long(0L))
            .init("change_sight_after", new NumberStat.Long(0L))
            .init("change_sight_sound", new SoundStat())
            .init("change_sight_animation", new ItemAnimationStat())

            .init("change_fire_mode_delay", new NumberStat.Long(0L))
            .init("change_fire_mode_after", new NumberStat.Long(0L))
            .init("change_fire_mode_sound", new SoundStat())
            .init("change_fire_mode_animation", new ItemAnimationStat())

            .init("load_ammo_delay", new NumberStat.Long(0L))
            .init("load_ammo_sound", new SoundStat())
            .init("load_ammo_animation", new ItemAnimationStat())

            .init("load_chamber_delay", new NumberStat.Long(0L))
            .init("load_chamber_sound", new SoundStat())
            .init("load_chamber_animation", new ItemAnimationStat())

            .init("fail_delay", new NumberStat.Long(0L))
            .init("fail_sound", new SoundStat())
            .init("fail_animation", new ItemAnimationStat())
            .get();

    /**
     * Temporarily stores info on deserialization for resolution later.
     */
    protected static class Dependencies {
        private JsonObject aimingStats;
        private JsonObject notAimingStats;
        private JsonObject noSightStats;
        private JsonObject noFireModeStats;
    }

    @LoadTimeOnly private boolean usable;
    @LoadTimeOnly private boolean showActionBar;
    @LoadTimeDependencies private transient Dependencies dependencies;
    @LoadTimeOnly private transient OrderedStatMap aimingStats;
    @LoadTimeOnly private transient OrderedStatMap notAimingStats;
    @LoadTimeOnly private transient OrderedStatMap noSightStats;
    @LoadTimeOnly private transient OrderedStatMap noFireModeStats;
    private transient ItemSystem itemSystem;
    private double shotSpread;
    private long shotSpreadTime;

    private SightReference sight;
    private boolean aiming;
    private FireModeReference fireMode;

    public GunSystem(CalibrePlugin plugin) {
        super(plugin);
    }
    public GunSystem() { this(null); }

    public boolean isUsable() { return usable; }
    public void setUsable(boolean usable) { this.usable = usable; }

    public OrderedStatMap getAimingStats() { return aimingStats; }
    public void setAimingStats(OrderedStatMap aimingStats) { this.aimingStats = aimingStats; }

    public OrderedStatMap getNotAimingStats() { return notAimingStats; }
    public void setNotAimingStats(OrderedStatMap notAimingStats) { this.notAimingStats = notAimingStats; }

    public OrderedStatMap getNoSightStats() { return noSightStats; }
    public void setNoSightStats(OrderedStatMap noSightStats) { this.noSightStats = noSightStats; }

    public OrderedStatMap getNoFireModeStats() { return noFireModeStats; }
    public void setNoFireModeStats(OrderedStatMap noFireModeStats) { this.noFireModeStats = noFireModeStats; }

    public double getShotSpread() { return shotSpread; }
    public void setShotSpread(double shotSpread) { this.shotSpread = shotSpread; }

    public long getShotSpreadTime() { return shotSpreadTime; }
    public void setShotSpreadTime(long shotSpreadTime) { this.shotSpreadTime = shotSpreadTime; }

    public SightReference getSight() { return sight; }
    public void setSight(SightReference sight) { this.sight = sight; }

    public boolean isAiming() { return aiming; }
    public void setAiming(boolean aiming) { this.aiming = aiming; }

    public FireModeReference getFireMode() { return fireMode; }
    public void setFireMode(FireModeReference fireMode) { this.fireMode = fireMode; }

    @Override
    public void systemInitialize(CalibreComponent parent) throws SystemInitializationException {
        if (dependencies.aimingStats != null)
            aimingStats = plugin.getGson().fromJson(dependencies.aimingStats, new TypeToken<OrderedStatMap>(){}.getType());
        if (dependencies.notAimingStats != null)
            notAimingStats = plugin.getGson().fromJson(dependencies.notAimingStats, new TypeToken<OrderedStatMap>(){}.getType());
        if (dependencies.noSightStats != null)
            noSightStats = plugin.getGson().fromJson(dependencies.noSightStats, new TypeToken<OrderedStatMap>(){}.getType());
        if (dependencies.noFireModeStats != null)
            noFireModeStats = plugin.getGson().fromJson(dependencies.noFireModeStats, new TypeToken<OrderedStatMap>(){}.getType());
    }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
        if (!usable) return;

        itemSystem = parent.getService(ItemSystem.class);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemEvents.Equip.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Holster.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Interact.class, this::onEvent, 0);
        events.registerListener(ItemEvents.BukkitInteract.class, this::onEvent, 0);
        events.registerListener(ItemEvents.SwapHand.class, this::onEvent, 0);
        events.registerListener(ItemEvents.BukkitSwapHand.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Drop.class, this::onEvent, 0);
        events.registerListener(ItemEvents.BukkitDrop.class, this::onEvent, 0);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }
    @Override public Collection<String> getDependencies() { return Collections.singleton(ItemSystem.ID); }

    public boolean available() {
        return usable
                && itemSystem.isAvailable()
                && getTree().isComplete();
    }

    public double calculateShotSpread() {
        long time = System.currentTimeMillis();
        shotSpread *= Math.pow(stat("shot_spread_recovery"), (time - shotSpreadTime) / 1000d);
        shotSpreadTime = time;
        return shotSpread;
    }

    @Override
    public OrderedStatMap buildStats() {
        OrderedStatMap stats = new OrderedStatMap();

        FireMode fireMode = getMappedFireMode();
        if (fireMode == null) {
            if (noFireModeStats != null)
                stats.combine(noFireModeStats);
        } else {
            if (fireMode.getStats() != null)
                stats.combine(fireMode.getStats());
        }

        if (aiming && aimingStats != null)
            stats.combine(aimingStats);
        else if (!aiming && notAimingStats != null)
            stats.combine(notAimingStats);

        Sight sight = getMappedSight();
        if (sight == null) {
            if (noSightStats != null)
                stats.combine(noSightStats);
        } else {
            if (!aiming && sight.getInactiveStats() != null)
                stats.combine(sight.getInactiveStats());
            if (aiming && sight.getActiveStats() != null)
                stats.combine(sight.getActiveStats());
        }

        return stats;
    }

    public SlotSearchOptions chamberSlotSearch() {
        return new SlotSearchOptions()
                .slotTag("chamber")
                .targetPriority(stat("chamber_priority"));
    }
    public SystemSearchOptions<ProjectileProviderSystem> chamberSystemSearch() {
        return new SystemSearchOptions<ProjectileProviderSystem>(chamberSlotSearch())
                .serviceType(ProjectileProviderSystem.class);
    }
    public SystemSearchResult<ProjectileProviderSystem> collectChamber() { return parent.firstOf(chamberSystemSearch()); }

    public SlotSearchOptions ammoSlotSearch() {
        return new SlotSearchOptions()
                .slotTag("ammo")
                .targetPriority(stat("ammo_priority"));
    }
    public SystemSearchOptions<AmmoStorageSystem> ammoSystemSearch() {
        return new SystemSearchOptions<AmmoStorageSystem>(ammoSlotSearch())
                .serviceType(AmmoStorageSystem.class);
    }
    public SystemSearchResult<AmmoStorageSystem> collectAmmo() { return parent.firstOf(ammoSystemSearch(), (slot, sys) -> sys.hasNext()); }

    // TODO clean this up

    //region Sights

    private void collectSights(CalibreComponent component, List<SightReference> result, String path) {
        SightSystem sys = component.getSystem(SightSystem.class);
        if (sys != null) {
            for (int i = 0; i < sys.getSights().size(); i++)
                result.add(new SightReference(path, i));
        }
    }

    public List<SightReference> collectSights() {
        List<SightReference> result = new ArrayList<>();
        collectSights(parent, result, null);
        parent.walk(data -> {
            data.getComponent().ifPresent(raw -> {
                if (!(raw instanceof CalibreComponent)) return;
                CalibreComponent comp = (CalibreComponent) raw;
                collectSights(comp, result, data.getJoinedPath());
            });
        });
        return result;
    }
    public SightReference getSight(int index) {
        List<SightReference> sights = collectSights();
        return sights.size() == 0 ? null : sights.get(Math.max(0, index % sights.size()));
    }

    public Sight getMappedSight() { return sight == null ? null : sight.getMapped(parent); }
    public Sight getMappedSight(int index) {
        SightReference ref = getSight(index);
        return ref == null ? null : ref.getMapped(parent);
    }

    public int getSightIndex() { return collectSights().indexOf(sight); }

    //endregion

    //region Fire modes

    private void collectFireModes(CalibreComponent component, List<FireModeReference> result, String path) {
        FireModeSystem sys = component.getSystem(FireModeSystem.class);
        if (sys != null) {
            for (int i = 0; i < sys.getModes().size(); i++)
                result.add(new FireModeReference(path, i));
        }
    }

    public List<FireModeReference> collectFireModes() {
        List<FireModeReference> result = new ArrayList<>();
        collectFireModes(parent, result, null);
        parent.walk(data -> {
            data.getComponent().ifPresent(raw -> {
                if (!(raw instanceof CalibreComponent)) return;
                CalibreComponent comp = (CalibreComponent) raw;
                collectFireModes(comp, result, data.getJoinedPath());
            });
        });
        return result;
    }
    public FireModeReference getFireMode(int index) {
        List<FireModeReference> fireModes = collectFireModes();
        return fireModes.size() == 0 ? null : fireModes.get(Math.max(0, index % fireModes.size()));
    }

    public FireMode getMappedFireMode() { return fireMode == null ? null : fireMode.getMapped(parent); }
    public FireMode getMappedFireMode(int index) {
        FireModeReference ref = getFireMode(index);
        return ref == null ? null : ref.getMapped(parent);
    }

    public int getFireModeIndex() { return collectFireModes().indexOf(fireMode); }

    //endregion

    private void onEvent(ItemEvents.Equip event) {
        if (!getTree().isComplete()) return;
        if (!(event.getTickContext().getLoop() instanceof SchedulerLoop)) {
            if (event.getUser() instanceof ShooterItemUser) {
                ShooterItemUser user = (ShooterItemUser) event.getUser();
                Vector2 sway = stat("sway");
                sway.multiply(1 + (calculateShotSpread() * (double) stat("sway_spread_multiplier"))); // todo * (((data.getSpread() / base.stat("spread", double.class)) + 1) * base.stat("spread_sway_multiplier", double.class));
                if (sway.manhattanLength() > 0) {
                    double angle = Math.toRadians(event.getTickContext().getLoop().getTicks());
                    Vector2 rotation = new Vector2(
                            Math.cos(angle) * sway.getX(),
                            Math.sin(angle) * sway.getY()
                    );
                    Utils.useService(CalibreSwayStabilization.class, s -> {
                        if (s.stabilize(event.getUser(), rotation, event.getTickContext()))
                            rotation.multiply((double) stat("sway_stabilize_multiplier"));
                    });

                    if (rotation.manhattanLength() > 0)
                        user.rotateCamera((float) rotation.getX(), (float) rotation.getY());
                }
            }
        }
        if (event.getSlot() instanceof EquipmentItemSlot && ((EquipmentItemSlot) event.getSlot()).getEquipmentSlot() != EquipmentSlot.HAND) return;

        boolean update = false;
        if (fireMode == null || getMappedFireMode() == null) {
            fireMode = Utils.atOr(collectFireModes(), 0);
            update = true;
        }
        if (sight == null || getMappedSight() == null) {
            sight = Utils.atOr(collectSights(), 0);
            update = true;
        }
        if (update)
            event.updateItem(this);

        if (event.getUser() instanceof PlayerItemUser) {
            Player player = ((PlayerItemUser) event.getUser()).getEntity();
            // TODO ac bar
            if (showActionBar) {
                player.sendActionBar(
                        parent.collectSystems(ammoSystemSearch()).stream()
                                .map(result -> {
                                    AmmoStorageSystem sys = result.getSystem();
                                    String icon = sys.getIcon();
                                    String emptyIcon = sys.getEmptyIcon();
                                    return sys.getComponents().stream()
                                            .map(q -> {
                                                CalibreComponent comp = q.get();
                                                BulletSystem bullet = comp.getSystem(BulletSystem.class);
                                                return bullet == null ? null : (bullet.getPrefix() + icon).repeat(q.getAmount());
                                            })
                                            .collect(Collectors.joining())
                                            + emptyIcon.repeat(Math.max(0, sys.getCapacity() - sys.size()));
                                })
                                .collect(Collectors.joining(ChatColor.RESET + " "))
                                + ChatColor.GRAY + " [" +
                                parent.collectSystems(chamberSystemSearch()).stream()
                                        .filter(result -> result.getSystem() instanceof GunProjectileProviderSystem)
                                        .map(result -> {
                                            GunProjectileProviderSystem sys = (GunProjectileProviderSystem) result.getSystem();
                                            return sys.getPrefix() + sys.getIcon();
                                        })
                                        .collect(Collectors.joining())
                                + ChatColor.GRAY + "] " + ChatColor.YELLOW +
                                (getMappedFireMode() == null ? "-none-" : getMappedFireMode().getName())
                                + ChatColor.GOLD + " " +
                                (getMappedSight() == null ? "-none-" : getMappedSight().getName())
                                + ChatColor.DARK_AQUA + " " +
                                String.format("%.0f", (double) stat("sighting_range")) + "m"
                                + ChatColor.GRAY + " " +
                                String.format("%.3f", calculateShotSpread())
                );
            }
            /*
            if (actionBar != null) {
                player.sendActionBar(format(actionBar,
                        "count_ammo", getAllAmmo().stream()
                                .map(result -> format(actionBarAmmo,
                                        "size", result.getSystem().size(),
                                        "capacity", result.getSystem().getCapacity()))
                                .collect(Collectors.joining(actionBarAmmoSeparator)),
                        "bar_ammo", getAllAmmo().stream()
                                .map(result -> )
                        "count_chamber", getChambers().size(),
                        "sighting_range", stat("sighting_range")
                ));
            }*/
        }
    }

    private void onEvent(ItemEvents.Holster event) {
        //shootAt = null;
        aiming = false;
        event.updateItem(this);
    }

    private void onEvent(ItemEvents.Interact event) {
        if (!available()) return;
        if (Utils.isRightClick(event.getAction())) {
            if (stat("chamber_handling") == ChamberHandling.NORMAL && collectChamber() == null)
                chamber(new Events.PreChamber(
                        event.getStack(), event.getSlot(), event.getUser(), this
                ));
            else
                fire(new Events.Fire(
                        event.getStack(), event.getSlot(), event.getUser(), this
                ));
        } else {
            if (
                    aiming
                    && event.getUser() instanceof PlayerItemUser
                    && ((PlayerItemUser) event.getUser()).getEntity().isSneaking()
                    && collectSights().size() > 1
            ) {
                changeSight(new Events.CycleSight(
                        event.getStack(), event.getSlot(), event.getUser(), this
                ));
            } else {
                toggleAiming(new Events.ToggleAiming(
                        event.getStack(), event.getSlot(), event.getUser(), this, !aiming
                ));
            }
        }
    }

    private void onEvent(ItemEvents.BukkitInteract event) {
        if (!usable) return;
        event.getEvent().setCancelled(true);
    }

    private void onEvent(ItemEvents.SwapHand event) {
        if (!available()) return;
        changeFireMode(new Events.CycleFireMode(
                event.getStack(), event.getSlot(), event.getUser(), this
        ));
    }

    private void onEvent(ItemEvents.BukkitSwapHand event) {
        if (event.getSlot() instanceof EquipmentItemSlot) {
            EquipmentSlot slot = ((EquipmentItemSlot) event.getSlot()).getEquipmentSlot();
            if (slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND)
                event.getEvent().setCancelled(true);
        }
    }

    private void onEvent(ItemEvents.Drop event) {
        if (!available()) return;
        reload(new Events.PreReload(
                event.getStack(), event.getSlot(), event.getUser(), this
        ));
    }

    private void onEvent(ItemEvents.BukkitDrop event) {
        event.getEvent().setCancelled(true);
    }

    public void fire(Events.Fire event) {
        if (callEvent(event).cancelled) return;

        int shots = stat("shots");
        long shotDelay = stat("shot_delay");
        long shotStartDelay = stat("shot_start_delay");
        for (int i = 0; i < shots; i++)
            itemSystem.addTask(this, event.getSlot(), sys -> {
                if (!fireOnce(new Events.FireOnce(
                        event.getStack(),
                        event.getSlot(),
                        event.getUser(),
                        this
                ))) {
                    itemSystem.cancelTasks();
                    event.updateItem(this);
                }
            }, shotStartDelay + (shotDelay * i));
        event.updateItem();
    }

    public boolean fireOnce(Events.FireOnce event) {
        if (callEvent(event).cancelled) return false;
        ItemUser user = event.getUser();

        // Get location
        Vector barrelOffset = stat("barrel_offset");
        if (user instanceof PlayerItemUser && ((PlayerItemUser) user).getEntity().getMainHand() == MainHand.LEFT)
            barrelOffset = barrelOffset.clone().setX(-barrelOffset.getX());
        Location location = Utils.getFacingRelative(user.getLocation(), barrelOffset);
        if (!(boolean) stat("can_fire_underwater")) {
            Material type = location.getBlock().getType();
            if (type == Material.WATER || type == Material.LAVA)
                return false;
        }
        if (user instanceof EntityItemUser && Utils.isObstructed(((EntityItemUser) user).getEntity(), location))
            return false;

        SystemSearchResult<AmmoStorageSystem> ammoResult = collectAmmo();
        // Grab chamber
        SystemSearchResult<ProjectileProviderSystem> chamberResult;
        switch ((ChamberHandling) stat("chamber_handling")) {
            // Emulates open-bolt gun
            case FAIL:
                fail(new Events.Fail(
                        event.getStack(), event.getSlot(), event.getUser(), this, FailReason.DOUBLE_FEED
                ));
                event.updateItem();
                return false;
            case DISCARD:
                chamber(new Events.PreChamber(
                        event.getStack(), event.getSlot(), event.getUser(), this
                ));
                chamberResult = collectChamber();
                break;
            case CONDITIONAL_CHAMBER:
                chamberResult = collectChamber();
                if (chamberResult == null) {
                    chamber(new Events.PreChamber(
                            event.getStack(), event.getSlot(), event.getUser(), this
                    ));
                    chamberResult = collectChamber();
                }
                break;
            // Emulates closed-bolt gun
            default:
                chamberResult = collectChamber();
        }
        if (chamberResult == null)
            return false;
        CalibreComponentSlot chamberSlot = chamberResult.getSlot();
        ProjectileProviderSystem chamber = chamberResult.getSystem();

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

        Location fLocation = location.clone();
        for (int i = 0; i < (int) stat("projectiles"); i++) {
            Loop loop = plugin.getSchedulerLoop();
            loop.onNextTick(() -> loop.registerTickable(chamber.createProjectile(new ProjectileData(
                    fLocation,
                    randomRotate(velocity.clone(), stat("projectile_spread")),
                    stat("projectile_bounce"), stat("projectile_drag"), gravity, stat("projectile_expansion"),
                    stat("projectile_trail"), stat("projectile_hit_particle"), stat("projectile_trail_step"),
                    stat("max_hits"),
                    user, stat("damage"), GUN_DAMAGE_CAUSE,
                    stat("block_penetration"), stat("entity_penetration"), stat("armor_penetration"),
                    stat("dropoff"), stat("range"),
                    stat("breakable_blocks") == null ? null : new HashSet<>(Arrays.asList(stat("breakable_blocks"))),
                    event
            )).inEntity(user instanceof EntityItemUser ? ((EntityItemUser) user).getEntity() : null)));
        }

        // Recoil
        if (user instanceof ShooterItemUser)
            ((ShooterItemUser) user).applyRecoil(
                    stat("recoil"), stat("recoil_speed"),
                    stat("recoil_recovery"), stat("recoil_recovery_after"), stat("recoil_recovery_speed")
            );

        // Chamber
        if (user instanceof LivingEntityItemUser)
            ejectChamber(chamber, ((LivingEntityItemUser) user).getEntity());
        chamberResult.getSlot().set(null);
        if (stat("auto_chamber")) {
            if (ammoResult != null) {
                CalibreComponent nextChamber = ammoResult.getSystem().next();
                if (chamberSlot.isCompatible(nextChamber))
                    chamberSlot.set(nextChamber);
            }
        }

        // Entity awareness
        if (user instanceof LivingEntityItemUser) {
            LivingEntity eUser = ((LivingEntityItemUser) user).getEntity();
            double entityAwareness = stat("entity_awareness");
            if (entityAwareness > 0 && (!(user instanceof PlayerItemUser) || ((PlayerItemUser) user).getEntity().getGameMode() != GameMode.CREATIVE)) {
                location.getNearbyLivingEntities(entityAwareness).forEach(entity -> {
                    if (entity.getType() != eUser.getType() && entity instanceof Monster && ((Monster) entity).getTarget() == null)
                        ((Monster) entity).setTarget(eUser);
                });
            }
        }

        itemSystem.doAction(this, "fire", user, event.getSlot(), location.subtract(user.getLocation()).toVector());
        event.updateItem();
        return true;
    }

    public void chamber(Events.PreChamber event) {
        if (callEvent(event).cancelled) return;
        ItemUser user = event.getUser();

        AmmoStorageSystem[] ammo = new AmmoStorageSystem[]{null};
        boolean[] search = new boolean[]{true};
        LinkedHashMap<CalibreComponentSlot, AmmoStorageSystem> loadedChambers = new LinkedHashMap<>();
        parent.searchSlots(
                new SlotSearchOptions()
                        .slotTag("chamber")
                        .targetPriority(stat("chamber_priority")),
                slot -> {
                    if (!search[0]) return;
                    if (ammo[0] == null || !ammo[0].hasNext()) {
                        SystemSearchResult<AmmoStorageSystem> result = collectAmmo();
                        if (result == null) {
                            search[0] = false;
                            return;
                        }
                        ammo[0] = result.getSystem();
                    }

                    CalibreComponent nextChamber = ammo[0].peek();
                    if (nextChamber != null && slot.isCompatible(nextChamber))
                        loadedChambers.put(slot, ammo[0]);
                }
        );

        Events.Chamber event2 = new Events.Chamber(
                event.getStack(), event.getSlot(), event.getUser(), this,
                loadedChambers
        );
        callEvent(event2);

        if (loadedChambers.size() > 0) {
            itemSystem.doAction(this, "chamber", user, event.getSlot(), sys -> {
                loadedChambers.forEach((slot, sys2) -> {
                    if (slot.get() != null && user instanceof LivingEntityItemUser)
                        ejectChamber(slot.get().getSystem(ProjectileProviderSystem.class), ((LivingEntityItemUser) user).getEntity());
                    slot.set(sys2.next());
                });
                event.updateItem();
            });
            event.updateItem();
        } else {
            fail(new Events.Fail(
                    event.getStack(), event.getSlot(), user, this, FailReason.EMPTY
            ));
        }
    }

    public void toggleAiming(Events.ToggleAiming event) {
        if (callEvent(event).cancelled) return;
        if (getMappedSight() == null) return;
        if (aiming == event.newAiming) return;

        itemSystem.doAction(this, "aim_" + (aiming ? "out" : "in"), event.getUser(), event.getSlot(), sys -> {
            aiming = event.newAiming;
            event.updateItem();
        });
        event.updateItem();
    }

    public void changeSight(Events.ChangeSight event) {
        if (callEvent(event).cancelled) return;
        SightReference newSight = event.newSight;
        if (newSight == null || newSight.equals(sight)) return;

        itemSystem.doAction(this, "change_sight", event.getUser(), event.getSlot(), sys -> {
            sight = newSight;
            event.updateItem();
        });
        event.updateItem();
    }

    public void changeFireMode(Events.ChangeFireMode event) {
        if (callEvent(event).cancelled) return;
        FireModeReference newMode = event.newMode;
        if (newMode == null || newMode.equals(fireMode)) return;

        itemSystem.doAction(this, "change_fire_mode", event.getUser(), event.getSlot(), sys -> {
            fireMode = newMode;
            event.updateItem();
        });
        event.updateItem();
    }

    public void reload(Events.PreReload event) {
        if (callEvent(event).cancelled) return;
        ItemUser user = event.getUser();
        ItemSlot itemSlot = event.getSlot();

        // TODO split these up into:
        // - load
        // - unload
        // - reload
        // - loadChamber
        boolean[] resume = new boolean[]{true};
        parent.searchSystems(ammoSystemSearch(), (slot, sys) -> {
            if (!resume[0]) return;
            if (sys.size() >= sys.getCapacity()) return;

            resume[0] = false;
            // Pass reloading to the ammo storage system
            sys.reload(slot, event);

        });
        if (!resume[0]) return;

        parent.searchSlots(ammoSlotSearch(), slot -> {
            if (!resume[0]) return;

            resume[0] = false;
            // Place non-empty compatible component into slot from inventory
            Utils.useService(CalibreComponentSupplier.class, s -> {
                CalibreComponent component = s.supply(slot, user, this, false);
                if (component != null) {
                    slot.set(component);
                    getTree().buildStats();
                    itemSystem.doAction(this, "load_ammo", user, itemSlot);
                    ItemStack oldStack = event.getStack().clone();
                    // todo
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        if (!event.getSlot().get().equals(oldStack)) return;
                        CalibreComponent newComponent = s.supply(slot, user, this, true);
                        if (newComponent == null) return;
                        slot.set(newComponent);
                        event.updateItem();
                    }, 15);
                }
            });
        });
        if (!resume[0]) return;

        parent.searchSlots(chamberSlotSearch(), slot -> {
            if (!resume[0]) return;
            if (slot.get() != null) return;

            resume[0] = false;
            // Place chamber from inventory
            Utils.useService(CalibreComponentSupplier.class, s -> {
                CalibreComponent component = s.supply(slot, user, this, true);
                // TODO apply this after a stat delay, "load_chamber_after"
                if (component != null) {
                    slot.set(component);
                    getTree().buildStats();
                    itemSystem.doAction(this, "load_chamber", user, itemSlot);
                    event.updateItem();
                }
            });
        });
    }

    public void fail(Events.Fail event) {
        if (callEvent(event).cancelled) return;
        itemSystem.doAction(this, "fail", event.getUser(), event.getSlot());
        event.updateItem();
    }

    public void ejectChamber(ProjectileProviderSystem chamber, LivingEntity entity) {
        if (!(chamber instanceof GunProjectileProviderSystem)) return;
        Vector offset = stat("ejection_port_offset");
        Location position = Utils.getFacingRelative(entity, offset);
        Vector velocity = Utils.getFacingRelative(entity, offset.add(stat("ejection_port_velocity"))).subtract(position).toVector();
        ItemStack item = ((GunProjectileProviderSystem) chamber).createEjection();
        if (item == null) return;

        Item drop = position.getWorld().dropItem(position, item);
        drop.setVelocity(velocity);
        drop.setPickupDelay(Integer.MAX_VALUE);
        drop.setCanMobPickup(false);
        drop.setTicksLived(ITEM_DESPAWN_TIME - (int) Utils.toTicks(stat("ejection_lifetime")));
        drop.setVelocity(velocity);
    }

    @Override public String getId() { return ID; }
    @Override public GunSystem clone() { return (GunSystem) super.clone(); }
    @Override public GunSystem copy() { return clone(); }

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

        public static class Event extends ItemEvents.SystemEvent<GunSystem> {
            public Event(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system);
            }
        }

        public static class Fire extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public Fire(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }


        public static class FireOnce extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public FireOnce(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static class PreChamber extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public PreChamber(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static class Chamber extends Event {
            private final LinkedHashMap<CalibreComponentSlot, AmmoStorageSystem> loadedChambers;

            public Chamber(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system, LinkedHashMap<CalibreComponentSlot, AmmoStorageSystem> loadedChambers) {
                super(stack, slot, user, system);
                this.loadedChambers = loadedChambers;
            }

            public LinkedHashMap<CalibreComponentSlot, AmmoStorageSystem> getLoadedChambers() { return loadedChambers; }
        }

        public static class ToggleAiming extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;
            private boolean newAiming;

            public ToggleAiming(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system, boolean newAiming) {
                super(stack, slot, user, system);
                this.newAiming = newAiming;
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            public boolean willAim() { return newAiming; }
            public void setWillAim(boolean newAiming) { this.newAiming = newAiming; }
        }

        public static class ChangeSight extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;
            private SightReference newSight;

            public ChangeSight(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system, SightReference newSight) {
                super(stack, slot, user, system);
                this.newSight = newSight;
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            public SightReference getNewSight() { return newSight; }
            public void setNewSight(SightReference newSight) { this.newSight = newSight; }
        }

        public static class CycleSight extends ChangeSight {
            public CycleSight(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system, system.getSight(system.getSightIndex() + 1));
            }
        }

        public static class ChangeFireMode extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;
            private FireModeReference newMode;

            public ChangeFireMode(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system, FireModeReference newMode) {
                super(stack, slot, user, system);
                this.newMode = newMode;
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            public FireModeReference getNewMode() { return newMode; }
            public void setNewMode(FireModeReference newMode) { this.newMode = newMode; }
        }

        public static class CycleFireMode extends ChangeFireMode {
            public CycleFireMode(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system, system.getFireMode(system.getFireModeIndex() + 1));
            }
        }

        public static class PreReload extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public PreReload(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static class Fail extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;
            private final int reason;

            public Fail(ItemStack stack, ItemSlot slot, ItemUser user, GunSystem system, int reason) {
                super(stack, slot, user, system);
                this.reason = reason;
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            public int getReason() { return reason; }
        }
    }
}
