package me.aecsocket.calibre.component;

import me.aecsocket.calibre.system.CalibreSystem;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

/**
 * Determines conditions for a component to fulfil.
 */
@ConfigSerializable
public class ComponentCompatibility {
    /** The ID required. Can be null. */
    private String id;
    /** A list of categories required. The component must fulfil at least one. Can be empty or null. */
    private final List<String> categories;
    /** A system that the component must hold. Can be null. */
    private transient Class<? extends CalibreSystem> systemType;

    public ComponentCompatibility() {
        categories = new ArrayList<>();
    }

    public String id() { return id; }
    public ComponentCompatibility id(String id) { this.id = id; return this; }

    public List<String> categories() { return categories; }
    public ComponentCompatibility categories(Collection<String> categories) { this.categories.addAll(categories); return this; }
    public ComponentCompatibility categories(String... categories) { return categories(Arrays.asList(categories)); }

    public Class<? extends CalibreSystem> systemType() { return systemType; }
    public void systemType(Class<? extends CalibreSystem> systemType) { this.systemType = systemType; }

    /**
     * If the conditions apply to the specified component.
     * @param component The component.
     * @return The result.
     */
    public boolean applies(CalibreComponent<?> component) {
        if (id != null && !component.id.equals(id)) return false;
        if (!categories.isEmpty() && (Collections.disjoint(component.categories, categories))) return false;
        if (systemType != null) {
            boolean foundSystem = false;
            for (CalibreSystem system : component.systems.values()) {
                if (systemType.isInstance(system)) {
                    foundSystem = true;
                    break;
                }
            }
            if (!foundSystem)
                return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentCompatibility that = (ComponentCompatibility) o;
        return Objects.equals(id, that.id) && Objects.equals(categories, that.categories) && Objects.equals(systemType, that.systemType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categories, systemType);
    }

    @Override
    public String toString() {
        return "ComponentCompatibility{" +
                "id='" + id + '\'' +
                ", categories=" + categories +
                ", systemType=" + systemType +
                '}';
    }
}
