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

    @FromMaster
    protected Style ammoStyle;
    @FromMaster
    protected Style chamberStyle;
    @FromMaster
    protected boolean strict;
    protected transient GunSystem gun;

    /**
     * Used for registration + deserialization.
     */
    public GunInfoSystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public GunInfoSystem(GunInfoSystem o) {
        super(o);
        gun = o.gun;
        ammoStyle = o.ammoStyle;
        chamberStyle = o.chamberStyle;
        strict = o.strict;
    }

    @Override public String id() { return ID; }

    public Style ammoStyle() { return ammoStyle; }
    public void ammoStyle(Style ammoStyle) { this.ammoStyle = ammoStyle; }

    public Style chamberStyle() { return chamberStyle; }
    public void chamberStyle(Style chamberStyle) { this.chamberStyle = chamberStyle; }

    public boolean strict() { return strict; }
    public void strict(boolean strict) { this.strict = strict; }

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
        int priority = listenerPriority(0);
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, priority);

        gun = require(GunSystem.class);
    }

    public Component genFor(String locale, CalibreSlot slot) {
        CalibreSlot loadSlot = gun.getProjectile(slot).b();
        return gen(locale, "system." + ID + ".chamber." +
                (loadSlot == null || loadSlot.get() == null ? slot.<CalibreComponent<?>>get().id() : loadSlot.<CalibreComponent<?>>get().id()));
    }

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
                    gun.collectChamberSlots().stream()
                            .filter(slot -> slot.get() != null && (!strict || gun.getProjectile(slot).c() != null))
                            .forEach(slot -> builder.append(genFor(locale, slot)));
                    chamber = builder.build();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown chamber style " + chamberStyle);
            }

            ((SenderUser) user).sendInfo(gen(locale, "system." + ID + ".action_bar",
                    "ammo", Utils.join(Component.text(" . "), ammo),
                    "chamber", chamber,
                    "fire_mode", gen(locale, fireMode == null ? "system." + ID + ".no_fire_mode" : "fire_mode.short." + fireMode.id),
                    "sight", gen(locale, sight == null ? "system." + ID + ".no_sight" : "sight.short." + sight.id)));
            /*((SenderUser) user).sendInfo(Component.text()
                    .append(gun.getFireMode() == null ? Component.text("") : gen(locale, "fire_mode.short." + gun.getFireMode().id))
                    .append(Component.text(" "))
                    .append(gun.getSight() == null ? Component.text("") : gen(locale, "sight.short." + gun.getSight().id))
                    .append(Component.text(" "))
                    .append(
                            gun.collectAmmo(gun.collectAmmoSlots()).stream().map(sys -> Component.text(
                                    "(" + (sys.amount() + (sys instanceof CapacityComponentContainerSystem ? " / " + ((CapacityComponentContainerSystem) sys).capacity() : "")) + ")"
                            )).collect(Collectors.toList())
                    )
                    .append(Component.text(" + " + gun.collectChambers(gun.collectChamberSlots()).stream().count()))
                    .build());*/
        }
    }

    public abstract GunInfoSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
