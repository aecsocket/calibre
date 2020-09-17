package me.aecsocket.calibre.item.system;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.unifiedframework.component.ComponentWalkData;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A result of finding a system from {@link CalibreComponent#searchSystems(SystemSearchOptions, Predicate)}.
 * @param <T> The system type.
 */
public class SystemSearchResult<T> {
    private final T system;
    private final ComponentWalkData walkData;

    public SystemSearchResult(T system, ComponentWalkData walkData) {
        this.system = system;
        this.walkData = walkData;
    }

    public T getSystem() { return system; }
    public ComponentWalkData getWalkData() { return walkData; }

    public Optional<CalibreComponent> getComponent() { return getSlot() == null ? Optional.empty() : Optional.of(getSlot().get()); }
    public CalibreComponentSlot getSlot() { return walkData.getSlot() instanceof CalibreComponentSlot ? (CalibreComponentSlot) walkData.getSlot() : null; }
}
