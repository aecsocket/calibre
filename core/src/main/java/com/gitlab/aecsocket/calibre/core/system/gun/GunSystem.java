package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.rule.Rule;
import com.gitlab.aecsocket.calibre.core.rule.visitor.Visitor;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentContainerSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.ProjectileSystem;
import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.world.user.*;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.reload.external.ExternalReloadSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.reload.internal.InternalReloadSystem;
import com.gitlab.aecsocket.calibre.core.world.slot.EquippableSlot;
import com.gitlab.aecsocket.calibre.core.world.slot.HandSlot;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.unifiedframework.core.event.Cancellable;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.loop.MinecraftSyncLoop;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.BooleanStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.EnumStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.StringStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.Vector2DDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.Vector3DDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.core.util.data.Tuple3;
import com.gitlab.aecsocket.unifiedframework.core.util.data.Tuple4;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.Vector2DDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.Vector3DDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.ViewCoordinates;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class GunSystem extends AbstractSystem {
    /** Handles how to fire a gun if there is already a chamber loaded before firing. */
    public enum ChamberHandling {
        /** Only fire if there is an available chamber (closed-bolt gun). */
        NORMAL,
        /** Fail if there is an extra chamber (double feed in an open-bolt gun). */
        FAIL,
        /** Auto-chamber if there is none already, and fire. */
        SAFE
    }

    public static final String ID = "gun";
    public static final int LISTENER_PRIORITY = 0;
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            // Basic stats about projectiles
            .init("muzzle_velocity", NumberDescriptorStat.of(0d))
            .init("shots", NumberDescriptorStat.of(1))
            .init("projectiles", NumberDescriptorStat.of(1))
            .init("barrel_offset", new Vector3DDescriptorStat(new Vector3D()))
            .init("barrel_offset_random", new Vector3DDescriptorStat(new Vector3D()))
            .init("converge_range", NumberDescriptorStat.of(0d))
            .init("zero_range", NumberDescriptorStat.of(0d))
            .init("eject_offset", new Vector3DDescriptorStat(new Vector3D()))
            .init("eject_velocity", new Vector3DDescriptorStat(new Vector3D()))
            .init("resting_offset", new Vector3DDescriptorStat(new Vector3D()))

            // Target slot types
            .init("slot_tag_chamber", new StringStat("chamber"))
            .init("slot_tag_ammo", new StringStat("ammo"))

            // Toggles
            .init("auto_chamber", new BooleanStat(true))
            .init("auto_eject", new BooleanStat(true))
            .init("can_fire_underwater", new BooleanStat(false))
            .init("chamber_handling", new EnumStat<>(ChamberHandling.NORMAL, ChamberHandling.class))
            .init("sprint_disables", new BooleanStat(false))
            .init("chamber_while_aiming", new BooleanStat(false))
            .init("change_fire_mode_while_aiming", new BooleanStat(false))
            .init("reload_while_aiming", new BooleanStat(false))

            // Inaccuracy
            .init("inaccuracy_jump", NumberDescriptorStat.of(0d))
            .init("inaccuracy_shot", NumberDescriptorStat.of(0d))

            // Recoil
            .init("recoil", new Vector2DDescriptorStat(new Vector2D()))
            .init("recoil_random", new Vector2DDescriptorStat(new Vector2D()))
            .init("recoil_speed", NumberDescriptorStat.of(0d))
            .init("recoil_recovery", NumberDescriptorStat.of(0d))
            .init("recoil_recovery_speed", NumberDescriptorStat.of(0d))
            .init("recoil_recovery_after", NumberDescriptorStat.of(0L))
            .init("recoil_inaccuracy_coefficient", NumberDescriptorStat.of(0d))

            // Spread
            .init("spread", new Vector2DDescriptorStat(new Vector2D()))
            .init("spread_projectile", new Vector2DDescriptorStat(new Vector2D()))
            .init("spread_inaccuracy_coefficient", NumberDescriptorStat.of(0d))

            // Sway
            .init("sway", new Vector2DDescriptorStat(new Vector2D()))
            .init("sway_cycle", NumberDescriptorStat.of(0L))
            .init("sway_stabilization", new Vector2DDescriptorStat(new Vector2D(1)))
            .init("sway_inaccuracy_coefficient", NumberDescriptorStat.of(0d))

            // Actions
            .init("start_shot_delay", NumberDescriptorStat.of(0L))
            .init("shot_delay", NumberDescriptorStat.of(0L))
            .init("fire_delay", NumberDescriptorStat.of(0L))

            .init("chamber_delay", NumberDescriptorStat.of(0L))
            .init("chamber_after", NumberDescriptorStat.of(0L))

            .init("change_fire_mode_delay", NumberDescriptorStat.of(0L))
            .init("change_sight_delay", NumberDescriptorStat.of(0L))

            .init("fail_delay", NumberDescriptorStat.of(0L))
            .get();
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = MapInit.of(new HashMap<String, Class<? extends Rule>>())
            .init(Rules.Aiming.TYPE, Rules.Aiming.class)
            .init(Rules.Resting.TYPE, Rules.Resting.class)
            .init(Rules.HasSight.TYPE, Rules.HasSight.class)
            .init(Rules.HasFireMode.TYPE, Rules.HasFireMode.class)
            .get();

    @ConfigSerializable
    private static class Dependencies {
        private ConfigurationNode fallbackSight;
        private ConfigurationNode fallbackFireMode;
    }

    @Setting(nodeFromParent = true)
    private Dependencies dependencies;
    @FromMaster protected transient SightSystem.Sight fallbackSight;
    @FromMaster protected transient FireModeSystem.FireMode fallbackFireMode;
    protected transient SchedulerSystem scheduler;
    protected transient SwayStabilization stabilization;

    protected boolean aiming;
    protected SightPath sight;
    protected FireModePath fireMode;
    protected boolean resting;

    /**
     * Used for registration + deserialization.
     */
    public GunSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public GunSystem(GunSystem o) {
        super(o);
        stabilization = o.stabilization;
        fallbackSight = o.fallbackSight;
        fallbackFireMode = o.fallbackFireMode;
    }

    public SightSystem.Sight fallbackSight() { return fallbackSight; }
    public void fallbackSight(SightSystem.Sight fallbackSight) { this.fallbackSight = fallbackSight; }

    public FireModeSystem.FireMode fallbackFireMode() { return fallbackFireMode; }
    public void fallbackFireMode(FireModeSystem.FireMode fallbackFireMode) { this.fallbackFireMode = fallbackFireMode; }

    public SchedulerSystem scheduler() { return scheduler; }
    public SwayStabilization stabilization() { return stabilization; }

    public boolean aiming() { return aiming; }
    public void aiming(boolean aiming) { this.aiming = aiming; }

    public SightPath sight() { return sight; }
    public void sight(SightPath sight) { this.sight = sight; }
    public SightSystem.Sight getRawSight() { return sight == null ? null : sight.get(parent); }
    public SightSystem.Sight getSight() { return sight == null ? fallbackSight : sight.get(parent); }

    public FireModePath fireMode() { return fireMode; }
    public void fireMode(FireModePath fireMode) { this.fireMode = fireMode; }
    public FireModeSystem.FireMode getRawFireMode() { return fireMode == null ? null : fireMode.get(parent); }
    public FireModeSystem.FireMode getFireMode() { return fireMode == null ? fallbackFireMode : fireMode.get(parent); }

    public boolean resting() { return resting; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }
    @Override public Map<String, Class<? extends Rule>> ruleTypes() { return RULE_TYPES; }

    @Override
    public void buildStats(StatCollection stats) {
        FireModeSystem.FireMode fireMode = getFireMode();
        if (fireMode != null && fireMode.stats != null) {
            stats.combine(fireMode.stats.build(parent));
        }

        SightSystem.Sight sight = getSight();
        if (sight != null && sight.stats != null) {
            stats.combine(sight.stats.build(parent));
        }

        // TODO dirty * hack. do something about this. please.
        for (CalibreSlot slot : collectChamberSlots(stats.flatten())) {
            ProjectileSystem projectile = getProjectile(slot).c();
            if (projectile != null && projectile.stats() != null) {
                stats.combine(projectile.stats().build(parent));
            }
        }
    }

    private <T extends ContainerPath<?>, S extends CalibreSystem> List<T> collect(Class<S> systemType, Function<S, List<?>> listGetter, BiFunction<String[], Integer, T> provider) {
        List<T> results = new ArrayList<>();
        parent.<CalibreComponent<Item>>forWalkAndThis((component, path) -> {
            S system = component.system(systemType);
            if (system != null) {
                List<?> containers = listGetter.apply(system);
                for (int i = 0; i < containers.size(); i++) {
                    results.add(provider.apply(path, i));
                }
            }
        });
        return results;
    }

    public List<SightPath> collectSights() {
        return this.collect(SightSystem.class, sys -> sys.sights, SightPath::new);
    }

    public List<FireModePath> collectFireModes() {
        return this.collect(FireModeSystem.class, sys -> sys.fireModes, FireModePath::new);
    }

    public List<CalibreSlot> collectChamberSlots(StatMap stats) {
        return parent.collectSlots(stats.val("slot_tag_chamber"));
    }
    public List<CalibreSlot> collectChamberSlots() { return collectChamberSlots(tree().stats()); }
    public List<ChamberSystem> collectChambers(List<CalibreSlot> slots) {
        return parent.fromSlots(slots, ChamberSystem.class);
    }

    public List<CalibreSlot> collectAmmoSlots(StatMap stats) {
        return parent.collectSlots(stats.val("slot_tag_ammo"));
    }
    public List<CalibreSlot> collectAmmoSlots() { return collectAmmoSlots(tree().stats()); }
    public List<ComponentContainerSystem> collectAmmo(List<CalibreSlot> slots) {
        return parent.fromSlots(slots, ComponentContainerSystem.class);
    }

    @Override
    public void setup(CalibreComponent<?> parent) {
        super.setup(parent);
        fallbackSight = deserialize(dependencies.fallbackSight, SightSystem.Sight.class);
        fallbackFireMode = deserialize(dependencies.fallbackFireMode, FireModeSystem.FireMode.class);
        dependencies = null;
        if (fallbackSight.id == null)
            fallbackSight = null;
        if (fallbackFireMode.id == null)
            fallbackFireMode = null;
        require(SchedulerSystem.class);
        require(SwayStabilization.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;
        if (!tree().complete()) return;

        scheduler = require(SchedulerSystem.class);
        stabilization = require(SwayStabilization.class);
        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Jump.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Scroll.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.GameClick.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.SwapHand.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Drop.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.BreakBlock.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.PlaceBlock.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Death.class, this::onEvent, listenerPriority);

        if (getRawSight() == null) {
            sight = null;
            List<SightPath> found = collectSights();
            if (found.size() > 0)
                sight = found.get(0);
        }

        if (getRawFireMode() == null) {
            fireMode = null;
            List<FireModePath> found = collectFireModes();
            if (found.size() > 0)
                fireMode = found.get(0);
        }
    }

    protected Component createFireModes(Locale locale) {
        Component separator = gen(locale, "system." + ID + ".fire_modes.separator");
        List<Component> values = new ArrayList<>();
        for (FireModePath ref : collectFireModes()) {
            values.add(gen(locale, "system." + ID + ".fire_modes." + (ref.equals(fireMode) ? "selected" : "unselected"),
                    "value", gen(locale, "fire_mode.short." + ref.get(parent).id)));
        }
        return values.size() == 0 ? gen(locale, "system." + ID + ".fire_modes.none") : Utils.join(separator, values);
    }

    protected Component createSights(Locale locale) {
        Component separator = gen(locale, "system." + ID + ".sights.separator");
        List<Component> values = new ArrayList<>();
        for (SightPath ref : collectSights()) {
            values.add(gen(locale, "system." + ID + ".sights." + (ref.equals(sight) ? "selected" : "unselected"),
                    "value", gen(locale, "sight.short." + ref.get(parent).id)));
        }
        return values.size() == 0 ? gen(locale, "system." + ID + ".sights.none") : Utils.join(separator, values);
    }

    public boolean resting(RestableUser user, ItemSlot<?> slot) {
        return user.restsOn(offset(user, slot, tree().<Vector3DDescriptor>stat("resting_offset").apply()));
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        List<Component> info = new ArrayList<>();
        Locale locale = event.locale();

        info.add(gen(locale, "system." + ID + ".info",
                "fire_modes", createFireModes(locale),
                "sights", createSights(locale)));

        event.item().addInfo(info);
    }

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        ItemUser user = event.user();

        if (event.tickContext().loop() instanceof MinecraftSyncLoop) {
            boolean update = false;

            boolean pre = resting;
            resting = user instanceof RestableUser && resting((RestableUser) user, event.slot());
            if (resting != pre) {
                update = true;
            }

            if (aiming && getSight() == null) {
                aiming = false;
                update = true;
            }

            if (event.slot() instanceof HandSlot) {
                HandSlot<I> handSlot = (HandSlot<I>) event.slot();
                HandSlot<I> opposite = handSlot.opposite();
                if (aiming && (!handSlot.main() || (opposite != null && opposite.get() != null))) {
                    aiming = false;
                    update = true;
                }
            }

            if (update) {
                tree().build();
                update(event);
            }
        } else {
            if (user instanceof CameraUser) {
                // sway
                TickContext ctx = event.tickContext();
                Vector2D sway = tree().<Vector2DDescriptor>stat("sway").apply()
                        .multiply(ctx.delta() / 1000d);
                long cycle = tree().<NumberDescriptor.Long>stat("sway_cycle").apply();

                if (user instanceof InaccuracyUser) {
                    sway = sway.multiply(1 + (
                            calculateInaccuracy((InaccuracyUser) user) * tree().<NumberDescriptor.Double>stat("sway_inaccuracy_coefficient").apply()
                    ));
                }

                if (sway.manhattanLength() > 0 && cycle > 0) {
                    double angle = ((double) ctx.elapsed() / (cycle / 2d /* so it is a full ellipse of sway, not half of one */)) * Math.PI;
                    Vector2D rotation = new Vector2D(
                            Math.cos(angle) * sway.x(),
                            Math.sin(angle) * sway.y()
                    );
                    if (stabilization.stabilizes(ctx, user))
                        rotation = rotation.multiply(tree().<Vector2DDescriptor>stat("sway_stabilization").apply());
                    ((CameraUser) user).applyRotation(rotation);
                }
            }
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Jump<I> event) {
        if (event.user() instanceof InaccuracyUser)
            ((InaccuracyUser) event.user()).addInaccuracy(tree().<NumberDescriptor.Double>stat("inaccuracy_jump").apply());
    }

    protected <I extends Item> void onEvent(ItemEvents.GameClick<I> event) {
        if (!scheduler.available()) return;
        ItemUser user = event.user();
        if (tree().<Boolean>stat("sprint_disables") && user instanceof MovementUser && ((MovementUser) user).sprinting()) return;

        CalibreComponent<I> component = event.component();
        ItemSlot<I> slot = event.slot();
        if (slot instanceof HandSlot) {
            HandSlot<I> handSlot = (HandSlot<I>) slot;
            HandSlot<I> opposite = handSlot.opposite();
            if (!handSlot.main() || (opposite != null && opposite.get() != null)) {
                // dual wielding
                if (handSlot.main()) {
                    if (event.type() == ItemEvents.GameClick.LEFT)
                        startFire(new Events.StartFire<>(component, user, slot, this));
                } else if (event.type() == ItemEvents.GameClick.RIGHT)
                    startFire(new Events.StartFire<>(component, user, slot, this));
                return;
            }
        }

        // main hand OR not hand slot
        if (event.type() == ItemEvents.GameClick.LEFT) {
            if (!tree().<Boolean>stat("can_fire_underwater") && user instanceof SwimmableUser && ((SwimmableUser) user).swimming())
                return;
            startFire(new Events.StartFire<>(component, user, slot, this));
        } else if (event.type() == ItemEvents.GameClick.RIGHT) {
            aim(new Events.Aim<>(
                    component, user, slot, this, !aiming
            ));
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Scroll<I> event) {
        if (!aiming)
            return;

        int length = event.length();
        ItemUser user = event.user();
        if (user instanceof MovementUser && ((MovementUser) user).sneaking()) {
            if (!scheduler.available()) return;

            List<SightPath> collected = collectSights();
            int size = collected.size();
            if (size == 0)
                return;
            int idx = Utils.wrapIndex(size, collected.indexOf(sight) + length);

            changeSight(new Events.ChangeSight<>(
                    event.component(), event.user(), event.slot(), this, collected.get(idx)
            ));
        } else {
            /*
            zoom = Utils.clamp(zoom + (direction * 0.1), -1, 1);
            test(event.user(), zoom);
            event.updateItem(this);
             */
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Switch<I> event) {
        if (event.position() == ItemEvents.Switch.TO)
            return;

        if (aiming)
            event.cancel();
    }

    protected <I extends Item> void onEvent(ItemEvents.SwapHand<I> event) {
        event.cancel();
        if (!scheduler.available()) return;
        ItemUser user = event.user();
        if (tree().<Boolean>stat("sprint_disables") && user instanceof MovementUser && ((MovementUser) user).sprinting()) return;

        List<FireModePath> collected = collectFireModes();
        int size = collected.size();
        if (size == 0)
            return;

        int idx = Utils.wrapIndex(size, collected.indexOf(fireMode) + (user instanceof MovementUser && ((MovementUser) user).sneaking() ? -1 : 1));
        if (!aiming || tree().<Boolean>stat("change_fire_mode_while_aiming")) {
            changeFireMode(new Events.ChangeFireMode<>(
                    event.component(), event.user(), event.slot(), this, collected.get(idx)
            ));
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Drop<I> event) {
        if (!(event.slot() instanceof EquippableSlot) || !((EquippableSlot<I>) event.slot()).equipped())
            return;

        event.cancel();
        if (!scheduler.available()) return;
        if (aiming && !tree().<Boolean>stat("reload_while_aiming"))
            return;

        ItemUser user = event.user();
        if (tree().<Boolean>stat("sprint_disables") && user instanceof MovementUser && ((MovementUser) user).sprinting()) return;

        List<CalibreSlot> ammoSlots = collectAmmoSlots();
        if (ammoSlots.size() == 0)
            return;
        CalibreSlot ammoSlot = ammoSlots.get(0);
        CalibreComponent<?> component = ammoSlot.get();
        if (component == null) {
            InternalReloadSystem handler = parent.system(InternalReloadSystem.class);
            if (handler != null) {
                internalReload(new Events.InternalReload<>(
                        event.component(), event.user(), event.slot(), this,
                        handler, ammoSlot
                ));
            }
        } else {
            ExternalReloadSystem handler = component.system(ExternalReloadSystem.class);
            if (handler != null) {
                externalReload(new Events.ExternalReload<>(
                        event.component(), event.user(), event.slot(), this,
                        handler, ammoSlot
                ));
            }
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.BreakBlock<I> event) {
        event.cancel();
    }

    protected <I extends Item> void onEvent(ItemEvents.PlaceBlock<I> event) {
        event.cancel();
    }

    protected <I extends Item> void onEvent(ItemEvents.Death<I> event) {
        aiming = false;
        update(event);
    }

    public <I extends Item> void startFire(Events.StartFire<I> event) {
        if (tree().call(event).cancelled) return;

        List<CalibreSlot> chamberSlots = collectChamberSlots();
        ChamberHandling handling = tree().stat("chamber_handling");
        Tuple4<?, ?, ?, ?> chamber = collectChamber(chamberSlots);
        if (handling == ChamberHandling.NORMAL && (chamber == null || chamber.d() == null)) {
            chamber(new Events.Chamber<>(
                    event.component(), event.user(), event.slot(), this
            ), chamberSlots);
        } else {
            long shotStart = tree().<NumberDescriptor.Long>stat("start_shot_delay").apply();
            long shotDelay = tree().<NumberDescriptor.Long>stat("shot_delay").apply();
            int shots = tree().<NumberDescriptor.Integer>stat("shots").apply();
            scheduler.delay(tree().<NumberDescriptor.Long>stat("fire_delay").apply());
            for (int i = 0; i < shots; i++) {
                scheduler.schedule(this, shotStart + (shotDelay * i), (self, equip, ctx) -> self.fire(new Events.Fire<>(
                        equip.component(), equip.user(), equip.slot(), self
                )));
            }

            tree().build();
            event.updateItem();
        }
    }

    public double random(double bound) {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * bound;
    }

    public Vector3D rotate(Vector3D vec, Vector2D bound, double length) {
        ViewCoordinates view = vec
                .rotateY(random(Math.toRadians(bound.x())))
                .toViewCoordinates();
        return view
                .pitch(Math.toRadians(Math.toDegrees(view.pitch()) + random(bound.y())))
                .toVector()
                .multiply(length);
    }

    public <I extends Item> void ejectCasing(CalibreComponent<I> chamber, ItemUser user, ItemSlot<I> slot) {
        Vector3D offset = tree().<Vector3DDescriptor>stat("eject_offset").apply();
        Vector3D position = offset(user, slot, offset);
        Vector3D velocity = offset(user, slot, offset.add(tree().<Vector3DDescriptor>stat("eject_velocity").apply())).subtract(position);
        ejectCasing(
                chamber,
                user,
                position,
                velocity
        );
    }

    public abstract <I extends Item> void ejectCasing(CalibreComponent<I> chamber, ItemUser user, Vector3D position, Vector3D velocity);

    public Vector3D offset(ItemUser user, ItemSlot<?> slot, Vector3D offset) {
        return Utils.relativeOffset(user.position(), user.direction(), offset);
    }

    public double calculateInaccuracy(InaccuracyUser user) {
        return user.inaccuracy();
    }

    public <I extends Item> Vector2D calculateSpread(Events.Fire<I> event) {
        Vector2D spread = tree().<Vector2DDescriptor>stat("spread").apply(new Vector2D());
        ItemUser user = event.user();
        if (user instanceof InaccuracyUser) {
            spread = spread.multiply(1 + (
                    calculateInaccuracy((InaccuracyUser) user) * tree().<NumberDescriptor.Double>stat("spread_inaccuracy_coefficient").apply()
            ));
        }
        return spread;
    }

    public <I extends Item> void fire(Events.Fire<I> event) {
        if (tree().call(event).cancelled) return;

        ChamberHandling handling = tree().stat("chamber_handling");
        ItemUser user = event.user();
        ItemSlot<I> itemSlot = event.slot();

        // Get the next chamber to fire
        List<CalibreSlot> chamberSlots = collectChamberSlots();
        Tuple4<CalibreSlot, ChamberSystem, CalibreSlot, ProjectileSystem> result;
        switch (handling) {
            case FAIL:
                result = collectChamber(chamberSlots);
                if (result == null || (result.a() != null && result.a().get() == null)) {
                    updateChambers(chamberSlots, false, false, user, itemSlot);
                    result = collectChamber(chamberSlots);
                } else {
                    fail(new Events.Fail<>(
                            event.component(), user, itemSlot, this, Events.Fail.Reason.DOUBLE_FEED
                    ));
                    return;
                }
                break;
            case SAFE:
                result = collectChamber(chamberSlots);
                if (result == null || result.b() == null) {
                    updateChambers(chamberSlots, false, false, user, itemSlot);
                    result = collectChamber(chamberSlots);
                }
                break;
            default:
                result = collectChamber(chamberSlots);
        }
        if (result == null || result.b() == null)
            return;

        event.chamberSlot = result.a();
        event.chamberSystem = result.b();
        event.loadSlot = result.c();
        event.projectileSystem = result.d();

        // get barrel offset
        Vector3D offset = Utils.addRandom(
                tree().<Vector3DDescriptor>stat("barrel_offset").apply(),
                tree().<Vector3DDescriptor>stat("barrel_offset_random").apply()
        );
        Vector3D position = offset(user, itemSlot, offset);
        ViewCoordinates view = user.direction().toViewCoordinates();

        double zeroRange = tree().<NumberDescriptor.Double>stat("zero_range").apply();
        if (zeroRange > 0) {
            double add = zero(
                    tree().<NumberDescriptor.Double>stat("muzzle_velocity").apply(),
                    zeroRange,
                    0,
                    tree().<NumberDescriptor.Double>stat("projectile_gravity").apply()
            );
            view = view.pitch(view.pitch() - add); /* subtract because pitch is inverted */
        }

        double convergeRange = tree().<NumberDescriptor.Double>stat("converge_range").apply();
        if (convergeRange > 0) {
            Vector3D ahead = user.position().add(view.toVector().multiply(convergeRange));
            view = ahead.subtract(position).toViewCoordinates();
        }

        // get shot origin vector
        double velocity = tree().<NumberDescriptor.Double>stat("muzzle_velocity").apply();
        Vector2D spread = calculateSpread(event);
        Vector3D direction = rotate(
                view.toVector(),
                spread,
                velocity
        );

        fireSuccess(new Events.FireSuccess<>(
                event.component(), user, itemSlot, this,
                result.a(), result.b(), result.c(), result.d(),
                position, direction, velocity
        ));
    }

    public <I extends Item> void fireSuccess(Events.FireSuccess<I> event) {
        if (tree().call(event).cancelled) return;

        Vector3D position = event.position;
        Vector3D direction = event.direction;
        double velocity = event.velocity;
        CalibreSlot chamberSlot = event.chamberSlot();
        ItemUser user = event.user();

        // shoot projectiles
        Vector2D projectileSpread = tree().<Vector2DDescriptor>stat("spread_projectile").apply(new Vector2D());
        for (int i = 0; i < tree().<NumberDescriptor.Integer>stat("projectiles").apply(); i++) {
            event.projectileSystem.createProjectile(event.user(), position, rotate(direction, projectileSpread, velocity));
        }

        // apply recoil
        if (user instanceof RecoilableUser) {
            Vector2D recoil = Utils.addRandom(
                    tree().<Vector2DDescriptor>stat("recoil").apply(),
                    tree().<Vector2DDescriptor>stat("recoil_random").apply()
            );
            if (user instanceof InaccuracyUser)
                recoil = recoil.multiply(1 + (
                        calculateInaccuracy((InaccuracyUser) event.user()) * tree().<NumberDescriptor.Double>stat("recoil_inaccuracy_coefficient").apply()
                ));

            ((RecoilableUser) user).applyRecoil(
                    recoil,
                    tree().<NumberDescriptor.Double>stat("recoil_speed").apply(),
                    tree().<NumberDescriptor.Double>stat("recoil_recovery").apply(),
                    tree().<NumberDescriptor.Double>stat("recoil_recovery_speed").apply(),
                    tree().<NumberDescriptor.Long>stat("recoil_recovery_after").apply()
            );
        }

        // apply inaccuracy
        if (user instanceof InaccuracyUser)
            ((InaccuracyUser) user).addInaccuracy(tree().<NumberDescriptor.Double>stat("inaccuracy_shot").apply());

        // remove load - we've just shot it
        event.loadSlot.set(null);
        // eject casing if there is any (if the load =/= the casing)
        if (tree().<Boolean>stat("auto_eject") && chamberSlot.get() != null) {
            ejectCasing(chamberSlot.<CalibreComponent<I>>get().buildTree(), user, event.slot());
            chamberSlot.set(null);
        }

        // if we auto-chamber, load new chamber from ammo
        if (tree().stat("auto_chamber")) {
            List<ComponentContainerSystem> allAmmo = collectAmmo(collectAmmoSlots());
            for (ComponentContainerSystem ammo : allAmmo) {
                CalibreComponent<?> peek = ammo.peek();
                if (peek != null && chamberSlot.get() == null && chamberSlot.isCompatible(peek)) {
                    chamberSlot.set(ammo.pop());
                    break;
                }
            }
        }

        update(event);
    }

    public <I extends Item> boolean updateChambers(List<CalibreSlot> chamberSlots, boolean dry, boolean ejectLoaded, ItemUser user, ItemSlot<I> itemSlot) {
        List<ComponentContainerSystem> allAmmo = collectAmmo(collectAmmoSlots());
        boolean success = false;
        for (CalibreSlot slot : chamberSlots) {
            CalibreComponent<I> chamber = slot.get();
            if (chamber != null) {
                if (ejectLoaded || getProjectile(slot).c() == null) {
                    if (!dry) {
                        // eject if it has no valid load OR we want to eject all loaded
                        ejectCasing(chamber.buildTree(), user, itemSlot);
                        slot.set(null);
                    }
                    // this is considered a "successful" action since we still do *something*
                    success = true;
                } else
                    // skip this if it's already loaded
                    continue;
            }
            // find a compatible ammo component
            ComponentContainerSystem ammo = null;
            for (ComponentContainerSystem current : allAmmo) {
                CalibreComponent<?> peek = current.peek();
                if (peek == null || !slot.isCompatible(peek)) continue;
                ammo = current;
            }
            if (ammo == null) continue;
            if (!dry)
                slot.set(ammo.pop());
            success = true;
        }
        return success;
    }

    public <I extends Item> void chamber(Events.Chamber<I> event, List<CalibreSlot> chamberSlots) {
        if (tree().call(event).cancelled) return;

        if (!aiming || tree().<Boolean>stat("chamber_while_aiming")) {
            event.result = updateChambers(chamberSlots, true, false, null, null) ? ItemEvents.Result.SUCCESS : ItemEvents.Result.FAILURE;
            if (event.result == ItemEvents.Result.SUCCESS) {
                scheduler.schedule(this, tree().<NumberDescriptor.Long>stat("chamber_after").apply(), (self, equip, ctx) -> self.endChamber(equip));
            }
        } else
            event.result = ItemEvents.Result.FAILURE;

        if (event.result == ItemEvents.Result.SUCCESS) {
            scheduler.delay(tree().<NumberDescriptor.Long>stat("chamber_delay").apply());
        } else {
            fail(new Events.Fail<>(
                    event.component(), event.user(), event.slot(), this, Events.Fail.Reason.EMPTY
            ));
        }

        tree().build();
        event.updateItem();
    }

    public <I extends Item> void chamber(Events.Chamber<I> event) {
        chamber(event, collectChamberSlots());
    }

    protected <I extends Item> void endChamber(ItemEvents.Equipped<I> event) {
        updateChambers(collectChamberSlots(), false, false, event.user(), event.slot());
        update(event);
    }

    public <I extends Item> void aim(Events.Aim<I> event) {
        if (tree().call(event).cancelled) return;

        if (getSight() == null || aiming == event.aim) {
            event.result = ItemEvents.Result.FAILURE;
            return;
        }
        aiming = event.aim;

        event.result = ItemEvents.Result.SUCCESS;
        tree().build();
        event.updateItem();
    }

    public <I extends Item> void changeSight(Events.ChangeSight<I> event) {
        if (tree().call(event).cancelled) return;

        if (sight != null && sight.equals(event.sight)) {
            event.result = ItemEvents.Result.FAILURE;
            return;
        }
        sight = event.sight;

        event.result = ItemEvents.Result.SUCCESS;
        scheduler.delay(tree().<NumberDescriptor.Long>stat("change_sight_delay").apply());

        tree().build();
        event.updateItem();
    }

    public <I extends Item> void changeFireMode(Events.ChangeFireMode<I> event) {
        if (tree().call(event).cancelled) return;

        if (fireMode != null && fireMode.equals(event.fireMode)) {
            event.result = ItemEvents.Result.FAILURE;
            return;
        }
        fireMode = event.fireMode;

        event.result = ItemEvents.Result.SUCCESS;
        scheduler.delay(tree().<NumberDescriptor.Long>stat("change_fire_mode_delay").apply());
        tree().build();
        event.updateItem();
    }

    public <I extends Item> void internalReload(Events.InternalReload<I> event) {
        if (tree().call(event).cancelled()) return;
        event.handler().reload(event);
    }

    public <I extends Item> void externalReload(Events.ExternalReload<I> event) {
        if (tree().call(event).cancelled()) return;
        event.handler().reload(event);
    }

    public <I extends Item> void fail(Events.Fail<I> event) {
        scheduler.delay(tree().<NumberDescriptor.Long>stat("fail_delay").apply());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GunSystem that = (GunSystem) o;
        return aiming == that.aiming && Objects.equals(sight, that.sight) && Objects.equals(fireMode, that.fireMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aiming, sight, fireMode);
    }

    /**
     * Gets the pitch angle to use to hit a point at a particular distance: zeroes in the gun.
     * @param v The velocity in m/s.
     * @param x The distance to the target in m.
     * @param y The height difference in m.
     * @param g The gravity in m/s.
     * @return The pitch angle in radians to add to hit the desired point.
     */
    public static double zero(double v /* speed */, double x /* dist */, double y /* height */, double g /* gravity */) {
        double theta = (float)(
                Math.atan((Math.pow(v, 2) - Math.sqrt(Math.pow(v, 4) - (g * (g*Math.pow(x, 2) + 2*y*Math.pow(v, 2))))) / (g*x))
        );
        if (!Double.isFinite(theta))
            return 0;
        return theta;
    }

    public static Tuple3<ChamberSystem, CalibreSlot, ProjectileSystem> getProjectile(CalibreComponent<?> component) {
        ChamberSystem chamberSystem;
        if (
                component == null
                        || (chamberSystem = component.system(ChamberSystem.class)) == null
        ) return Tuple3.of(null, null, null);

        CalibreSlot loadSlot = chamberSystem.getLoadSlot();
        if (loadSlot == null)
            return Tuple3.of(chamberSystem, null, null);

        CalibreComponent<?> loadComponent = loadSlot.get();
        ProjectileSystem projectileSystem;
        if (
                loadComponent == null
                        || (projectileSystem = loadComponent.system(ProjectileSystem.class)) == null
        ) return Tuple3.of(chamberSystem, loadSlot, null);

        return Tuple3.of(chamberSystem, loadSlot, projectileSystem);
    }

    public static Tuple3<ChamberSystem, CalibreSlot, ProjectileSystem> getProjectile(CalibreSlot chamberSlot) {
        return getProjectile(chamberSlot.<CalibreComponent<?>>get());
    }

    public static Tuple4<CalibreSlot, ChamberSystem, CalibreSlot, ProjectileSystem> collectChamber(List<CalibreSlot> slots) {
        if (slots.size() == 0)
            return null;
        CalibreSlot chamberSlot = slots.get(0);
        Tuple3<ChamberSystem, CalibreSlot, ProjectileSystem> result = getProjectile(chamberSlot);
        return Tuple4.of(chamberSlot, result.a(), result.b(), result.c());
    }


    public static final class Rules {
        private Rules() {}

        @ConfigSerializable
        public static class Aiming implements Rule {
            public static final String TYPE = "aiming";
            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(CalibreComponent<?> component) {
                return component.system(ID) != null && component.<GunSystem>system(ID).aiming;
            }

            @Override public void visit(Visitor visitor) { visitor.visit(this); }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                return o != null && getClass() == o.getClass();
            }
        }

        @ConfigSerializable
        public static class Resting implements Rule {
            public static final String TYPE = "resting";
            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(CalibreComponent<?> component) {
                return component.system(ID) != null && component.<GunSystem>system(ID).resting;
            }

            @Override public void visit(Visitor visitor) { visitor.visit(this); }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                return o != null && getClass() == o.getClass();
            }
        }

        @ConfigSerializable
        public static class HasSight implements Rule {
            public static final String TYPE = "has_sight";
            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(CalibreComponent<?> component) {
                return component.system(ID) != null && component.<GunSystem>system(ID).sight != null;
            }

            @Override public void visit(Visitor visitor) { visitor.visit(this); }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                return o != null && getClass() == o.getClass();
            }
        }

        @ConfigSerializable
        public static class HasFireMode implements Rule {
            public static final String TYPE = "has_fire_mode";
            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(CalibreComponent<?> component) {
                return component.system(ID) != null && component.<GunSystem>system(ID).fireMode != null;
            }

            @Override public void visit(Visitor visitor) { visitor.visit(this); }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                return o != null && getClass() == o.getClass();
            }
        }
    }

    public static final class Events {
        private Events() {}

        public static class Base<I extends Item> extends ItemEvents.Base<I> implements ItemEvents.SystemEvent<I, GunSystem> {
            private final GunSystem system;

            public Base(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system) {
                super(component, user, slot);
                this.system = system;
            }

            @Override public GunSystem system() { return system; }
        }

        public static class StartFire<I extends Item> extends Base<I> implements Cancellable {
            private boolean cancelled;

            public StartFire(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system) {
                super(component, user, slot, system);
            }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class Fire<I extends Item> extends StartFire<I> {
            private CalibreSlot chamberSlot;
            private ChamberSystem chamberSystem;
            private CalibreSlot loadSlot;
            private ProjectileSystem projectileSystem;
            private boolean cancelled;

            public Fire(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system) {
                super(component, user, slot, system);
            }

            public CalibreSlot chamberSlot() { return chamberSlot; }
            public void chamberSlot(CalibreSlot chamberSlot) { this.chamberSlot = chamberSlot; }

            public ChamberSystem chamberSystem() { return chamberSystem; }
            public void chamberSystem(ChamberSystem chamberSystem) { this.chamberSystem = chamberSystem; }

            public CalibreSlot loadSlot() { return loadSlot; }
            public void loadSlot(CalibreSlot calibreSlot) { this.loadSlot = calibreSlot; }

            public ProjectileSystem projectileSystem() { return projectileSystem; }
            public void projectileSystem(ProjectileSystem projectileSystem) { this.projectileSystem = projectileSystem; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class FireSuccess<I extends Item> extends Fire<I> implements Cancellable {
            private final ChamberSystem chamberSystem;
            private final CalibreSlot loadSlot;
            private final ProjectileSystem projectileSystem;
            private Vector3D position;
            private Vector3D direction;
            private double velocity;
            private boolean cancelled;

            public FireSuccess(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system,
                               CalibreSlot chamberSlot, ChamberSystem chamberSystem, CalibreSlot loadSlot, ProjectileSystem projectileSystem,
                               Vector3D position, Vector3D direction, double velocity) {
                super(component, user, slot, system);
                chamberSlot(chamberSlot);
                this.chamberSystem = chamberSystem;
                this.loadSlot = loadSlot;
                this.projectileSystem = projectileSystem;
                this.position = position;
                this.direction = direction;
                this.velocity = velocity;
            }

            public ChamberSystem chamberSystem() { return chamberSystem; }
            public CalibreSlot loadSlot() { return loadSlot; }
            public ProjectileSystem projectileSystem() { return projectileSystem; }

            public Vector3D position() { return position; }
            public void position(Vector3D position) { this.position = position; }

            public Vector3D direction() { return direction; }
            public void direction(Vector3D direction) { this.direction = direction; }

            public double velocity() { return velocity; }
            public void velocity(double velocity) { this.velocity = velocity; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class Chamber<I extends Item> extends Base<I> implements Cancellable {
            private boolean cancelled;
            private ItemEvents.Result result = ItemEvents.Result.NONE;

            public Chamber(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system) {
                super(component, user, slot, system);
            }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }

            public ItemEvents.Result result() { return result; }
            public void result(ItemEvents.Result result) { this.result = result; }
        }

        public static class Aim<I extends Item> extends Base<I> implements Cancellable {
            private final boolean aim;
            private boolean cancelled;
            private ItemEvents.Result result = ItemEvents.Result.NONE;

            public Aim(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, boolean aim) {
                super(component, user, slot, system);
                this.aim = aim;
            }

            public boolean aim() { return aim; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }

            public ItemEvents.Result result() { return result; }
            public void result(ItemEvents.Result result) { this.result = result; }
        }

        public static class ChangeSight<I extends Item> extends Base<I> implements Cancellable {
            private final SightPath sight;
            private boolean cancelled;
            private ItemEvents.Result result = ItemEvents.Result.NONE;

            public ChangeSight(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, SightPath sight) {
                super(component, user, slot, system);
                this.sight = sight;
            }

            public SightPath sight() { return sight; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }

            public ItemEvents.Result result() { return result; }
            public void result(ItemEvents.Result result) { this.result = result; }
        }

        public static class ChangeFireMode<I extends Item> extends Base<I> implements Cancellable {
            private final FireModePath fireMode;
            private boolean cancelled;
            private ItemEvents.Result result = ItemEvents.Result.NONE;

            public ChangeFireMode(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, FireModePath fireMode) {
                super(component, user, slot, system);
                this.fireMode = fireMode;
            }

            public FireModePath fireMode() { return fireMode; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }

            public ItemEvents.Result result() { return result; }
            public void result(ItemEvents.Result result) { this.result = result; }
        }

        public static class Reload<I extends Item, H> extends Base<I> implements Cancellable {
            private final H handler;
            private final CalibreSlot ammoSlot;
            private ItemEvents.Result result = ItemEvents.Result.NONE;
            private boolean cancelled;

            public Reload(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, H handler, CalibreSlot ammoSlot) {
                super(component, user, slot, system);
                this.handler = handler;
                this.ammoSlot = ammoSlot;
            }

            public H handler() { return handler; }
            public CalibreSlot ammoSlot() { return ammoSlot; }

            public ItemEvents.Result result() { return result; }
            public Reload<I, H> result(ItemEvents.Result result) { this.result = result; return this; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class InternalReload<I extends Item> extends Reload<I, InternalReloadSystem> {
            public InternalReload(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, InternalReloadSystem handler, CalibreSlot ammoSlot) {
                super(component, user, slot, system, handler, ammoSlot);
            }
        }

        public static class ExternalReload<I extends Item> extends Reload<I, ExternalReloadSystem> {
            public ExternalReload(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, ExternalReloadSystem handler, CalibreSlot ammoSlot) {
                super(component, user, slot, system, handler, ammoSlot);
            }
        }

        public static class Fail<I extends Item> extends Base<I> {
            public static final class Reason {
                private Reason() {}

                public static final int EMPTY = 0;
                public static final int DOUBLE_FEED = 1;
            }

            private final int reason;

            public Fail(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, int reason) {
                super(component, user, slot, system);
                this.reason = reason;
            }

            public int reason() { return reason; }
        }
    }
}
