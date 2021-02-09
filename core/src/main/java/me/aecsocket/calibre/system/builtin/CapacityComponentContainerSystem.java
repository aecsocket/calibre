package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.world.Item;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Objects;

@ConfigSerializable
public abstract class CapacityComponentContainerSystem extends ComponentContainerSystem {
    public static final String ID = "capacity_component_container";
    @FromMaster protected int capacity;

    /**
     * Used for registration + deserialization.
     */
    public CapacityComponentContainerSystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public CapacityComponentContainerSystem(CapacityComponentContainerSystem o) {
        super(o);
        capacity = o.capacity;
    }

    @Override public String id() { return ID; }

    public int capacity() { return capacity; }
    public void capacity(int capacity) { this.capacity = capacity; }

    public int remaining() { return capacity - amount(); }

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        event.result(gen(event.locale(), "system." + ID + ".component_name",
                "name", event.result(),
                "amount", Integer.toString(amount()),
                "capacity", Integer.toString(capacity)));
    }

    @Override
    protected Component writeTotal(String locale, int total) {
        return gen(locale, "system." + ID + ".total",
                "total", Integer.toString(total),
                "capacity", Integer.toString(capacity));
    }

    @Override
    protected <I extends Item> int amountToInsert(I rawCursor, CalibreComponent<I> cursor, boolean shiftClick) {
        return Math.min(remaining(), super.amountToInsert(rawCursor, cursor, shiftClick));
    }

    @Override public abstract CapacityComponentContainerSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CapacityComponentContainerSystem that = (CapacityComponentContainerSystem) o;
        return capacity == that.capacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), capacity);
    }
}
