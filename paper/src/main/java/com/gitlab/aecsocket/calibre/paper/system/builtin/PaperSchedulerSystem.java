package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.proto.system.SystemsBuiltin;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.core.world.Item;
import com.google.protobuf.Any;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
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

    @Override public PaperSchedulerSystem copy() {
        PaperSchedulerSystem sys = new PaperSchedulerSystem(this);
        sys.availableAt = availableAt;
        sys.tasks.addAll(tasks);
        return sys;
    }

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
