package me.aecsocket.calibre.item.component.search;

import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.system.CalibreSystem;

public class SystemSearchResult<T extends CalibreSystem> {
    private final CalibreComponentSlot slot;
    private final T system;

    public SystemSearchResult(CalibreComponentSlot slot, T system) {
        this.slot = slot;
        this.system = system;
    }

    public CalibreComponentSlot getSlot() { return slot; }
    public T getSystem() { return system; }
}
