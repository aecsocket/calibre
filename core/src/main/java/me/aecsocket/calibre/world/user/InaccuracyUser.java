package me.aecsocket.calibre.world.user;

public interface InaccuracyUser extends ItemUser {
    double inaccuracy();
    void addInaccuracy(double amount);
}
