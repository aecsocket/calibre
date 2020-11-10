package me.aecsocket.calibre.item.blueprint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.util.LoadTimeDependencies;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.item.ItemStackFactory;
import me.aecsocket.unifiedframework.registry.ResolutionContext;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.registry.ValidationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class Blueprint implements CalibreIdentifiable, ItemStackFactory {
    /**
     * Temporarily stores info on deserialization for resolution later.
     */
    protected static class Dependencies {
        private final JsonElement tree;

        public Dependencies() {
            tree = null;
        }
    }

    @LoadTimeDependencies private transient Dependencies dependencies;
    private transient CalibrePlugin plugin;
    private final String id;
    private transient ComponentTree tree;

    public Blueprint(String id) {
        this.id = id;
    }

    public Blueprint() {
        this(null);
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return id; }

    public ComponentTree getTree() { return tree; }
    public void setTree(ComponentTree tree) { this.tree = tree; }

    @Override public String getNameKey() { return "blueprint." + id; }

    @Override
    public void validate() throws ValidationException {
        if (dependencies.tree == null) throw new ValidationException("Tree cannot be null");
    }

    @Override
    public Collection<String> getDependencies() {
        Collection<String> result = new ArrayList<>();
        getDependencies(result, dependencies.tree);
        return result;
    }

    private void getDependencies(Collection<String> result, JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (object.has("id")) result.add(object.get("id").getAsString());
            if (object.has("slots")) object.get("slots").getAsJsonObject().entrySet().forEach(entry -> getDependencies(result, entry.getValue()));
        } else if (json.isJsonPrimitive())
            result.add(json.getAsString());
    }

    @Override
    public void resolve(ResolutionContext context) throws ResolutionException {
        try {
            tree = plugin.getGson().fromJson(dependencies.tree, ComponentTree.class);
        } catch (JsonParseException e) {
            throw new ResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public ItemStack createItem(@Nullable Player player, int amount) throws ItemCreationException {
        if (getRoot() == null)
            throw new ItemCreationException("Invalid tree");
        return getRoot().createItem(player, amount);
    }

    public CalibreComponent getRoot() { return tree == null ? null : tree.getRoot(); }

    @Override public String toString() { return "Blueprint:" + id; }
}
