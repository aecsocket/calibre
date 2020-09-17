package me.aecsocket.calibre.item.system;

import me.aecsocket.calibre.item.component.CalibreComponentSlot;

import java.util.Arrays;
import java.util.Collections;

/**
 * Filters for searching for systems in a component tree.
 * @param <T> The system type.
 */
public class SystemSearchOptions<T> {
    private String[] slotTags;
    private Integer slotPriority;
    private Class<T> systemType;

    public String[] getSlotTags() { return slotTags; }
    public Integer getSlotPriority() { return slotPriority; }
    public Class<T> getSystemType() { return systemType; }

    public SystemSearchOptions<T> slotTags(String... slotTags) { this.slotTags = slotTags; return this; }
    public SystemSearchOptions<T> slotPriority(Integer slotPriority) { this.slotPriority = slotPriority; return this; }
    public SystemSearchOptions<T> systemType(Class<T> systemType) { this.systemType = systemType; return this; }

    public boolean test(CalibreComponentSlot slot) {
        if (slotTags != null) {
            if (Collections.disjoint(slot.getTags(), Arrays.asList(slotTags)))
                return false;
        }

        return slotPriority == null || slot.getPriority() == slotPriority;
    }

    @SuppressWarnings("unchecked")
    public T systemOf(CalibreSystem<?> system) {
        return systemType == null
                ? (T) system
                : systemType.isAssignableFrom(system.getClass())
                ? systemType.cast(system)
                : null;
    }

    /**
     * Creates simple filters according to specified parameters.
     * @param slotTag The required tag for a slot to be accepted.
     * @param slotPriority The required priority for a slot to be accepted.
     * @param systemType The required type for a system to be accepted.
     * @param <T> The system type.
     * @return The filter.
     */
    public static <T> SystemSearchOptions<T> of(String slotTag, int slotPriority, Class<T> systemType) {
        return new SystemSearchOptions<T>()
                .slotTags(slotTag)
                .slotPriority(slotPriority)
                .systemType(systemType);
    }
}
