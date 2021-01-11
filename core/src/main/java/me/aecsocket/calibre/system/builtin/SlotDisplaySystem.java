package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.component.Slot;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SlotDisplaySystem extends AbstractSystem {
    public static final String ID = "slot_display";

    public SlotDisplaySystem() {}

    public SlotDisplaySystem(SlotDisplaySystem o) {
        super(o);
    }

    @Override public String id() { return ID; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority());
    }

    protected abstract int listenerPriority();
    protected String slotType(CalibreSlot slot) {
        return slot.required() ? "required" : "normal";
    }

    private Component repeat(Component component, int amount) {
        TextComponent.Builder result = Component.text();
        for (int i = 0; i < amount; i++)
            result.append(component);
        return result.build();
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        String locale = event.locale();
        List<Component> info = new ArrayList<>();
        Component paddingStart = localize(locale, "system." + ID + ".padding.start");
        Component paddingMiddle = localize(locale, "system." + ID + ".padding.middle");
        Component paddingEnd = localize(locale, "system." + ID + ".padding.end");

        Map<String, Component> generatedKeys = new HashMap<>();
        parent.walk(data -> {
            Slot raw = data.slot();
            if (!(raw instanceof CalibreSlot))
                return;
            CalibreSlot slot = (CalibreSlot) raw;
            String slotType = slotType(slot);
            int depth = data.depth();
            try {
                info.add(localize(locale, "system." + ID + ".entry",
                        "pad", depth == 0 ? Component.empty() : paddingStart.append(repeat(paddingMiddle, depth)).append(paddingEnd),
                        "key", localize(locale, "system." + ID + ".slot_type." + slotType,
                                "key", generatedKeys.computeIfAbsent(data.path()[depth], k -> localize(locale, "slot." + k))),
                        "slot", slot.get() == null
                                ? localize(locale, "system." + ID + ".empty")
                                // use `.copy().buildTree()` so that the component pretends it's the root
                                : slot.<CalibreComponent<?>>get().copy().buildTree().name(locale)));
            } catch (SerializationException ignore) {}
        });

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    public abstract SlotDisplaySystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }
}
