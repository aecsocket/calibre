package com.gitlab.aecsocket.calibre.core.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.calibre.core.component.ComponentCompatibility;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.unifiedframework.core.component.IncompatibleComponentException;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.util.Quantifier;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@ConfigSerializable
public abstract class ComponentContainerSystem extends AbstractSystem {
    public static class Serializer implements TypeSerializer<ComponentContainerSystem> {
        private final TypeSerializer<ComponentContainerSystem> delegate;

        public Serializer(TypeSerializer<ComponentContainerSystem> delegate) {
            this.delegate = delegate;
        }

        public TypeSerializer<ComponentContainerSystem> delegate() { return delegate; }

        @Override
        public void serialize(Type type, @Nullable ComponentContainerSystem obj, ConfigurationNode node) throws SerializationException {
            delegate.serialize(type, obj, node);
            if (obj == null)
                return;
            List<Quantifier<ComponentTree>> trees = obj.components.stream()
                    .map(quantifier -> new Quantifier<>(quantifier.get().buildTree().tree(), quantifier.getAmount()))
                    .collect(Collectors.toList());
            node.node("components").set(new TypeToken<>(){}, trees);
        }

        @Override
        public ComponentContainerSystem deserialize(Type type, ConfigurationNode node) throws SerializationException {
            ComponentContainerSystem obj = delegate.deserialize(type, node);
            List<Quantifier<ComponentTree>> trees = node.node("components").getList(new TypeToken<>(){});
            if (trees != null)
                trees.forEach(quantifier -> obj.components.add(new Quantifier<>(quantifier.get().root(), quantifier.getAmount())));
            return obj;
        }
    }

    public static final String ID = "component_container";
    public static final int LISTENER_PRIORITY = 1200;
    protected transient final LinkedList<Quantifier<CalibreComponent<?>>> components;
    @Setting(nodeFromParent = true)
    @FromMaster protected ComponentCompatibility compatibility;

    /**
     * Used for registration + deserialization.
     */
    public ComponentContainerSystem() {
        super(LISTENER_PRIORITY);
        components = new LinkedList<>();
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public ComponentContainerSystem(ComponentContainerSystem o) {
        super(o);
        components = new LinkedList<>();
        compatibility = o.compatibility;
    }

    @Override public String id() { return ID; }

    public LinkedList<Quantifier<CalibreComponent<?>>> components() { return new LinkedList<>(components); }

    public ComponentCompatibility compatibility() { return compatibility; }
    public void compatibility(ComponentCompatibility compatibility) { this.compatibility = compatibility; }

    public boolean accepts(CalibreComponent<?> component) {
        return compatibility.applies(component);
    }

    public void push(Quantifier<CalibreComponent<?>> quantifier) {
        Quantifier<CalibreComponent<?>> last = components.peekLast();
        CalibreComponent<?> component = quantifier.get();
        if (!accepts(component))
            throw new IncompatibleComponentException("Component [" + component + "] is not compatible with this system");
        if (last != null && last.get().equals(component))
            last.add(quantifier.getAmount());
        else
            components.add(quantifier);
    }
    public void push(CalibreComponent<?> component, int amount) { push(new Quantifier<>(component, amount)); }
    public void push(CalibreComponent<?> component) { push(component, 1); }

    public CalibreComponent<?> peek() {
        Quantifier<CalibreComponent<?>> last = components.peekLast();
        return last == null ? null : last.get();
    }

    public CalibreComponent<?> pop() {
        Quantifier<CalibreComponent<?>> last = components.peekLast();
        if (last == null)
            return null;
        last.add(-1);
        if (last.getAmount() <= 0)
            components.removeLast();
        return last.get().copy().buildTree();
    }

    public int amount() { return Quantifier.amount(components); }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.NameCreate.class, this::onEvent, listenerPriority);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.ItemClick.class, this::onEvent, listenerPriority);
    }

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        event.result(gen(event.locale(), "system." + ID + ".component_name",
                "name", event.result(),
                "amount", Integer.toString(amount())));
    }

    protected Component writeTotal(Locale locale, int total) {
        return gen(locale, "system." + ID + ".total",
                "total", Integer.toString(total));
    }

    protected void onEvent(CalibreComponent.Events.ItemCreate<?> event) {
        List<Component> info = new ArrayList<>();
        Locale locale = event.locale();

        int total = 0;
        for (Quantifier<CalibreComponent<?>> quantifier : components) {
            total += quantifier.getAmount();
            info.add(gen(locale, "system." + ID + ".entry",
                    "name", quantifier.get().name(locale),
                    "amount", Integer.toString(quantifier.getAmount())));
        }
        info.add(writeTotal(locale, total));

        event.item().addInfo(info);
    }

    protected <I extends Item> int amountToInsert(I rawCursor, CalibreComponent<I> cursor, boolean shiftClick) {
        return shiftClick ? 1 : rawCursor.amount();
    }

    protected <I extends Item> void remove(ItemEvents.ItemClick<I> event, Quantifier<CalibreComponent<I>> last) {}
    protected <I extends Item> void insert(ItemEvents.ItemClick<I> event, int amount, I rawCursor, CalibreComponent<I> cursor) {}

    protected <I extends Item> void onEvent(ItemEvents.ItemClick<I> event) {
        if (event.slot().get().amount() > 1)
            return;

        Locale locale = event.user().locale();
        I rawCursor = event.cursor().get();
        if (event.rightClick()) {
            if (!event.shiftClick() || rawCursor != null)
                return;
            @SuppressWarnings("unchecked")
            Quantifier<CalibreComponent<I>> last = (Quantifier<CalibreComponent<I>>) (Quantifier<?>) components.peekLast();
            if (last != null) {
                I contained = last.get().create(locale, last.getAmount());
                event.cursor().set(contained);
                last.add(-contained.amount());
                if (last.getAmount() <= 0)
                    components.removeLast();
                remove(event, last);
            }
        } else {
            CalibreComponent<I> cursor = event.component().getComponent(rawCursor);
            if (cursor == null)
                return;
            if (accepts(cursor)) {
                int amount = amountToInsert(rawCursor, cursor, event.shiftClick());
                if (amount > 0) {
                    push(cursor, amount);
                    rawCursor.subtract(amount);
                    event.cursor().set(rawCursor);
                    insert(event, amount, rawCursor, cursor);
                }
            }
        }

        event.cancel();
        update(event);
    }

    protected abstract ComponentContainerSystem partialCopy();

    @Override
    public ComponentContainerSystem copy() {
        ComponentContainerSystem sys = partialCopy();
        sys.components.addAll(components);
        return sys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentContainerSystem that = (ComponentContainerSystem) o;
        return components.equals(that.components) && compatibility.equals(that.compatibility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(components, compatibility);
    }
}