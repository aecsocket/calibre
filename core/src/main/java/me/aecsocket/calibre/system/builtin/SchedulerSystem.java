package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.*;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.data.Tuple2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

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
        protected final Map<Integer, Task<?>> tasks = new HashMap<>();
        protected int taskId;
        protected long cleanDelay;
        protected long cleanThreshold;

        protected long nextClean;

        public Scheduler(long cleanDelay, long cleanThreshold) {
            this.cleanDelay = cleanDelay;
            this.cleanThreshold = cleanThreshold;
        }

        public Map<Integer, Task<?>> tasks() { return tasks; }
        public int taskId() { return taskId; }
        public int nextTaskId() { return ++taskId; }

        public long cleanDelay() { return cleanDelay; }
        public void cleanDelay(long cleanDelay) { this.cleanDelay = cleanDelay; }

        public long cleanThreshold() { return cleanThreshold; }
        public void cleanThreshold(long cleanThreshold) { this.cleanThreshold = cleanThreshold; }

        public <S extends CalibreSystem> Tuple2<Integer, Task<S>> schedule(S system, long delay, Consumer<S> function) {
            Task<S> task = new Task<>(system.parent().path(), system.id(), System.currentTimeMillis() + delay, function);
            int id = nextTaskId();
            tasks.put(id, task);
            return Tuple2.of(id, task);
        }
        public Task<?> unschedule(int id) { return tasks.remove(id); }

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

    public static class Task<S extends CalibreSystem> {
        private final String[] path;
        private final String systemId;
        private final long runAt;
        private final Consumer<S> function;

        public Task(String[] path, String systemId, long runAt, Consumer<S> function) {
            this.path = path;
            this.systemId = systemId;
            this.runAt = runAt;
            this.function = function;
        }

        public String[] path() { return path; }
        public String systemId() { return systemId; }
        public long runAt() { return runAt; }
        public Consumer<S> function() { return function; }
    }

    public static final String ID = "scheduler";
    public static final int LISTENER_PRIORITY = 100000;

    @FromMaster(fromDefault = true)
    private final transient Scheduler scheduler;
    private final List<Integer> tasks = new ArrayList<>();
    private long availableAt;

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

    public <S extends CalibreSystem> Tuple2<Integer, Task<S>> schedule(S system, long delay, Consumer<S> function) {
        Tuple2<Integer, Task<S>> result = scheduler.schedule(system, delay, function);
        tasks.add(result.a());
        return result;
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, listenerPriority);
    }

    public boolean checkTasks() {
        int before = tasks.size();
        tasks.removeIf(id -> {
            Task<?> task = scheduler.tasks.get(id);
            if (task == null)
                return true;
            if (System.currentTimeMillis() < task.runAt)
                return false;
            runTask(task);
            return true;
        });
        return before != tasks.size();
    }

    public <S extends CalibreSystem> void runTask(Task<S> task) {
        CalibreComponent<?> targetComp = parent.root().component(task.path);
        if (targetComp == null)
            return;
        S system = targetComp.system(task.systemId);
        if (system == null)
            return;
        task.function.accept(system);
    }

    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        if (checkTasks())
            update(event);
    }

    protected <I extends Item> void onEvent(ItemEvents.Switch<I> event) {
        if (event.position() == ItemEvents.Switch.TO)
            return;

        if (event.cancelled())
            return;
        tasks.forEach(scheduler.tasks::remove);
        tasks.clear();
        update(event);
    }

    public abstract SchedulerSystem copy();

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
