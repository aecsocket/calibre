package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.slot.ItemSlot;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.vector.Vector3I;

public final class ItemEvents {
    private ItemEvents() {}

    public enum Result {
        SUCCESS,
        FAILURE,
        NONE
    }

    public interface ItemEvent<I extends Item> {
        CalibreComponent<I> component();
        ItemUser user();
        ItemSlot<I> slot();
    }

    public interface SystemEvent<I extends Item, S extends CalibreSystem> extends ItemEvent<I> {
        S system();

        default void updateItem() {
            system().update(user(), slot(), this);
        }
    }

    public static class Base<I extends Item> implements ItemEvent<I> {
        private final CalibreComponent<I> component;
        private final ItemUser user;
        private final ItemSlot<I> slot;

        public Base(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot) {
            this.component = component;
            this.user = user;
            this.slot = slot;
        }

        @Override public CalibreComponent<I> component() { return component; }
        @Override public ItemUser user() { return user; }
        @Override public ItemSlot<I> slot() { return slot; }
    }

    // implementations

    public interface UpdateItem<I extends Item> extends ItemEvent<I> {
        Object cause();
        I item();
    }

    public interface Equipped<I extends Item> extends ItemEvent<I> {
        TickContext tickContext();
    }

    public interface Jump<I extends Item> extends ItemEvent<I>, Cancellable {}

    public interface ToggleSprint<I extends Item> extends ItemEvent<I> {
        boolean sprinting();
    }

    public interface Scroll<I extends Item> extends ItemEvent<I> {
        int length();
    }

    public interface Switch<I extends Item> extends ItemEvent<I>, Cancellable {
        int FROM = 0;
        int TO = 1;

        int position();
    }

    public interface Interact<I extends Item> extends ItemEvent<I>, Cancellable {
        int LEFT = 0;
        int RIGHT = 1;

        int type();
    }

    public interface SwapHand<I extends Item> extends ItemEvent<I>, Cancellable {
        ItemSlot<I> offhand();
    }

    public interface Drop<I extends Item> extends ItemEvent<I>, Cancellable {}

    public interface Click<I extends Item> extends ItemEvent<I>, Cancellable {
        ItemSlot<I> cursor();
        boolean leftClick();
        boolean rightClick();
        boolean shiftClick();
    }

    public interface BreakBlock<I extends Item> extends ItemEvent<I>, Cancellable {
        Vector3I blockPosition();
    }

    public interface PlaceBlock<I extends Item> extends ItemEvent<I>, Cancellable {}

    public interface Death<I extends Item> extends ItemEvent<I> {}
}
