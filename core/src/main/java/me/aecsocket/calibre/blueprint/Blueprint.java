package me.aecsocket.calibre.blueprint;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.ItemCreationException;
import me.aecsocket.calibre.util.ItemSupplier;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A prebuit tree of components which can be used as a givable item.
 * @param <I> The item type.
 */
@ConfigSerializable
public abstract class Blueprint<I extends Item> implements CalibreIdentifiable, ItemSupplier<I> {
    @ConfigSerializable
    protected static class Dependencies {
        @Setting(nodeFromParent = true)
        protected ConfigurationNode tree;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;

    /** This object's ID. */
    protected String id;
    /** The tree this blueprint holds and builds. */
    protected transient ComponentTree tree;

    public Blueprint(String id) {
        this.id = id;
    }

    @Override public String id() { return id; }
    @Override public void id(String id) { this.id = id; }

    public ComponentTree tree() { return tree; }

    private void dependencies(ConfigurationNode node, Collection<String> dependencies) {
        if (node.virtual())
            return;

        String id;
        if (node.isMap()) {
            id = node.node("id").getString();
            node.node("slots").childrenMap().forEach((key, child) -> dependencies(child, dependencies));
        } else
            id = node.getString();

        if (id != null)
            dependencies.add(id);
    }

    @Override
    public Collection<String> dependencies() {
        List<String> dependencies = new ArrayList<>();
        dependencies(this.dependencies.tree, dependencies);
        return dependencies;
    }

    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        if (dependencies != null) {
            try {
                tree = dependencies.tree.get(ComponentTree.class);
            } catch (SerializationException e) {
                throw new ResolutionException(e);
            }
        }
    }

    @Override
    public I create(String locale, int amount) throws ItemCreationException {
        return tree.<CalibreComponent<I>>root().create(locale, amount);
    }
}
