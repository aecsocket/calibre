package com.gitlab.aecsocket.calibre.core.world.slot;

import com.gitlab.aecsocket.calibre.core.world.Item;

public interface EquippableSlot<I extends Item> extends ItemSlot<I> {
    boolean equipped();
}
