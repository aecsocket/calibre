package com.gitlab.aecsocket.calibre.core.world.slot;

import com.gitlab.aecsocket.calibre.core.world.Item;

public interface ItemSlot<I extends Item> {
    I get();
    void set(I item);
}
