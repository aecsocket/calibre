package me.aecsocket.calibre.system.builtin;

import com.google.protobuf.Any;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.proto.system.SystemsBuiltin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperSchedulerSystem extends SchedulerSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     * @param scheduler The scheduler used to schedule system tasks.
     */
    public PaperSchedulerSystem(CalibrePlugin plugin, Scheduler scheduler) {
        super(scheduler);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperSchedulerSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperSchedulerSystem(PaperSchedulerSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.UpdateItem.class, this::onEvent, 0);
    }

    protected <I extends Item> void onEvent(ItemEvents.UpdateItem<I> event) {
        if (event.cause() instanceof ItemEvents.Equipped && event.item() instanceof BukkitItem) {
            plugin.itemManager().hide(((BukkitItem) event.item()).item(), true);
        }
    }

    @Override public PaperSchedulerSystem copy() { return new PaperSchedulerSystem(this); }

    @Override
    public Any writeProtobuf() {
        return Any.pack(SystemsBuiltin.SchedulerSystem.newBuilder()
                .setAvailableAt(availableAt)
                .addAllTasks(tasks)
                .build());
    }

    @Override
    public PaperSchedulerSystem readProtobuf(Any raw) {
        SystemsBuiltin.SchedulerSystem msg = unpack(raw, SystemsBuiltin.SchedulerSystem.class);
        PaperSchedulerSystem sys = new PaperSchedulerSystem(plugin, scheduler);
        sys.availableAt = msg.getAvailableAt();
        sys.tasks.addAll(msg.getTasksList());
        return sys;
    }
}
