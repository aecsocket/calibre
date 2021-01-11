package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.ItemSlot;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.world.SenderUser;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.DoubleDescriptorStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.Vector2DDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class GunSystem extends AbstractSystem {
    public static final String ID = "gun";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("damage", new DoubleDescriptorStat(0).min(0d).max(20d).format("0.0"))
            .init("muzzle_velocity", new DoubleDescriptorStat(0).format("0"))
            .init("recoil", new Vector2DDescriptorStat(new Vector2D()))
            .get();

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode aimingStats;
        protected ConfigurationNode notAimingStats;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;
    protected transient StatCollection aimingStats;
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

    private <T extends ContainerRef<?>, S extends CalibreSystem> List<T> collect(Class<S> systemType, Function<S, List<?>> listGetter, BiFunction<String[], Integer, T> provider) {
        List<T> results = new ArrayList<>();

        parent.<CalibreComponent<Item>>forWalkAndThis((component, path) -> {
            S system = component.system(systemType);
            if (system != null) {
                List<?> containers = listGetter.apply(system);
                for (int i = 0; i < containers.size(); i++) {
                    int fi = i;
                    containers.forEach(container -> results.add(provider.apply(path, fi)));
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

    @Override
    public void setup(CalibreComponent<?> parent) {
        if (dependencies != null) {
            aimingStats = deserializeStats(dependencies.aimingStats, "aimingStats");
            notAimingStats = deserializeStats(dependencies.notAimingStats, "notAimingStats");
            dependencies = null;
        }
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.Interact.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, 0);

        if (sight == null) {
            List<SightRef> found = collectSights();
            if (found.size() > 0)
                sight = found.get(0);
        }

        if (fireMode != null) {
            List<FireModeRef> found = collectFireModes();
            if (found.size() > 0)
                fireMode = found.get(0);
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Interact<I> event) {
        CalibreComponent<I> component = event.component();
        ItemUser user = event.user();
        ItemSlot<I> slot = event.slot();
        if (event.type() == ItemEvents.Interact.LEFT) {
            fire(new Events.Fire<>(component, user, slot, this));
        } else if (event.type() == ItemEvents.Interact.RIGHT) {
            if (user.sneaking() && aiming && collectSights().size() > 1) {
                List<SightRef> collected = collectSights();
                changeSight(new Events.ChangeSight<>(
                        component, user, slot, this, collected.get((collected.indexOf(sight) + 1) % collected.size()
                )));
            } else if (getSight() != null) {
                aim(new Events.Aim<>(
                        component, user, slot, this, !aiming
                ));
            }
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        if (event.user() instanceof SenderUser)
            ((SenderUser) event.user()).sendInfo(Component.text(
                    parent.id() + " " +
                            sight + "=" + getSight() + "#" + collectSights() + " " +
                            fireMode + "=" + getFireMode() + "#" + collectFireModes()
            ));
    }

    public void fire(Events.Fire<?> event) {
        if (tree().call(event).cancelled) return;
        event.updateItem();
    }

    public void aim(Events.Aim<?> event) {
        if (tree().call(event).cancelled) return;
        if (sight == null)
            return;
        if (aiming == event.aim)
            return;
        aiming = event.aim;
        event.updateItem();
    }

    public void changeSight(Events.ChangeSight<?> event) {
        if (tree().call(event).cancelled) return;
        if (sight == null || sight.equals(event.sight))
            return;
        sight = event.sight;
        event.updateItem();
    }

    public abstract GunSystem copy();

    @Override
    public void inherit(CalibreSystem child) {
        if (!(child instanceof GunSystem)) return;
        GunSystem other = (GunSystem) child;
        other.aimingStats = aimingStats;
        other.notAimingStats = notAimingStats;
    }

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
    }
}
