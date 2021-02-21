package com.gitlab.aecsocket.calibre.core.world.user;

public interface InaccuracyUser extends ItemUser {
    double inaccuracy();
    void addInaccuracy(double amount);
}
