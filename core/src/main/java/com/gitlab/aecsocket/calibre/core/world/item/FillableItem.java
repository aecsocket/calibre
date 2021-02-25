package com.gitlab.aecsocket.calibre.core.world.item;

public interface FillableItem extends Item {
    double filled();
    void fill(double percentage);
}
