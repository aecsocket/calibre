package me.aecsocket.calibre.item.component;

import me.aecsocket.unifiedframework.registry.Registry;

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
public interface ComponentDescriptor {
    /**
     * Creates the {@link CalibreComponent} this can create.
     * @param registry The {@link Registry} used for resolving other components.
     * @return The created {@link CalibreComponent}.
     */
    CalibreComponent create(Registry registry);
}
