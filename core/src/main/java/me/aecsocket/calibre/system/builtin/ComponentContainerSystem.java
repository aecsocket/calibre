package me.aecsocket.calibre.system.builtin;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentCompatibility;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.util.Quantifier;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ConfigSerializable
public abstract class ComponentContainerSystem<I extends Item> extends AbstractSystem {
    public static class Serializer implements TypeSerializer<ComponentContainerSystem<?>> {
        private final TypeSerializer<ComponentContainerSystem<?>> delegate;

        public Serializer(TypeSerializer<ComponentContainerSystem<?>> delegate) {
            this.delegate = delegate;
        }

        public TypeSerializer<ComponentContainerSystem<?>> delegate() { return delegate; }

        @Override
        public void serialize(Type type, @Nullable ComponentContainerSystem<?> obj, ConfigurationNode node) throws SerializationException {
            delegate.serialize(type, obj, node);
            if (obj == null)
                return;
            List<Quantifier<ComponentTree>> trees = obj.components.stream()
                    .map(quantifier -> new Quantifier<>(quantifier.get().tree(), quantifier.getAmount()))
                    .collect(Collectors.toList());
            node.node("components").set(new TypeToken<>(){}, trees);
        }

        @Override
        public ComponentContainerSystem<?> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            ComponentContainerSystem<?> obj = delegate.deserialize(type, node);
            List<Quantifier<ComponentTree>> trees = node.node("components").getList(new TypeToken<>(){});
            if (trees != null)
                trees.forEach(quantifier -> obj.components.add(new Quantifier<>(quantifier.get().root(), quantifier.getAmount())));
            return obj;
        }
    }

    public static final String ID = "component_container";
    protected transient final LinkedList<Quantifier<CalibreComponent<I>>> components;
    @Setting(nodeFromParent = true)
    protected ComponentCompatibility compatibility;

    public ComponentContainerSystem() {
        components = new LinkedList<>();
    }

    public ComponentContainerSystem(ComponentContainerSystem<I> o) {
        super(o);
        components = o.components == null ? null : new LinkedList<>(o.components);
        compatibility = o.compatibility;
    }

    public LinkedList<Quantifier<CalibreComponent<I>>> components() { return new LinkedList<>(components); }

    public ComponentCompatibility compatibility() { return compatibility; }
    public void compatibility(ComponentCompatibility compatibility) { this.compatibility = compatibility; }

    public void add(Quantifier<CalibreComponent<I>> quantifier) {
        Quantifier<CalibreComponent<I>> last = components.peekLast();
        if (last != null && last.get().equals(quantifier.get()))
            last.add(quantifier.getAmount());
        else
            components.add(quantifier);
    }
    public void add(CalibreComponent<I> component, int amount) { add(new Quantifier<>(component, amount)); }
    public void add(CalibreComponent<I> component) { add(component, 1); }

    public int amount() { return Quantifier.amount(components); }

    public void removeLast() {
        Quantifier<CalibreComponent<I>> last = components.peekLast();
        if (last == null)
            return;
        last.add(-1);
        if (last.getAmount() <= 0)
            components.removeLast();
    }

    @Override public String id() { return ID; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        int priority = listenerPriority();
        events.registerListener(CalibreComponent.Events.NameCreate.class, this::onEvent, priority);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Click.class, this::onEvent, priority);
    }

    protected abstract int listenerPriority();

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        event.result(localize(event.locale(), "system." + ID + ".component_name",
                "name", event.result(),
                "amount", Integer.toString(amount())));
    }

    protected Component writeTotal(String locale, int total) {
        return localize(locale, "system.component_container.total",
                "total", Integer.toString(total));
    }

    protected void onEvent(CalibreComponent.Events.ItemCreate<?> event) {
        List<Component> info = new ArrayList<>();
        String locale = event.locale();

        int total = 0;
        for (Quantifier<CalibreComponent<I>> quantifier : components) {
            total += quantifier.getAmount();
            info.add(localize(locale, "system.component_container.entry",
                    "name", quantifier.get().name(locale),
                    "amount", Integer.toString(quantifier.getAmount())));
        }
        info.add(writeTotal(locale, total));

        event.item().addInfo(info);
    }

    protected boolean canInsert(I rawCursor, CalibreComponent<I> cursor) { return true; }

    protected int amountToInsert(I rawCursor, CalibreComponent<I> cursor) {
        return rawCursor.amount();
    }

    protected void onEvent(ItemEvents.Click<I> event) {
        if (event.slot().get().amount() > 1)
            return;

        /*
              right-click       : slots
        sneak right-click       : take out stack
              left-click  cursor: put in stack
        sneak left-click  cursor: put in single

         */

        I rawCursor = event.cursor().get();
        if (event.rightClick()) {
            if (!event.shiftClick() || rawCursor != null)
                return;
            Quantifier<CalibreComponent<I>> last = components.peekLast();
            if (last != null) {
                I contained = last.get().create(event.user().locale(), last.getAmount());
                event.cursor().set(contained);
                components.removeLast();
            }
        } else {
            CalibreComponent<I> cursor = event.component().getComponent(rawCursor);
            if (cursor == null)
                return;
            if (compatibility.applies(cursor)) {
                if (canInsert(rawCursor, cursor)) {
                    if (event.shiftClick()) {
                        add(cursor);
                        rawCursor.subtract();
                        event.cursor().set(rawCursor);
                    } else {
                        int amount = amountToInsert(rawCursor, cursor);
                        if (amount > 0) {
                            add(cursor, amount);
                            rawCursor.subtract(amount);
                            event.cursor().set(rawCursor);
                        }
                    }
                }
            }
        }

        event.cancel();
        event.updateItem(this);
    }

    public abstract ComponentContainerSystem<I> copy();

    @Override
    public void inherit(CalibreSystem child) {
        if (!(child instanceof ComponentContainerSystem)) return;
        @SuppressWarnings("unchecked")
        ComponentContainerSystem<I> other = (ComponentContainerSystem<I>) child;
        other.compatibility = compatibility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentContainerSystem<?> that = (ComponentContainerSystem<?>) o;
        return components.equals(that.components) && compatibility.equals(that.compatibility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(components, compatibility);
    }
}
