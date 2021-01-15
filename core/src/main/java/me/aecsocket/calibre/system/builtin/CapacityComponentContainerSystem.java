package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.world.Item;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Objects;

@ConfigSerializable
public abstract class CapacityComponentContainerSystem<I extends Item> extends ComponentContainerSystem<I> {
    public static final String ID = "capacity_component_container";
    @FromParent
    protected int capacity;

    public CapacityComponentContainerSystem() {}

    public CapacityComponentContainerSystem(ComponentContainerSystem<I> o, int capacity) {
        super(o);
        this.capacity = capacity;
    }

    public CapacityComponentContainerSystem(CapacityComponentContainerSystem<I> o) {
        this(o, o.capacity);
    }

    @Override public String id() { return ID; }

    public int capacity() { return capacity; }
    public void capacity(int capacity) { this.capacity = capacity; }

    public int remaining() { return capacity - amount(); }

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        event.result(localize(event.locale(), "system." + ID + ".component_name",
                "name", event.result(),
                "amount", Integer.toString(amount()),
                "capacity", Integer.toString(capacity)));
    }

    @Override
    protected Component writeTotal(String locale, int total) {
        return localize(locale, "system.capacity_component_container.total",
                "total", Integer.toString(total),
                "capacity", Integer.toString(capacity));
    }

    @Override
    protected int amountToInsert(I rawCursor, CalibreComponent<I> cursor, boolean shiftClick) {
        return Math.min(remaining(), super.amountToInsert(rawCursor, cursor, shiftClick));
    }

    public abstract CapacityComponentContainerSystem<I> copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CapacityComponentContainerSystem<?> that = (CapacityComponentContainerSystem<?>) o;
        return capacity == that.capacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), capacity);
    }
}
