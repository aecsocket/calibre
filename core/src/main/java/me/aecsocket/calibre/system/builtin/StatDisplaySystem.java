package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.DisplayableStat;
import me.aecsocket.unifiedframework.stat.StatInstance;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.util.data.Tuple2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class StatDisplaySystem extends AbstractSystem {
    public static final String ID = "stat_display";
    private static final Map<Class<?>, StatRenderer> renderers = new HashMap<>();

    public StatDisplaySystem() {}

    public StatDisplaySystem(StatDisplaySystem o) {
        super(o);
    }

    public static Map<Class<?>, StatRenderer> renderers() { return new HashMap<>(renderers); }
    public static <T> StatRenderer renderer(Class<T> type) { return renderers.get(type); }
    public static <T> void renderer(Class<T> type, StatRenderer displayer) { renderers.put(type, displayer); }

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

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        String locale = event.locale();
        List<Component> info = new ArrayList<>();

        Map<Integer, StatMap> stats;
        if (tree().complete())
            stats = Map.of(0, tree().stats());
        else
            stats = parent.stats().ordered();

        // Generate key components and widths
        // Gets widest key in *all priorities*
        AtomicInteger columnWidth = new AtomicInteger(0);
        Map<String, Tuple2<Component, Integer>> generatedKeys = new LinkedHashMap<>();
        stats.values().forEach(map -> map.forEach((key, inst) -> {
            // If...
            //   - not displayable
            //   - empty
            // ...skip it
            if (!(inst.stat() instanceof DisplayableStat) || inst.empty())
                return;
            DisplayableStat<?> stat = (DisplayableStat<?>) inst.stat();

            // If stat hidden, skip it
            if (stat.hidden())
                return;

            // If we've already generated data for this key, skip it
            if (generatedKeys.containsKey(key))
                return;

            // If we don't have a way to display the value of this key, skip it
            if (!renderers.containsKey(inst.get().getClass())) return;

            Component generatedKey = localize(locale, "stat." + key);
            int keyWidth = getWidth(PlainComponentSerializer.plain().serialize(generatedKey));
            generatedKeys.put(key, Tuple2.of(generatedKey, keyWidth));
            if (keyWidth > columnWidth.get())
                columnWidth.set(keyWidth);
        }));

        stats.forEach((priority, map) -> {
            // Maps with priority below 0 are considered "internal maps" e.g. they store the item representation of a component
            if (priority < 0)
                return;

            List<Component> section = new ArrayList<>();

            // Generate value components
            generatedKeys.forEach((key, data) -> {
                StatInstance<?> inst = map.get(key);
                // If this stat key is not in this map, skip it
                if (inst == null || inst.empty())
                    return;

                Component generatedKey = data.getA();
                int keyWidth = data.getB();

                section.add(localize(locale, "system." + ID + ".value." + key,
                        "key", generatedKey,
                        "pad", pad(locale, columnWidth.get() - keyWidth),
                        "value", renderers.get(inst.get().getClass()).create(inst, locale, key)));
            });

            // Add section header
            if (section.size() > 0 && priority != 0)
                section.add(0, localize(locale, "system." + ID + ".header",
                        "priority", Integer.toString(priority)));

            info.addAll(section);
        });

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    protected abstract int getWidth(String text);
    protected abstract String pad(String locale, int width);

    public abstract StatDisplaySystem copy();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderers);
    }
}
