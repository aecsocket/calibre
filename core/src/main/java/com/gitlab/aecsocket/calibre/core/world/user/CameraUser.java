package com.gitlab.aecsocket.calibre.core.world.user;

import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;

public interface CameraUser extends ItemUser {
    void zoom(double zoom);
    void applyRotation(Vector2D vector);
}
