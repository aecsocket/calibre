package me.aecsocket.calibre.item.component;

import me.aecsocket.calibre.item.system.CalibreSystem;

import java.util.function.BiConsumer;

public final class SystemSearchOptions<T extends CalibreSystem> {
    private String slotTag;
    private Integer targetPriority;
    private Class<? extends T> serviceType;

    public SystemSearchOptions() {}
    public SystemSearchOptions(Class<T> serviceType) {
        this.serviceType = serviceType;
    }

    public String getSlotTag() { return slotTag; }
    public SystemSearchOptions<T> slotTag(String slotTag) { this.slotTag = slotTag; return this; }

    public Integer getTargetPriority() { return targetPriority; }
    public SystemSearchOptions<T> targetPriority(Integer targetPriority) { this.targetPriority = targetPriority; return this; }

    public Class<? extends CalibreSystem> getServiceType() { return serviceType; }
    public SystemSearchOptions<T> serviceType(Class<? extends T> serviceType) { this.serviceType = serviceType; return this; }

    public void onEachMatching(CalibreComponentSlot slot, BiConsumer<CalibreComponentSlot, T> consumer) {
        if (slotTag != null && !slot.getTags().contains(slotTag)) return;
        if (targetPriority != null && slot.getPriority() != targetPriority) return;

        if (slot.get() != null) {
            slot.get().getMappedServices().forEach((type, sys) -> {
                if (serviceType != null && !serviceType.isInstance(sys)) return;
                @SuppressWarnings("unchecked")
                T tSys = (T) sys;
                consumer.accept(slot, tSys);
            });
        }
    }
}
