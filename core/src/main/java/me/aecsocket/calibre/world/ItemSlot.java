package me.aecsocket.calibre.world;

public interface ItemSlot<I extends Item> {
    I get();
    void set(I item);
}
