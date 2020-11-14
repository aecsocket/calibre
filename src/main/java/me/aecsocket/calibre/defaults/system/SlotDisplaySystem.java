package me.aecsocket.calibre.defaults.system;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

public class SlotDisplaySystem extends BaseSystem {
    public static final String ID = "slot_display";
    public static final int LISTENER_PRIORITY = 1000;

    public SlotDisplaySystem(CalibrePlugin plugin) {
        super(plugin);
    }
    public SlotDisplaySystem() { this(null); }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemSystem.Events.SectionCreate.class, this::onEvent, LISTENER_PRIORITY);
    }

    @Override
    public Collection<Class<? extends CalibreSystem>> getServiceTypes() {
        return Collections.singleton(SlotDisplaySystem.class);
    }

    private void onEvent(ItemSystem.Events.SectionCreate event) {
        if (!parent.isRoot()) return;
        Player player = event.getPlayer();
        String prefix = plugin.gen(player, "system.slot_display.prefix");
        StringJoiner lore = new StringJoiner("\n");
        parent.walk(data -> {
            if (!(data.getSlot() instanceof CalibreComponentSlot)) return;
            CalibreComponentSlot slot = (CalibreComponentSlot) data.getSlot();
            AtomicReference<CalibreComponent> component = new AtomicReference<>();
            data.getComponent().ifPresent(o -> {
                if (o instanceof CalibreComponent)
                    component.set((CalibreComponent) o);
            });
            lore.add(
                    plugin.gen(player, "system.slot_display.line",
                            "prefix", prefix.repeat(data.getDepth() - 1),
                            "slot", slot.getName(plugin, data.getLastNode(), plugin.locale(player)),
                            "component", plugin.gen(player, "system.slot_display.component." + (
                                    data.getComponent().isEmpty()
                                    ? "empty"
                                    : component.get() == null
                                    ? "unknown"
                                    : "normal"),
                                    "name", component.get() == null ? "null" : component.get().getLocalizedName(player)
                            )
                    )
            );
        });
        event.getSections().add(lore.toString());
    }

    @Override public String getId() { return ID; }
    @Override public SlotDisplaySystem clone() { return (SlotDisplaySystem) super.clone(); }
    @Override public SlotDisplaySystem copy() { return clone(); }
}
