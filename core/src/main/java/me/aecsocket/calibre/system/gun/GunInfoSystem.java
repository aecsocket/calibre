package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.system.builtin.CapacityComponentContainerSystem;
import me.aecsocket.calibre.system.builtin.ComponentContainerSystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.world.user.SenderUser;
import me.aecsocket.calibre.world.user.StabilizableUser;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public abstract class GunInfoSystem extends AbstractSystem {
    public enum Style {
        NUMBER,
        ICONS
    }

    public static final String ID = "gun_info";
    public static final int LISTENER_PRIORITY = 100;

    @FromMaster protected Style ammoStyle;
    @FromMaster protected Style chamberStyle;
    @FromMaster protected boolean showAllFireModes;
    @FromMaster protected boolean showAllSights;
    @FromMaster protected boolean strict;
    @FromMaster protected boolean showStamina;
    protected transient GunSystem gun;

    /**
     * Used for registration + deserialization.
     */
    public GunInfoSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public GunInfoSystem(GunInfoSystem o) {
        super(o);
        gun = o.gun;
        ammoStyle = o.ammoStyle;
        chamberStyle = o.chamberStyle;
        showAllFireModes = o.showAllFireModes;
        showAllSights = o.showAllSights;
        strict = o.strict;
        showStamina = o.showStamina;
    }

    @Override public String id() { return ID; }

    public Style ammoStyle() { return ammoStyle; }
    public void ammoStyle(Style ammoStyle) { this.ammoStyle = ammoStyle; }

    public Style chamberStyle() { return chamberStyle; }
    public void chamberStyle(Style chamberStyle) { this.chamberStyle = chamberStyle; }

    public boolean showAllFireModes() { return showAllFireModes; }
    public void showAllFireModes(boolean showAllFireModes) { this.showAllFireModes = showAllFireModes; }

    public boolean showAllSights() { return showAllSights; }
    public void showAllSights(boolean showAllSights) { this.showAllSights = showAllSights; }

    public boolean strict() { return strict; }
    public void strict(boolean strict) { this.strict = strict; }

    public boolean showStamina() { return showStamina; }
    public void showStamina(boolean showStamina) { this.showStamina = showStamina; }

    public GunSystem gun() { return gun; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(GunSystem.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, listenerPriority);

        gun = require(GunSystem.class);
    }

    public Component genFor(String locale, CalibreComponent<?> component) {
        CalibreSlot loadSlot = gun.getProjectile(component).b();
        return gen(locale, "system." + ID + ".component." +
                (loadSlot == null || loadSlot.get() == null ? component.id() : loadSlot.<CalibreComponent<?>>get().id()));
    }

    protected abstract Component bar(String locale, String key, double percent);

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        String locale = event.user().locale();
        ItemUser user = event.user();
        if (user instanceof SenderUser) {
            FireMode fireMode = gun.getFireMode();
            Sight sight = gun.getSight();

            List<Component> ammo = new ArrayList<>();
            for (ComponentContainerSystem sys : gun.collectAmmo(gun.collectAmmoSlots())) {
                switch (ammoStyle) {
                    case NUMBER:
                        if (sys instanceof CapacityComponentContainerSystem)
                            ammo.add(gen(locale, "system." + ID + ".ammo.capacity",
                                    "amount", Integer.toString(sys.amount()),
                                    "capacity", Integer.toString(((CapacityComponentContainerSystem) sys).capacity())));
                        else
                            ammo.add(gen(locale, "system." + ID + ".ammo.no_capacity",
                                    "amount", Integer.toString(sys.amount())));
                        break;
                    case ICONS:
                        TextComponent.Builder builder = Component.text();
                        sys.components().forEach(quant -> {
                            Component gen = genFor(locale, quant.get());
                            builder.append(Utils.repeat(gen, quant.getAmount()));
                        });
                        if (sys instanceof CapacityComponentContainerSystem) {
                            Component gen = genFor(locale, sys.parent());
                            builder.append(Utils.repeat(gen, ((CapacityComponentContainerSystem) sys).remaining()));
                        }
                        ammo.add(builder.build());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown ammo style " + ammoStyle);
                }
            }

            Component chamber;
            switch (chamberStyle) {
                case NUMBER:
                    chamber = Component.text(gun.collectChamberSlots().stream()
                            .filter(slot -> slot.get() != null && (!strict || gun.getProjectile(slot).c() != null))
                            .count());
                    break;
                case ICONS:
                    TextComponent.Builder builder = Component.text();
                    gun.collectChamberSlots()
                            .forEach(slot -> {
                                if (slot.get() != null && (!strict || gun.getProjectile(slot).c() != null))
                                    builder.append(genFor(locale, slot.get()));
                                else
                                    builder.append(gen(locale, "system." + ID + ".chamber.empty"));
                            });
                    chamber = builder.build();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown chamber style " + chamberStyle);
            }

            Component fireModeGen;
            if (fireMode == null)
                fireModeGen = gen(locale, "system." + ID + ".no_fire_mode");
            else {
                if (showAllFireModes) {
                    List<Component> components = new ArrayList<>();
                    gun.collectFireModes().forEach(path -> {
                        FireMode currentMode = path.get(parent);
                        components.add(gen(locale, "system." + ID + ".fire_mode." + (fireMode.equals(currentMode) ? "selected" : "unselected"),
                                "value", gen(locale, "system." + ID + ".fire_mode.value." + currentMode.id)
                        ));
                    });
                    fireModeGen = Utils.join(gen(locale, "system." + ID + ".fire_mode.separator"), components);
                } else
                    fireModeGen = gen(locale, "system." + ID + ".fire_mode.value." + fireMode.id);
            }

            Component sightGen;
            if (sight == null)
                sightGen = gen(locale, "system." + ID + ".no_sight");
            else {
                if (showAllSights) {
                    List<Component> components = new ArrayList<>();
                    gun.collectSights().forEach(path -> {
                        Sight currentSight = path.get(parent);
                        components.add(gen(locale, "system." + ID + ".sight." + (sight.equals(currentSight) ? "selected" : "unselected"),
                                "value", gen(locale, "system." + ID + ".sight.value." + currentSight.id)
                        ));
                    });
                    sightGen = Utils.join(gen(locale, "system." + ID + ".sight.separator"), components);
                } else
                    sightGen = gen(locale, "system." + ID + ".sight.value." + sight.id);
            }

            if (showStamina && user instanceof StabilizableUser) {
                StabilizableUser stabilizable = (StabilizableUser) user;
                ((SenderUser) user).sendInfo(gen(locale, "system." + ID + ".action_bar.stamina",
                        "stamina", bar(locale, "system." + ID + ".action_bar.stamina_bar",
                                stabilizable.stamina() / stabilizable.maxStamina()),
                        "ammo", Utils.join(gen(locale, "system." + ID + ".ammo.separator"), ammo),
                        "chamber", chamber,
                        "fire_mode", fireModeGen,
                        "sight", sightGen
                ));
            } else {
                ((SenderUser) user).sendInfo(gen(locale, "system." + ID + ".action_bar.no_stamina",
                        "ammo", Utils.join(gen(locale, "system." + ID + ".ammo.separator"), ammo),
                        "chamber", chamber,
                        "fire_mode", fireModeGen,
                        "sight", sightGen
                ));
            }
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Switch<I> event) {
        if (event.cancelled())
            return;
        ItemUser user = event.user();
        if (user instanceof SenderUser)
            ((SenderUser) user).sendInfo(Component.text(""));
    }

    public abstract GunInfoSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
