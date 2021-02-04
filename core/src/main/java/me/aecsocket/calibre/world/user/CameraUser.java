package me.aecsocket.calibre.world.user;

import me.aecsocket.unifiedframework.util.vector.Vector2D;

public interface CameraUser extends ItemUser {
    void zoom(double zoom);
    void rotate(Vector2D vector);
}
