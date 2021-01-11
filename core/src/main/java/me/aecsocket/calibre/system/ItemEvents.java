package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.ItemSlot;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.unifiedframework.event.Cancellable;

public final class ItemEvents {
    private ItemEvents() {}

    public interface AbstractItemEvent<I extends Item> {
        CalibreComponent<I> component();
        ItemUser user();
        ItemSlot<I> slot();

        default void updateItem(CalibreComponent<I> component) {
            slot().set(component.<CalibreComponent<I>>root().create(user().locale(), slot().get().amount()));
        }

        @SuppressWarnings("unchecked")
        default void updateItem(CalibreSystem system) { updateItem((CalibreComponent<I>) system.parent()); }
    }

    public static class ItemEvent<I extends Item> implements AbstractItemEvent<I> {
        private final CalibreComponent<I> component;
        private final ItemUser user;
        private final ItemSlot<I> slot;

        public ItemEvent(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot) {
            this.component = component;
            this.user = user;
            this.slot = slot;
        }

        @Override public CalibreComponent<I> component() { return component; }
        @Override public ItemUser user() { return user; }
        @Override public ItemSlot<I> slot() { return slot; }
    }

    public interface SystemEvent<I extends Item, S extends CalibreSystem> extends AbstractItemEvent<I> {
        S system();

        default void updateItem() { updateItem(system()); }
    }

    public static class SystemItemEvent<I extends Item, S extends CalibreSystem> extends ItemEvent<I> implements SystemEvent<I, S> {
        private final S system;

        public SystemItemEvent(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, S system) {
            super(component, user, slot);
            this.system = system;
        }

        @Override public S system() { return system; }
    }


    public static class Equipped<I extends Item> extends ItemEvent<I> {
        public Equipped(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot) {
            super(component, user, slot);
        }
    }

    public static class Interact<I extends Item> extends ItemEvent<I> implements Cancellable {
        public static final int LEFT = 0;
        public static final int RIGHT = 1;

        private final int type;
        private boolean cancelled;

        public Interact(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, int type) {
            super(component, user, slot);
            this.type = type;
        }

        public int type() { return type; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancel() { cancelled = true; }
    }

    public static class Click<I extends Item> extends ItemEvent<I> implements Cancellable {
        private final ItemSlot<I> cursor;
        private final boolean leftClick;
        private final boolean rightClick;
        private final boolean shiftClick;
        private boolean cancelled;

        public Click(CalibreComponent<I> component, ItemUser user, ItemSlot<I> slot, ItemSlot<I> cursor, boolean leftClick, boolean rightClick, boolean shiftClick) {
            super(component, user, slot);
            this.cursor = cursor;
            this.leftClick = leftClick;
            this.rightClick = rightClick;
            this.shiftClick = shiftClick;
        }

        public ItemSlot<I> cursor() { return cursor; }
        public boolean leftClick() { return leftClick; }
        public boolean rightClick() { return rightClick; }
        public boolean shiftClick() { return shiftClick; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancel() { cancelled = true; }
    }
}
