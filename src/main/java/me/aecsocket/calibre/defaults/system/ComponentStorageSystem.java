package me.aecsocket.calibre.defaults.system;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentCompatibility;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.StringJoiner;

public class ComponentStorageSystem extends BaseSystem implements ComponentProviderSystem, Cloneable {
    public static class Adapter implements TypeAdapterFactory, JsonAdapter {
        private final CalibrePlugin plugin;

        public Adapter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!ComponentStorageSystem.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    JsonObject tree = assertObject(delegate.toJsonTree(value));
                    tree.add("components", gson.toJsonTree(
                            ((ComponentStorageSystem) value).toTreeList(),
                            new TypeToken<LinkedList<Quantifier<ComponentTree>>>(){}.getType()
                    ));
                    Streams.write(tree, out);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonObject tree = assertObject(Streams.parse(in));
                    T result = delegate.fromJsonTree(tree);
                    ((ComponentStorageSystem) result).setComponents(
                            tree.has("components")
                            ? ComponentStorageSystem.fromTreeList(gson.fromJson(
                                    get(tree, "components"), new TypeToken<LinkedList<Quantifier<ComponentTree>>>(){}.getType()
                            ))
                            : new LinkedList<>()
                    );
                    return result;
                }
            };
        }
    }

    public static final String ID = "component_storage";

    private transient LinkedList<Quantifier<CalibreComponent>> components = new LinkedList<>();
    @LoadTimeOnly private ComponentCompatibility compatibility;

    public ComponentStorageSystem(CalibrePlugin plugin) {
        super(plugin);
    }
    public ComponentStorageSystem() { this(null); }

    public LinkedList<Quantifier<CalibreComponent>> getComponents() { return components; }
    public void setComponents(LinkedList<Quantifier<CalibreComponent>> components) { this.components = components; }

    public ComponentCompatibility getCompatibility() { return compatibility; }
    public void setCompatibility(ComponentCompatibility compatibility) { this.compatibility = compatibility; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemSystem.Events.SectionCreate.class, this::onEvent, 0);
    }

    @Override
    public Collection<Class<? extends CalibreSystem>> getServiceTypes() {
        return Collections.singleton(ComponentProviderSystem.class);
    }

    @Override
    public boolean isCompatible(CalibreComponent component) {
        return component != null && (compatibility == null || compatibility.test(component));
    }

    private void onEvent(ItemSystem.Events.SectionCreate event) {
        if (!parent.isRoot()) return;
        Player player = event.getPlayer();
        StringJoiner section = new StringJoiner(plugin.gen(player, "system.component_storage.line.separator"));
        for (Quantifier<CalibreComponent> quantifier : components)
            section.add(plugin.gen(player, "system.component_storage.line",
                    "amount", quantifier.getAmount(),
                    "component", quantifier.get().getLocalizedName(player)));
        event.getSections().add(section.toString());
    }

    public LinkedList<Quantifier<ComponentTree>> toTreeList() {
        return toTreeList(components);
    }

    @Override public String getId() { return ID; }
    @Override public ComponentStorageSystem clone() { return (ComponentStorageSystem) super.clone(); }
    @Override public ComponentStorageSystem copy() {
        ComponentStorageSystem copy = clone();
        if (copy.components != null) {
            copy.components = new LinkedList<>();
            copy.components.addAll(components);
        }
        return copy;
    }

    public static LinkedList<Quantifier<ComponentTree>> toTreeList(LinkedList<Quantifier<CalibreComponent>> components) {
        LinkedList<Quantifier<ComponentTree>> result = new LinkedList<>();
        for (Quantifier<CalibreComponent> quantifier : components)
            result.add(new Quantifier<>(ComponentTree.createAndBuild(quantifier.get()), quantifier.getAmount()));
        return result;
    }

    public static LinkedList<Quantifier<CalibreComponent>> fromTreeList(LinkedList<Quantifier<ComponentTree>> trees) {
        LinkedList<Quantifier<CalibreComponent>> result = new LinkedList<>();
        for (Quantifier<ComponentTree> quantifier : trees)
            result.add(new Quantifier<>(quantifier.get().getRoot(), quantifier.getAmount()));
        return result;
    }
}
