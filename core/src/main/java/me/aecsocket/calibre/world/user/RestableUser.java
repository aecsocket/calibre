package me.aecsocket.calibre.world.user;

import me.aecsocket.unifiedframework.util.vector.Vector3D;

public interface RestableUser extends ItemUser {
    boolean restsOn(Vector3D position);
}
