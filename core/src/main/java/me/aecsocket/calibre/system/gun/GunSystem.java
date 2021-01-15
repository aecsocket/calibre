package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.builtin.ProjectileSystem;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.ItemSlot;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.world.SenderUser;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.DoubleDescriptorStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.Vector2DDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.descriptor.DoubleDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector2DDescriptor;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import me.aecsocket.unifiedframework.util.vector.ViewCoordinates;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class GunSystem extends AbstractSystem {
    public static final String ID = "gun";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("muzzle_velocity", new DoubleDescriptorStat(0).format("0"))
            .init("recoil", new Vector2DDescriptorStat(new Vector2D()).format("0.00"))
            .init("projectiles", NumberStat.of(1))

            .init("spread", new Vector2DDescriptorStat(new Vector2D()).format("0.00"))
            .init("projectile_spread", new Vector2DDescriptorStat(new Vector2D()).format("0.00"))
            .get();
    public static final String SLOT_TAG_CHAMBER = "chamber";
    public static final String AMMO_TAG_CHAMBER = "ammo";

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode aimingStats;
        protected ConfigurationNode notAimingStats;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;
    @FromParent
    protected transient StatCollection aimingStats;
    @FromParent
    protected transient StatCollection notAimingStats;

    protected boolean aiming;
    protected SightRef sight;
    protected FireModeRef fireMode;

    public GunSystem() {
        aimingStats = new StatCollection();
        notAimingStats = new StatCollection();
    }

    public GunSystem(GunSystem o) {
        super(o);
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

    public SightRef sight() { return sight; }
    public void sight(SightRef sight) { this.sight = sight; } 
    public Sight getSight() { return sight == null ? null : sight.get(parent); }

    public FireModeRef fireMode() { return fireMode; }
    public void fireMode(FireModeRef fireMode) { this.fireMode = fireMode; }
    public FireMode getFireMode() { return fireMode == null ? null : fireMode.get(parent); }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public StatCollection buildStats() {
        StatCollection result = new StatCollection();
        for (FireModeRef ref : collectFireModes()) {
            FireMode fireMode = ref.get(parent);
            if (ref.equals(this.fireMode)) {
                if (fireMode.activeStats != null)
                    result.combine(fireMode.activeStats);
            } else if (fireMode.notActiveStats != null)
                result.combine(fireMode.notActiveStats);
        }

        for (SightRef ref : collectSights()) {
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

    private <T extends ContainerRef<?>, S extends CalibreSystem> List<T> collect(Class<S> systemType, Function<S, List<?>> listGetter, BiFunction<String[], Integer, T> provider) {
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

    public List<SightRef> collectSights() {
        return this.collect(SightSystem.class, sys -> sys.sights, SightRef::new);
    }

    public List<FireModeRef> collectFireModes() {
        return this.collect(FireModeSystem.class, sys -> sys.fireModes, FireModeRef::new);
    }

    public List<CalibreSlot> collectChamberSlots() {
        return parent.collectSlots(SLOT_TAG_CHAMBER);
    }

    public List<ChamberSystem> collectChambers(List<CalibreSlot> slots) {
        return parent.fromSlots(slots, ChamberSystem.class);
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

        EventDispatcher events = tree.events();
        int priority = listenerPriority();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Interact.class, this::onEvent, priority);
        events.registerListener(ItemEvents.SwapHand.class, this::onEvent, priority);

        if (getSight() == null) {
            List<SightRef> found = collectSights();
            if (found.size() > 0)
                sight = found.get(0);
        }

        if (getFireMode() == null) {
            List<FireModeRef> found = collectFireModes();
            if (found.size() > 0)
                fireMode = found.get(0);
        }
    }

    protected abstract int listenerPriority();

    protected Component createFireModes(String locale) {
        Component separator = localize(locale, "system." + ID + ".fire_modes.separator");
        List<Component> values = new ArrayList<>();
        for (FireModeRef ref : collectFireModes()) {
            values.add(localize(locale, "system." + ID + ".fire_modes." + (ref.equals(fireMode) ? "selected" : "unselected"),
                    "value", localize(locale, "fire_mode.short." + ref.get(parent).id)));
        }
        return values.size() == 0 ? localize(locale, "system." + ID + ".fire_modes.none") : Utils.join(separator, values);
    }

    protected Component createSights(String locale) {
        Component separator = localize(locale, "system." + ID + ".sights.separator");
        List<Component> values = new ArrayList<>();
        for (SightRef ref : collectSights()) {
            values.add(localize(locale, "system." + ID + ".sights." + (ref.equals(sight) ? "selected" : "unselected"),
                    "value", localize(locale, "sight.short." + ref.get(parent).id)));
        }
        return values.size() == 0 ? localize(locale, "system." + ID + ".sights.none") : Utils.join(separator, values);
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        List<Component> info = new ArrayList<>();
        String locale = event.locale();

        info.add(localize(locale, "system." + ID + ".info",
                "fire_modes", createFireModes(locale),
                "sights", createSights(locale)));

        event.item().addInfo(info);
    }

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        if (event.user() instanceof SenderUser)
            ((SenderUser) event.user()).sendInfo(Component.text(
                    parent.id() + " " +
                            sight + "=" + getSight() + "#" + collectSights() + " " +
                            fireMode + "=" + getFireMode() + "#" + collectFireModes()
            ));
    }

    protected <I extends Item> void onEvent(ItemEvents.Interact<I> event) {
        CalibreComponent<I> component = event.component();
        ItemUser user = event.user();
        ItemSlot<I> slot = event.slot();
        if (event.type() == ItemEvents.Interact.LEFT) {
            fire(new Events.Fire<>(component, user, slot, this));
        } else if (event.type() == ItemEvents.Interact.RIGHT) {
            List<SightRef> collected = collectSights();
            if (user.sneaking() && aiming && collected.size() > 1) {
                changeSight(new Events.ChangeSight<>(
                        component, user, slot, this, collected.get((collected.indexOf(sight) + 1) % collected.size())
                ));
            } else if (getSight() != null) {
                aim(new Events.Aim<>(
                        component, user, slot, this, !aiming
                ));
            }
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.SwapHand<I> event) {
        List<FireModeRef> collected = collectFireModes();
        event.cancel();
        if (collected.size() > 1) {
            changeFireMode(new Events.ChangeFireMode<>(
                    event.component(), event.user(), event.slot(), this, collected.get((collected.indexOf(fireMode) + 1) % collected.size())
                    ));
        }
    }

    public void fire(Events.Fire<?> event) {
        if (tree().call(event).cancelled) return;

        List<CalibreSlot> chamberSlots = collectChamberSlots();
        AtomicBoolean found = new AtomicBoolean(false);
        for (CalibreSlot chamberSlot : chamberSlots) {
            CalibreComponent<?> component = chamberSlot.get();
            if (component == null) continue;
            ChamberSystem chamberSystem = component.system(ChamberSystem.class);
            if (chamberSystem == null) continue;
            ProjectileSystem projectileSystem = chamberSystem.getProjectile();
            if (projectileSystem == null) continue;

            found.set(true);

            fireSuccess(event, chamberSlot, chamberSystem, projectileSystem);
        }

        if (!found.get()) {
            // No chambers found - chamber
            chamber(event.user());
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

    protected void fireSuccess(Events.Fire<?> event, CalibreSlot chamberSlot, ChamberSystem chamberSystem, ProjectileSystem projectileSystem) {
        ItemUser user = event.user();
        Vector3D position = user.position();
        double length = tree().<DoubleDescriptor>stat("muzzle_velocity").apply(0d);
        Vector3D velocity = rotate(
                user.direction(),
                tree().<Vector2DDescriptor>stat("spread").apply(new Vector2D()),
                length
        );
        Vector2D projectileSpread = tree().<Vector2DDescriptor>stat("projectile_spread").apply(new Vector2D());
        for (int i = 0; i < tree().<Integer>stat("projectiles"); i++) {
            projectileSystem.createProjectile(user, position, rotate(velocity, projectileSpread, length));
        }
    }

    public void chamber(ItemUser user) {
        ((SenderUser) user).sendMessage(Component.text("chamber"));
    }

    public void aim(Events.Aim<?> event) {
        if (tree().call(event).cancelled) return;

        if (sight == null)
            return;
        if (aiming == event.aim)
            return;
        aiming = event.aim;

        tree().build();
        event.updateItem();
    }

    public void changeSight(Events.ChangeSight<?> event) {
        if (tree().call(event).cancelled) return;

        if (sight != null && sight.equals(event.sight))
            return;
        sight = event.sight;

        tree().build();
        event.updateItem();
    }

    public void changeFireMode(Events.ChangeFireMode<?> event) {
        if (tree().call(event).cancelled) return;

        if (fireMode != null && fireMode.equals(event.fireMode))
            return;
        fireMode = event.fireMode;

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

        public static class SystemItemEvent<I extends Item> extends ItemEvents.SystemItemEvent<I, GunSystem> {
            public SystemItemEvent(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system) {
                super(component, user, slot, system);
            }
        }

        public static class Fire<I extends Item> extends SystemItemEvent<I> implements Cancellable {
            private boolean cancelled;

            public Fire(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system) {
                super(component, user, slot, system);
            }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class Aim<I extends Item> extends SystemItemEvent<I> implements Cancellable {
            private final boolean aim;
            private boolean cancelled;

            public Aim(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, boolean aim) {
                super(component, user, slot, system);
                this.aim = aim;
            }

            public boolean aim() { return aim; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class ChangeSight<I extends Item> extends SystemItemEvent<I> implements Cancellable {
            private final SightRef sight;
            private boolean cancelled;

            public ChangeSight(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, SightRef sight) {
                super(component, user, slot, system);
                this.sight = sight;
            }

            public SightRef sight() { return sight; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class ChangeFireMode<I extends Item> extends SystemItemEvent<I> implements Cancellable {
            private final FireModeRef fireMode;
            private boolean cancelled;

            public ChangeFireMode(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, GunSystem system, FireModeRef fireMode) {
                super(component, user, slot, system);
                this.fireMode = fireMode;
            }

            public FireModeRef fireMode() { return fireMode; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }
    }
}
