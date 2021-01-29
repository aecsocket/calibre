package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.builtin.CapacityComponentContainerSystem;
import me.aecsocket.calibre.system.builtin.ComponentContainerSystem;
import me.aecsocket.calibre.system.builtin.ProjectileSystem;
import me.aecsocket.calibre.system.builtin.SchedulerSystem;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.*;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.Vector2DDescriptorStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.Vector3DDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.Tuple2;
import me.aecsocket.unifiedframework.util.data.Tuple3;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector2DDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector3DDescriptor;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import me.aecsocket.unifiedframework.util.vector.ViewCoordinates;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class GunSystem extends AbstractSystem {
    public static final String ID = "gun";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("muzzle_velocity", NumberDescriptorStat.of(0d))
            .init("shots", NumberDescriptorStat.of(1))
            .init("projectiles", NumberDescriptorStat.of(1))
            .init("barrel_offset", new Vector3DDescriptorStat(new Vector3D()))

            .init("recoil", new Vector2DDescriptorStat(new Vector2D()))
            .init("recoil_speed", NumberDescriptorStat.of(0d))
            .init("recoil_recovery", NumberDescriptorStat.of(0d))
            .init("recoil_recovery_speed", NumberDescriptorStat.of(0d))
            .init("recoil_recovery_after", NumberDescriptorStat.of(0L))
            .init("spread", new Vector2DDescriptorStat(new Vector2D()))
            .init("projectile_spread", new Vector2DDescriptorStat(new Vector2D()))

            .init("fire_delay", NumberDescriptorStat.of(0L))
            .init("shot_delay", NumberDescriptorStat.of(0L))
            .get();
    public static final String SLOT_TAG_CHAMBER = "chamber";
    public static final String SLOT_TAG_AMMO = "ammo";

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode aimingStats;
        protected ConfigurationNode notAimingStats;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;
    protected transient SchedulerSystem scheduler;
    @FromMaster
    protected transient StatCollection aimingStats;
    @FromMaster
    protected transient StatCollection notAimingStats;

    protected boolean aiming;
    protected SightPath sight;
    protected FireModePath fireMode;

    /**
     * Used for registration + deserialization.
     */
    public GunSystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public GunSystem(GunSystem o) {
        super(o);
        scheduler = o.scheduler;
        aimingStats = o.aimingStats == null ? null : new StatCollection(o.aimingStats);
        notAimingStats = o.notAimingStats == null ? null : new StatCollection(o.notAimingStats);

        aiming = o.aiming;
        sight = o.sight;
        fireMode = o.fireMode;
    }

    public StatCollection aimingStats() { return aimingStats; }
    public StatCollection notAimingStats() { return notAimingStats; }

    public boolean aiming() { return aiming; }
    public void aiming(boolean aiming) { this.aiming = aiming; }

    public SightPath sight() { return sight; }
    public void sight(SightPath sight) { this.sight = sight; }
    public Sight getSight() { return sight == null ? null : sight.get(parent); }

    public FireModePath fireMode() { return fireMode; }
    public void fireMode(FireModePath fireMode) { this.fireMode = fireMode; }
    public FireMode getFireMode() { return fireMode == null ? null : fireMode.get(parent); }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public StatCollection buildStats() {
        StatCollection result = new StatCollection();
        for (FireModePath ref : collectFireModes()) {
            FireMode fireMode = ref.get(parent);
            if (ref.equals(this.fireMode)) {
                if (fireMode.activeStats != null)
                    result.combine(fireMode.activeStats);
            } else if (fireMode.notActiveStats != null)
                result.combine(fireMode.notActiveStats);
        }

        for (SightPath ref : collectSights()) {
            Sight sight = ref.get(parent);
            if (ref.equals(this.sight)) {
                if (sight.activeStats != null)
                    result.combine(sight.activeStats);

                if (aiming) {
                    if (sight.aimingStats != null)
                        result.combine(sight.aimingStats);
                } else if (sight.notAimingStats != null)
                    result.combine(sight.notAimingStats);
            } else if (sight.notActiveStats != null)
                result.combine(sight.notActiveStats);
        }
        return result;
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

    public List<CalibreSlot> collectChamberSlots() {
        return parent.collectSlots(SLOT_TAG_CHAMBER);
    }
    public List<ChamberSystem> collectChambers(List<CalibreSlot> slots) {
        return parent.fromSlots(slots, ChamberSystem.class);
    }

    public List<CalibreSlot> collectAmmoSlots() {
        return parent.collectSlots(SLOT_TAG_AMMO);
    }
    public List<ComponentContainerSystem> collectAmmo(List<CalibreSlot> slots) {
        return parent.fromSlots(slots, ComponentContainerSystem.class);
    }

    @Override
    public void setup(CalibreComponent<?> parent) {
        if (dependencies != null) {
            aimingStats = deserialize(dependencies.aimingStats, StatCollection.class, "aimingStats");
            notAimingStats = deserialize(dependencies.notAimingStats, StatCollection.class, "notAimingStats");
            dependencies = null;
        }
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        scheduler = parent.system(SchedulerSystem.class);
        EventDispatcher events = tree.events();
        int priority = setting("listener_priority").getInt(0);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Scroll.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Interact.class, this::onEvent, priority);
        events.registerListener(ItemEvents.SwapHand.class, this::onEvent, priority);
        events.registerListener(ItemEvents.BreakBlock.class, this::onEvent, priority);
        events.registerListener(ItemEvents.PlaceBlock.class, this::onEvent, priority);

        if (getSight() == null) {
            List<SightPath> found = collectSights();
            if (found.size() > 0)
                sight = found.get(0);
        }

        if (getFireMode() == null) {
            List<FireModePath> found = collectFireModes();
            if (found.size() > 0)
                fireMode = found.get(0);
        }
    }

    protected Component createFireModes(String locale) {
        Component separator = gen(locale, "system." + ID + ".fire_modes.separator");
        List<Component> values = new ArrayList<>();
        for (FireModePath ref : collectFireModes()) {
            values.add(gen(locale, "system." + ID + ".fire_modes." + (ref.equals(fireMode) ? "selected" : "unselected"),
                    "value", gen(locale, "fire_mode.short." + ref.get(parent).id)));
        }
        return values.size() == 0 ? gen(locale, "system." + ID + ".fire_modes.none") : Utils.join(separator, values);
    }

    protected Component createSights(String locale) {
        Component separator = gen(locale, "system." + ID + ".sights.separator");
        List<Component> values = new ArrayList<>();
        for (SightPath ref : collectSights()) {
            values.add(gen(locale, "system." + ID + ".sights." + (ref.equals(sight) ? "selected" : "unselected"),
                    "value", gen(locale, "sight.short." + ref.get(parent).id)));
        }
        return values.size() == 0 ? gen(locale, "system." + ID + ".sights.none") : Utils.join(separator, values);
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        List<Component> info = new ArrayList<>();
        String locale = event.locale();

        info.add(gen(locale, "system." + ID + ".info",
                "fire_modes", createFireModes(locale),
                "sights", createSights(locale)));

        event.item().addInfo(info);
    }

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        if (event.user() instanceof SenderUser)
            ((SenderUser) event.user()).sendInfo(Component.text()
                    .append(Component.text(
                            (getFireMode() == null ? "(no fire mode)" : getFireMode().id)
                            + " "
                            + (getSight() == null ? "(no sight)" : getSight().id)
                            + " "
                    ))
                    .append(
                            collectAmmo(collectAmmoSlots()).stream().map(sys -> Component.text(
                                    "(" + (sys.amount() + (sys instanceof CapacityComponentContainerSystem ? " / " + ((CapacityComponentContainerSystem) sys).capacity() : "")) + ")"
                            )).collect(Collectors.toList())
                    )
                    .append(Component.text(" + " + collectChambers(collectChamberSlots()).stream().count()))
                    .build());
    }

    protected <I extends Item> void onEvent(ItemEvents.Interact<I> event) {
        CalibreComponent<I> component = event.component();
        ItemUser user = event.user();
        ItemSlot<I> slot = event.slot();
        if (event.type() == ItemEvents.Interact.LEFT) {
            startFire(new Events.StartFire<>(component, user, slot, this));
        } else if (event.type() == ItemEvents.Interact.RIGHT) {
            aim(new Events.Aim<>(
                    component, user, slot, this, !aiming
            ));
        }
    }

    protected abstract void test(ItemUser user, double fov);

    protected <I extends Item> void onEvent(ItemEvents.Scroll<I> event) {
        if (!aiming)
            return;

        int length = event.length();
        ItemUser user = event.user();
        if (user instanceof SneakableUser && ((SneakableUser) user).sneaking()) {
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
        List<FireModePath> collected = collectFireModes();
        event.cancel();
        int size = collected.size();
        if (size == 0)
            return;

        ItemUser user = event.user();
        int idx = Utils.wrapIndex(size, collected.indexOf(fireMode) + (user instanceof SneakableUser && ((SneakableUser) user).sneaking() ? -1 : 1));
        changeFireMode(new Events.ChangeFireMode<>(
                event.component(), event.user(), event.slot(), this, collected.get(idx)
        ));
    }

    protected <I extends Item> void onEvent(ItemEvents.BreakBlock<I> event) {
        event.cancel();
    }

    protected <I extends Item> void onEvent(ItemEvents.PlaceBlock<I> event) {
        event.cancel();
    }

    protected Tuple3<ChamberSystem, CalibreSlot, ProjectileSystem> getProjectile(CalibreSlot chamberSlot) {
        CalibreComponent<?> component;
        ChamberSystem chamberSystem;
        CalibreSlot loadSlot;
        ProjectileSystem projectileSystem;
        if (
                (component = chamberSlot.get()) == null
                || (chamberSystem = component.system(ChamberSystem.class)) == null
                || (loadSlot = chamberSystem.getLoadSlot()) == null
                || (projectileSystem = loadSlot.<CalibreComponent<?>>get().system(ProjectileSystem.class)) == null
        ) return null;
        return Tuple3.of(chamberSystem, loadSlot, projectileSystem);
    }

    public <I extends Item> void startFire(Events.StartFire<I> event) {
        if (tree().call(event).cancelled) return;

        List<CalibreSlot> chamberSlots = collectChamberSlots();
        boolean success = false;
        for (CalibreSlot chamberSlot : chamberSlots) {
            if (getProjectile(chamberSlot) == null)
                continue;
            success = true;
            scheduler.schedule(this, tree().<NumberDescriptor.Long>stat("shot_delay").apply(), self -> self.fire(new Events.Fire<>(
                    event.component(), event.user(), event.slot(), this, chamberSlot.path()
            )));
        }

        if (!success) {
            // no loaded chambers found - chamber
            chamber(new Events.Chamber<>(
                    event.component(), event.user(), event.slot(), this
            ), chamberSlots);
        }

        tree().build();
        event.updateItem();
    }

    protected double random(double bound) {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * bound;
    }

    protected Vector3D rotate(Vector3D vec, Vector2D bound, double length) {
        ViewCoordinates view = vec
                .rotateY(random(Math.toRadians(bound.x())))
                .toViewCoordinates();
        return view
                .pitch(Math.toRadians(Math.toDegrees(view.pitch()) + random(bound.y())))
                .toVector()
                .multiply(length);
    }

    protected Tuple2<Vector3D, Vector3D> getBarrelOffset(ItemUser user, Vector3D offset) {
        Vector3D direction = user.direction();
        Vector3D position = Utils.relativeOffset(user.position(), direction, offset);
        return Tuple2.of(position, direction);
    }

    public <I extends Item> void fire(Events.Fire<I> event) {
        if (tree().call(event).cancelled) return;

        ItemUser user = event.user();
        CalibreSlot chamberSlot = parent.root().slot(event.chamberSlotPath);
        if (chamberSlot == null)
            return;
        Tuple3<ChamberSystem, CalibreSlot, ProjectileSystem> result = getProjectile(chamberSlot);
        if (result == null)
            return;
        event.chamberSystem = result.a();
        event.loadSlot = result.b();
        event.projectileSystem = result.c();

        // get barrel offset
        Tuple2<Vector3D, Vector3D> offsetData = getBarrelOffset(user, tree().<Vector3DDescriptor>stat("barrel_offset").apply());
        Vector3D position = offsetData.a();

        // get shot origin vector
        double velocity = tree().<NumberDescriptor.Double>stat("muzzle_velocity").apply();
        Vector3D direction = rotate(
                offsetData.b(),
                tree().<Vector2DDescriptor>stat("spread").apply(new Vector2D()),
                velocity
        );

        fireSuccess(new Events.FireSuccess<>(
                event.component(), event.user(), event.slot(), this, event.chamberSlotPath,
                chamberSlot, result.a(), result.b(), result.c(),
                position, direction, velocity
        ));
    }

    protected <I extends Item> void fireSuccess(Events.FireSuccess<I> event) {
        if (tree().call(event).cancelled) return;
        Vector3D position = event.position;
        Vector3D direction = event.direction;
        double velocity = event.velocity;
        CalibreSlot chamberSlot = event.chamberSlot();
        ItemUser user = event.user();

        // shoot projectiles
        Vector2D projectileSpread = tree().<Vector2DDescriptor>stat("projectile_spread").apply(new Vector2D());
        for (int i = 0; i < tree().<NumberDescriptor.Integer>stat("projectiles").apply(); i++) {
            event.projectileSystem.createProjectile(event.user(), position, rotate(direction, projectileSpread, velocity));
        }

        // apply recoil
        if (user instanceof RecoilableUser)
            ((RecoilableUser) user).applyRecoil(
                    tree().<Vector2DDescriptor>stat("recoil").apply(),
                    tree().<NumberDescriptor.Double>stat("recoil_speed").apply(),
                    tree().<NumberDescriptor.Double>stat("recoil_recovery").apply(),
                    tree().<NumberDescriptor.Double>stat("recoil_recovery_speed").apply(),
                    tree().<NumberDescriptor.Long>stat("recoil_recovery_after").apply()
            );

        // remove load - we've just shot it
        event.loadSlot.set(null);
        // eject casing if there is any (if the load =/= the casing)
        chamberSlot.set(null);

        // load new chamber from ammo
        List<ComponentContainerSystem> allAmmo = collectAmmo(collectAmmoSlots());
        if (allAmmo.size() > 0) {
            ComponentContainerSystem ammo = allAmmo.get(0);
            CalibreComponent<?> peek = ammo.peek();
            if (peek != null && chamberSlot.get() == null && chamberSlot.isCompatible(peek))
                chamberSlot.set(ammo.pop());
        }

        update(event.user(), event.slot(), event);
    }

    protected <I extends Item> void chamber(Events.Chamber<I> event, List<CalibreSlot> chamberSlots) {
        List<ComponentContainerSystem> allAmmo = collectAmmo(collectAmmoSlots());
        boolean success = false;
        for (CalibreSlot slot : chamberSlots) {
            if (slot.get() != null) continue;
            // find a compatible ammo component
            ComponentContainerSystem ammo = null;
            for (ComponentContainerSystem current : allAmmo) {
                CalibreComponent<?> peek = current.peek();
                if (peek == null || !slot.isCompatible(peek)) continue;
                ammo = current;
            }
            if (ammo == null) continue;
            slot.set(ammo.pop());
            success = true;
        }
        event.result = success ? ItemEvents.Result.SUCCESS : ItemEvents.Result.FAILURE;
    }

    public <I extends Item> void chamber(Events.Chamber<I> event) {
        chamber(event, collectChamberSlots());
    }

    public void aim(Events.Aim<?> event) {
        if (tree().call(event).cancelled) return;

        if (sight == null || aiming == event.aim) {
            event.result = ItemEvents.Result.FAILURE;
            return;
        }
        aiming = event.aim;

        event.result = ItemEvents.Result.SUCCESS;
        tree().build();
        event.updateItem();
    }

    public void changeSight(Events.ChangeSight<?> event) {
        if (tree().call(event).cancelled) return;

        if (sight != null && sight.equals(event.sight)) {
            event.result = ItemEvents.Result.FAILURE;
            return;
        }
        sight = event.sight;

        event.result = ItemEvents.Result.SUCCESS;
        tree().build();
        event.updateItem();
    }

    public void changeFireMode(Events.ChangeFireMode<?> event) {
        if (tree().call(event).cancelled) return;

        if (fireMode != null && fireMode.equals(event.fireMode)) {
            event.result = ItemEvents.Result.FAILURE;
            return;
        }
        fireMode = event.fireMode;

        event.result = ItemEvents.Result.SUCCESS;
        tree().build();
        event.updateItem();
    }

    public abstract GunSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GunSystem that = (GunSystem) o;
        return aiming == that.aiming && Objects.equals(aimingStats, that.aimingStats) && Objects.equals(notAimingStats, that.notAimingStats) && Objects.equals(sight, that.sight) && Objects.equals(fireMode, that.fireMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aimingStats, notAimingStats, aiming, sight, fireMode);
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
            private final String[] chamberSlotPath;
            private CalibreSlot chamberSlot;
            private ChamberSystem chamberSystem;
            private CalibreSlot loadSlot;
            private ProjectileSystem projectileSystem;
            private boolean cancelled;

            public Fire(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, String[] chamberSlotPath) {
                super(component, user, slot, system);
                this.chamberSlotPath = chamberSlotPath;
            }

            public String[] chamberSlotPath() { return chamberSlotPath; }

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

            public FireSuccess(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, String[] chamberSlotPath,
                               CalibreSlot chamberSlot, ChamberSystem chamberSystem, CalibreSlot loadSlot, ProjectileSystem projectileSystem,
                               Vector3D position, Vector3D direction, double velocity) {
                super(component, user, slot, system, chamberSlotPath);
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
    }
}
