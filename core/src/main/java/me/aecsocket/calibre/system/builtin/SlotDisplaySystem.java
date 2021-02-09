package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.component.Slot;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.util.Utils;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SlotDisplaySystem extends AbstractSystem {
    public static final String ID = "slot_display";
    public static final int LISTENER_PRIORITY = 1100;

    /**
     * Used for registration + deserialization.
     */
    public SlotDisplaySystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public SlotDisplaySystem(SlotDisplaySystem o) {
        super(o);
    }

    @Override public String id() { return ID; }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority);
    }

    protected String slotType(CalibreSlot slot) {
        return slot.required() ? "required" : "normal";
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        String locale = event.locale();
        List<Component> info = new ArrayList<>();
        Component paddingStart = gen(locale, "system." + ID + ".padding.start");
        Component paddingMiddle = gen(locale, "system." + ID + ".padding.middle");
        Component paddingEnd = gen(locale, "system." + ID + ".padding.end");

        Map<String, Component> generatedKeys = new HashMap<>();
        parent.walk(data -> {
            Slot raw = data.slot();
            if (!(raw instanceof CalibreSlot))
                return;
            CalibreSlot slot = (CalibreSlot) raw;
            String slotType = slotType(slot);
            int depth = data.depth();
            info.add(gen(locale, "system." + ID + ".entry",
                    "pad", depth == 0 ? Component.empty() : paddingStart.append(Utils.repeat(paddingMiddle, depth)).append(paddingEnd),
                    "key", gen(locale, "system." + ID + ".slot_type." + slotType,
                            "key", generatedKeys.computeIfAbsent(data.path()[depth], k -> gen(locale, "slot." + k))),
                    "slot", slot.get() == null
                            ? gen(locale, "system." + ID + ".empty")
                            // use `.copy().buildTree()` so that the component pretends it's the root
                            : slot.<CalibreComponent<?>>get().copy().buildTree().name(locale)));
        });

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    @Override public abstract SlotDisplaySystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
