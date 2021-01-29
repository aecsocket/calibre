package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.world.RecoilableUser;
import me.aecsocket.calibre.world.SneakableUser;
import me.aecsocket.calibre.world.ZoomableUser;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import org.bukkit.entity.Player;

public interface PlayerUser extends LivingEntityUser, SneakableUser, ZoomableUser, RecoilableUser {
    Player entity();

    @Override default String locale() { return entity().getLocale(); }
    @Override default boolean sneaking() { return entity().isSneaking(); }
    @Override default void zoom(double zoom) { CalibreProtocol.fov(entity(), zoom); }
    @Override default void applyRecoil(Vector2D recoil, double recoilSpeed, double recoilRecovery, double recoilRecoverySpeed, long recoilRecoveryAfter) {
        CalibrePlugin.getInstance().playerData(entity()).applyRecoil(recoil, recoilSpeed, recoilRecovery, recoilRecoverySpeed, recoilRecoveryAfter);
    }

    static PlayerUser of(Player player) { return () -> player; }
}
