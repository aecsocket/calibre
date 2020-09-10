package me.aecsocket.calibre.util;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.unifiedframework.component.ComponentWalkData;

import java.util.Optional;

/**
 * Represents a system inside a component slot.
 * @param <T> The system type.
 */
public class SystemRepresentation<T> {
    private final T system;
    private final ComponentWalkData walkData;

    public SystemRepresentation(T system, ComponentWalkData walkData) {
        this.system = system;
        this.walkData = walkData;
    }

    public T getSystem() { return system; }
    public ComponentWalkData getWalkData() { return walkData; }

    public CalibreComponentSlot getSlot() { return (CalibreComponentSlot) walkData.getSlot(); }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<CalibreComponent> getComponent() { return (Optional) walkData.getComponent(); }
}
