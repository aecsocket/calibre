package me.aecsocket.calibre.world;

import me.aecsocket.unifiedframework.util.vector.Vector3D;

public interface ItemUser {
    Vector3D position();
    Vector3D direction();
    String locale();
}
