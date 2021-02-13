package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.MinecraftSyncLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.data.Tuple2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public abstract class SchedulerSystem extends AbstractSystem {
    public static class Serializer implements TypeSerializer<SchedulerSystem> {
        private final TypeSerializer<SchedulerSystem> delegate;

        public Serializer(TypeSerializer<SchedulerSystem> delegate) {
            this.delegate = delegate;
        }

        public TypeSerializer<SchedulerSystem> delegate() { return delegate; }

        @Override
        public void serialize(Type type, @Nullable SchedulerSystem obj, ConfigurationNode node) throws SerializationException {
            delegate.serialize(type, obj, node);
            if (System.currentTimeMillis() > obj.availableAt)
                node.removeChild("available_at");
            if (obj.tasks.size() == 0)
                node.removeChild("tasks");
        }

        @Override
        public SchedulerSystem deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return delegate.deserialize(type, node);
        }
    }

    public static class Scheduler implements Tickable {
        protected final Map<Integer, Task<?, ?>> tasks = new HashMap<>();
        protected int taskId;
        protected long cleanDelay;
        protected long cleanThreshold;

        protected long nextClean;

        public Scheduler(long cleanDelay, long cleanThreshold) {
            this.cleanDelay = cleanDelay;
            this.cleanThreshold = cleanThreshold;
        }

        public Map<Integer, Task<?, ?>> tasks() { return tasks; }
        public int taskId() { return taskId; }
        public int nextTaskId() { return ++taskId; }

        public long cleanDelay() { return cleanDelay; }
        public void cleanDelay(long cleanDelay) { this.cleanDelay = cleanDelay; }

        public long cleanThreshold() { return cleanThreshold; }
        public void cleanThreshold(long cleanThreshold) { this.cleanThreshold = cleanThreshold; }

        public <S extends CalibreSystem, I extends Item> Tuple2<Integer, Task<S, I>> schedule(S system, long delay, TaskFunction<S, I> function) {
            Task<S, I> task = new Task<>(system.parent().path(), system.id(), System.currentTimeMillis() + delay, function);
            int id = nextTaskId();
            tasks.put(id, task);
            return Tuple2.of(id, task);
        }
        public Task<?, ?> unschedule(int id) { return tasks.remove(id); }

        public void clean() {
            long time = System.currentTimeMillis();
            tasks.entrySet().removeIf(entry -> time > entry.getValue().runAt + cleanThreshold);
        }

        @Override
        public void tick(TickContext tickContext) {
            nextClean += tickContext.delta();
            if (nextClean > cleanDelay) {
                clean();
                nextClean %= Math.max(1, cleanDelay);
            }
        }
    }

    public interface TreeContext {
        <C extends CalibreComponent<?>> C component(C original);
        <S extends CalibreSlot> S slot(S original);
        <S extends CalibreSystem> S system(S original);
    }

    public interface TaskFunction<S extends CalibreSystem, I extends Item> {
        void run(S self, ItemEvents.Equipped<I> equip, TreeContext ctx);
    }

    public static class Task<S extends CalibreSystem, I extends Item> {
        private final String[] path;
        private final String systemId;
        private final long runAt;
        private final TaskFunction<S, I> function;

        public Task(String[] path, String systemId, long runAt, TaskFunction<S, I> function) {
            this.path = path;
            this.systemId = systemId;
            this.runAt = runAt;
            this.function = function;
        }

        public String[] path() { return path; }
        public String systemId() { return systemId; }
        public long runAt() { return runAt; }
        public TaskFunction<S, I> function() { return function; }
    }

    public static final String ID = "scheduler";
    public static final int LISTENER_PRIORITY = 100000;

    @FromMaster(fromDefault = true)
    protected final transient Scheduler scheduler;
    protected final List<Integer> tasks = new ArrayList<>();
    protected long availableAt;

    /**
     * Used for registration.
     * @param scheduler The scheduler to use.
     */
    public SchedulerSystem(Scheduler scheduler) {
        super(LISTENER_PRIORITY);
        this.scheduler = scheduler;
    }

    /**
     * Used for deserialization.
     */
    public SchedulerSystem() {
        super(LISTENER_PRIORITY);
        scheduler = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public SchedulerSystem(SchedulerSystem o) {
        super(o);
        scheduler = o.scheduler;
    }

    @Override public String id() { return ID; }

    public List<Integer> tasks() { return tasks; }

    public long availableAt() { return availableAt; }
    public void availableAt(long availableAt) { this.availableAt = availableAt; }

    public boolean available() { return System.currentTimeMillis() >= availableAt; }
    public void delay(long ms) { availableAt = System.currentTimeMillis() + ms; }

    public <S extends CalibreSystem, I extends Item> Tuple2<Integer, Task<S, I>> schedule(S system, long delay, TaskFunction<S, I> function) {
        Tuple2<Integer, Task<S, I>> result = scheduler.schedule(system, delay, function);
        tasks.add(result.a());
        return result;
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);

        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Death.class, this::onEvent, listenerPriority);
    }

    public boolean checkTasks(ItemEvents.Equipped<?> equip) {
        List<Integer> removed = new ArrayList<>();
        int tasksSize = tasks.size();
        new ArrayList<>(tasks).forEach(id -> {
            Task<?, ?> task = scheduler.tasks.get(id);
            if (task == null) {
                removed.add(id);
                return;
            }
            if (System.currentTimeMillis() < task.runAt) {
                return;
            }

            runTask(task, equip);
            removed.add(id);
        });
        removed.forEach(tasks::remove);
        // if any new tasks are scheduled, OR any tasks need to be removed, then update
        return tasksSize != tasks.size() || removed.size() > 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S extends CalibreSystem, I extends Item> void runTask(Task<S, I> task, ItemEvents.Equipped<?> equip) {
        CalibreComponent<?> root = parent.root();
        CalibreComponent<?> targetComp = root.component(task.path);
        if (targetComp == null)
            return;
        S targetSystem = targetComp.system(task.systemId);
        if (targetSystem == null)
            return;
        ((TaskFunction) task.function).run(targetSystem, equip, new TreeContext() {
            @Override
            public <C extends CalibreComponent<?>> C component(C original) {
                return root.component(original.path());
            }

            @Override
            public <T extends CalibreSlot> T slot(T original) {
                return root.slot(original.path());
            }

            @Override
            public <T extends CalibreSystem> T system(T original) {
                CalibreComponent<?> childComp = component(original.parent());
                return childComp == null ? null : childComp.system(original.id());
            }
        });
    }

    public void clearTasks() {
        tasks.forEach(scheduler.tasks::remove);
        tasks.clear();
    }

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        if (event.tickContext().loop() instanceof MinecraftSyncLoop) {
            if (checkTasks(event))
                update(event);
        }
    }

    protected <I extends Item> void onEvent(ItemEvents.Switch<I> event) {
        if (event.position() == ItemEvents.Switch.TO)
            return;

        if (event.cancelled())
            return;
        clearTasks();
        update(event);
    }

    protected <I extends Item> void onEvent(ItemEvents.Death<I> event) {
        clearTasks();
        update(event);
    }

    @Override public abstract SchedulerSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulerSystem that = (SchedulerSystem) o;
        return tasks.equals(that.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tasks);
    }
}
