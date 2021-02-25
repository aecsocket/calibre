package com.gitlab.aecsocket.calibre.core.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.world.item.FillableItem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.world.user.SenderUser;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Locale;
import java.util.Objects;

@ConfigSerializable
public abstract class CapacityComponentContainerSystem extends ComponentContainerSystem {
    public static final String ID = "capacity_component_container";
    @FromMaster protected int capacity;
    @FromMaster protected boolean showFilled = true;

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
        showFilled = o.showFilled;
    }

    @Override public String id() { return ID; }

    public int capacity() { return capacity; }
    public void capacity(int capacity) { this.capacity = capacity; }

    public boolean showFilled() { return showFilled; }
    public void showFilled(boolean showFilled) { this.showFilled = showFilled; }

    public int remaining() { return capacity - amount(); }
    public double filled() { return (double) amount() / capacity; }

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        event.result(gen(event.locale(), "system." + ID + ".component_name",
                "name", event.result(),
                "amount", Integer.toString(amount()),
                "capacity", Integer.toString(capacity)));
    }

    @Override
    protected void onEvent(CalibreComponent.Events.ItemCreate<?> event) {
        super.onEvent(event);
        if (showFilled && event.item() instanceof FillableItem) {
            ((FillableItem) event.item()).fill(filled());
        }
    }

    @Override
    protected Component writeTotal(Locale locale, int total) {
        return gen(locale, "system." + ID + ".total",
                "total", Integer.toString(total),
                "capacity", Integer.toString(capacity));
    }

    @Override
    public boolean accepts(CalibreComponent<?> component) {
        return super.accepts(component) && remaining() > 0;
    }

    @Override
    protected <I extends Item> int amountToInsert(I rawCursor, CalibreComponent<I> cursor, boolean shiftClick) {
        return Math.min(remaining(), super.amountToInsert(rawCursor, cursor, shiftClick));
    }

    @Override protected abstract CapacityComponentContainerSystem partialCopy();
    @Override public CapacityComponentContainerSystem copy() { return (CapacityComponentContainerSystem) super.copy(); }

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
