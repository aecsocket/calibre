package me.aecsocket.calibre.item.util.user;

public interface DelayableItemUser extends ItemUser {
    void applyDelay(long ms);
    double getDelay();
}
