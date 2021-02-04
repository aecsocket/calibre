package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.StatRenderer;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.StatInstance;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.util.data.Tuple2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;
import java.util.function.Function;

public abstract class StatDisplaySystem extends AbstractSystem implements StatRenderer {
    @ConfigSerializable
    public static final class Element {
        private final String stat;
        @Setting(nodeFromParent = true)
        private final ConfigurationNode config;

        public Element(String stat, ConfigurationNode config) {
            this.stat = stat;
            this.config = config;
        }

        public Element() {
            stat = null;
            config = null;
        }

        public String stat() { return stat; }
        public ConfigurationNode config() { return config; }

        @Override public String toString() { return stat; }
    }

    public static final String ID = "stat_display";
    @FromMaster
    private List<List<Element>> sections;
    @FromMaster(fromDefault = true)
    private transient Function<Class<?>, Formatter<?>> formatSupplier;

    /**
     * Used for registration.
     * @param formatSupplier The function which generates formatters for specific stat types.
     */
    public StatDisplaySystem(Function<Class<?>, Formatter<?>> formatSupplier) {
        this.formatSupplier = formatSupplier;
    }

    /**
     * Used for deserialization.
     */
    public StatDisplaySystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public StatDisplaySystem(StatDisplaySystem o) {
        super(o);
        sections = new ArrayList<>(o.sections);
        formatSupplier = o.formatSupplier;
    }

    @Override public String id() { return ID; }

    public List<List<Element>> sections() { return sections; }
    public void sections(List<List<Element>> sections) { this.sections = sections; }

    public Function<Class<?>, Formatter<?>> formatSupplier() { return formatSupplier; }
    public void formatSupplier(Function<Class<?>, Formatter<?>> formatSupplier) { this.formatSupplier = formatSupplier; }
    @SuppressWarnings("unchecked")
    public <T> Formatter<T> formatter(Class<T> type) { return (Formatter<T>) formatSupplier.apply(type); }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        int priority = listenerPriority(100);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);
    }

    protected abstract int getWidth(String text);
    protected abstract String pad(String locale, int width);

    protected boolean drawn(StatInstance<?> inst) {
        if (inst.empty())
            return false;
        return true;
    }

    @Override
    public List<Component> createInfo(String locale, Map<Integer, StatMap> stats, Component prefix) {
        List<Component> info = new ArrayList<>();
        if (sections == null)
            return info;

        // Get a map of all the keys which *will* be drawn from the sections, and their configs
        Map<String, Element> drawnKeys = new HashMap<>();
        sections.forEach(elements -> elements.forEach(element -> drawnKeys.put(element.stat, element)));

        // Generate key components and widths
        // Gets widest key in *all priorities*
        int columnWidth = 0;
        Map<String, Tuple2<Component, Integer>> generatedKeys = new LinkedHashMap<>();
        for (Map.Entry<Integer, StatMap> entry : stats.entrySet()) {
            for (Map.Entry<String, StatInstance<?>> entry2 : entry.getValue().entrySet()) {
                String key = entry2.getKey();
                StatInstance<?> inst = entry2.getValue();

                // If we've already generated data for this key, skip it
                if (generatedKeys.containsKey(key))
                    continue;

                Element element = drawnKeys.get(key);
                // If this is not in any drawn element, skip it
                if (element == null)
                    continue;

                // If we don't draw this stat instance, skip it
                if (!drawn(entry2.getValue()))
                    continue;

                Component generatedKey = generateKey(locale, inst, element);
                int keyWidth = getWidth(PlainComponentSerializer.plain().serialize(generatedKey));
                generatedKeys.put(key, Tuple2.of(generatedKey, keyWidth));
                if (keyWidth > columnWidth)
                    columnWidth = keyWidth;
            }
        }
        int fColumnWidth = columnWidth;

        /*
        Priority 1:                                  }
          Damage: 3            ] element  } section  }
          Muzzle velocity: 100 ] element  }          }
                                                     } priority
          Recoil: 5            ] element  } section  }
          Spread: 10           ] element  }          }
         */

        Component sectionSeparator = gen(locale, "system." + ID + ".section_separator");
        int i = 0;
        for (Map.Entry<Integer, StatMap> entry : stats.entrySet()) {
            int priority = entry.getKey();
            StatMap priorityStats = entry.getValue();

            List<List<Component>> sectionsInfo = new ArrayList<>();

            sections.forEach(section -> {
                List<Component> sectionInfo = new ArrayList<>();
                for (Element element : section) {
                    String key = element.stat;
                    StatInstance<?> inst = priorityStats.get(key);

                    // If we don't draw this stat instance, skip it
                    if (inst == null || !drawn(inst))
                        continue;

                    Tuple2<Component, Integer> generated = generatedKeys.get(key);
                    // If we don't have any generated data for this key, skip it
                    if (generated == null)
                        continue;

                    Component line = addElement(locale, drawnKeys.get(key), inst, fColumnWidth, generated);
                    if (line != null)
                        sectionInfo.add(prefix.append(line));
                }
                if (sectionInfo.size() > 0)
                    sectionsInfo.add(sectionInfo);
            });

            // Join sections using separator
            List<Component> priorityInfo = new ArrayList<>();
            for (int j = 0; j < sectionsInfo.size(); j++) {
                priorityInfo.addAll(sectionsInfo.get(j));
                if (j < sectionsInfo.size() - 1)
                    priorityInfo.add(sectionSeparator);
            }

            // Add priority header
            if (stats.size() > 1 && priorityInfo.size() > 0) {
                if (priority != 0)
                    priorityInfo.add(0, prefix.append(gen(locale, "system." + ID + ".header." + (priority >= 0 ? "ascending" : "descending"),
                            "priority", Integer.toString(priority >= 0 ? priority : -priority))));
                if (i > 0)
                    priorityInfo.add(0, prefix.append(gen(locale, "system." + ID + ".priority_separator")));
            }

            if (priorityInfo.size() > 0) {
                info.addAll(priorityInfo);
                ++i;
            }
        }

        return info;
    }

    protected <T> Component addElement(String locale, Element element, StatInstance<T> inst, int columnWidth, Tuple2<Component, Integer> generated) {
        @SuppressWarnings("unchecked")
        Formatter<T> formatter = (Formatter<T>) formatter(inst.get().getClass());
        if (formatter == null)
            return null;
        return gen(locale, "system." + ID + ".value",
                "pad", pad(locale, columnWidth - generated.b()),
                "key", generated.a(),
                "value", formatter.format(locale, inst, element));
    }

    protected <T> Component generateKey(String locale, StatInstance<T> inst, Element element) {
        @SuppressWarnings("unchecked")
        Formatter<T> formatter = (Formatter<T>) formatter(inst.get().getClass());
        if (formatter != null) {
            Component component = formatter.key(locale, inst, element);
            if (component != null)
                return component;
        }
        return gen(locale, "stat.key." + element.stat);
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        List<Component> info = createInfo(event.locale(),
                tree().complete()
                        ? Map.of(0, tree().stats())
                        : parent.stats().ordered(),
                Component.text("")
        );

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    public abstract StatDisplaySystem copy();

    @Override
    public String toString() { return super.toString() + " " + formatSupplier; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
