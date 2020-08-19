package me.aecsocket.calibre.item.component.descriptor;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Can create a {@link CalibreComponent} if provided with a {@link Registry}.
 * <p>
 * These are used as wrapper classes when deserializing, so that a user can write, for example:
 * <code>
 * { "id": "magazine", "ammo": [ "bullet" ] }
 * </code>
 * rather than just writing out the component's definition again.
 * <p>
 * It is also not viable to store just a string reference to the ID of the component,
 * as a component can have instance data, which would be replaced with the registry's instance's data
 * otherwise. This would mean that, for example components which hold ammo would share the exact bullets
 * with every other component of the same ID.
 */
public class ComponentDescriptor {
    private String id;

    public ComponentDescriptor(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    /**
     * Creates the {@link CalibreComponent} this can create.
     * @param registry The {@link Registry} used for resolving other components.
     * @return The created {@link CalibreComponent}.
     * @throws ComponentCreationException If the component could not be created.
     */
    public @NotNull CalibreComponent create(Registry registry) throws ComponentCreationException {
        if (!registry.has(id, CalibreComponent.class)) throw new ComponentCreationException(TextUtils.format("Failed to find component with ID {id}", "id", id));
        return registry.getRaw(id, CalibreComponent.class);
    }
}
