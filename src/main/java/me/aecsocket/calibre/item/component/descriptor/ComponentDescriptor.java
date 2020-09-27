package me.aecsocket.calibre.item.component.descriptor;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Can create a {@link CalibreComponent} if provided with a {@link Registry}.
 * <p>
 * These are used in tandem with system descriptors so that a user can write, for example:
 * <pre>
 * {@code
 * {                                                      # the component descriptor
 *   "id": "magazine_component",
 *   "ammo_storage": {                                    # a system descriptor that describes the "ammo_storage" system
 *      "ammo": [ "bullet_type1", "bullet_type2", ... ]
 *   }
 * }
 * }
 * </pre>
 * rather than just writing out the component's (and the systems') definition again.
 * <p>
 * It is also not viable to store just a string reference to the ID of the component,
 * as a component's systems can have instance data, which would be replaced with the registry's instance's systems'
 * data (the master copy's) otherwise. This would mean that, for example components with systems which hold ammo
 * would share the exact bullets with every other component of the same ID.
 */
public class ComponentDescriptor {
    private String id;
    private Map<String, Object> systems = new HashMap<>();
    private Map<String, ComponentDescriptor> slots = new HashMap<>();

    public ComponentDescriptor(String id, Map<String, Object> systems, Map<String, ComponentDescriptor> slots) {
        this.id = id;
        this.systems = systems;
        this.slots = slots;
    }

    public ComponentDescriptor(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Map<String, Object> getSystems() { return systems; }
    public void setSystems(Map<String, Object> systems) { this.systems = systems; }

    public Map<String, ComponentDescriptor> getSlots() { return slots; }
    public void setSlots(Map<String, ComponentDescriptor> slots) { this.slots = slots; }

    /**
     * Creates the {@link CalibreComponent} this can create, also creating the component tree and setting
     * up event handlers.
     * @param registry The {@link Registry} used for resolving other components.
     * @param tree The {@link ComponentTree} for all components in the tree.
     * @return The created {@link CalibreComponent}.
     * @throws ComponentCreationException If the component could not be created.
     */
    public @NotNull CalibreComponent create(Registry registry, ComponentTree tree) throws ComponentCreationException {
        // todo: a lot of this is a duplicate of ComponentTree#rebuild's code. do something here?
        CalibreComponent component = registry.getRaw(id, CalibreComponent.class);
        if (component == null) throw new ComponentCreationException(TextUtils.format("Failed to find component with ID {id}", "id", id));
        component = component.copy();

        component.setTree(tree);

        // Stats
        component.modifyTree(tree);

        // Systems
        for (CalibreSystem<?> system : component.getSystems().values()) {
            String id = system.getId();
            if (systems.containsKey(id))
                provideDescriptor(system, systems.get(id));
        }

        // Slots
        for (Map.Entry<String, ComponentDescriptor> entry : slots.entrySet()) {
            String name = entry.getKey();
            if (component.getSlots().containsKey(name)) {
                CalibreComponentSlot slot = component.getSlots().get(name);
                CalibreComponent child = entry.getValue().create(registry, tree);
                if (slot.isCompatible(child))
                    slot.set(child);
            }
        }

        return component;
    }

    /**
     * Creates the {@link CalibreComponent} this can create, also creating the component tree and setting
     * up event handlers.
     * @param registry The {@link Registry} used for resolving other components.
     * @return The created {@link CalibreComponent}.
     * @throws ComponentCreationException If the component could not be created.
     */
    public @NotNull CalibreComponent create(Registry registry) throws ComponentCreationException {
        ComponentTree tree = new ComponentTree();
        CalibreComponent root = create(registry, tree);
        tree.setRoot(root);
        tree.rebuild();
        return root;
    }

    private <D> void provideDescriptor(CalibreSystem<D> system, Object descriptor) {
        @SuppressWarnings("unchecked")
        Class<D> descriptorType = (Class<D>) system.getDescriptorType().getRawType();
        if (descriptorType == null) return;
        if (descriptorType.isAssignableFrom(descriptor.getClass()))
            system.acceptDescriptor(descriptorType.cast(descriptor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentDescriptor that = (ComponentDescriptor) o;
        return id.equals(that.id) &&
                systems.equals(that.systems) &&
                slots.equals(that.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, systems, slots);
    }

    /**
     * Creates a {@link ComponentDescriptor} from a {@link CalibreComponent}, including system descriptors
     * and slots.
     * @param component The CalibreComponent.
     * @return The ComponentDescriptor.
     */
    public static ComponentDescriptor of(CalibreComponent component) {
        ComponentDescriptor descriptor = new ComponentDescriptor(component.getId());

        component.getSystems().values().forEach(system -> {
            Object sysDescriptor = system.createDescriptor();
            if (sysDescriptor == null) return;
            descriptor.getSystems().put(system.getId(), sysDescriptor);
        });
        component.getSlots().forEach((name, slot) -> {
            if (slot.get() == null) return;
            descriptor.getSlots().put(name, ComponentDescriptor.of(slot.get()));
        });

        return descriptor;
    }
}
