package me.aecsocket.calibre.item.component.search;

import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.system.CalibreSystem;

import java.util.function.BiConsumer;

public class SystemSearchOptions<T extends CalibreSystem> extends SlotSearchOptions {
    private Class<? extends T> serviceType;

    public SystemSearchOptions() {}
    public SystemSearchOptions(Class<T> serviceType) {
        this.serviceType = serviceType;
    }

    @SuppressWarnings("unchecked")
    @Override public SystemSearchOptions<T> slotTag(String slotTag) { return (SystemSearchOptions<T>) super.slotTag(slotTag); }
    @SuppressWarnings("unchecked")
    @Override public SystemSearchOptions<T> targetPriority(Integer targetPriority) { return (SystemSearchOptions<T>) super.targetPriority(targetPriority); }

    public Class<? extends CalibreSystem> getServiceType() { return serviceType; }
    public SystemSearchOptions<T> serviceType(Class<? extends T> serviceType) { this.serviceType = serviceType; return this; }

    public void onEachMatching(CalibreComponentSlot slot, BiConsumer<CalibreComponentSlot, T> consumer) {
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
