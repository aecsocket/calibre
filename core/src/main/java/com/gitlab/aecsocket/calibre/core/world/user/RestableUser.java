package com.gitlab.aecsocket.calibre.core.world.user;

import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;

public interface RestableUser extends ItemUser {
    boolean restsOn(Vector3D position);
}
