package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.util.CalibrePlayer;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.world.user.*;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface PlayerUser extends LivingEntityUser, MovementUser, CameraUser, RecoilableUser, InaccuracyUser, StabilizableUser, InventoryUser {
    Player entity();

    default CalibrePlayer playerData() { return CalibrePlugin.instance().playerData(entity()); }

    @Override default String locale() { return entity().getLocale(); }

    @Override default boolean sneaking() { return entity().isSneaking(); }
    @Override default boolean sprinting() { return entity().isSprinting(); }

    @Override default void zoom(double zoom) { CalibreProtocol.fov(entity(), zoom); }
    @Override default void applyRotation(Vector2D vector) {
        playerData().applyRotation(vector);
    }

    @Override default void applyRecoil(Vector2D recoil, double recoilSpeed, double recoilRecovery, double recoilRecoverySpeed, long recoilRecoveryAfter) {
        playerData().applyRecoil(recoil, recoilSpeed, recoilRecovery, recoilRecoverySpeed, recoilRecoveryAfter);
    }

    @Override default double inaccuracy() { return playerData().inaccuracy(); }
    @Override default void addInaccuracy(double amount) { playerData().inaccuracy(playerData().inaccuracy() + amount); }

    @Override default boolean stabilize(TickContext tickContext) { return playerData().stabilize(); }
    @Override default double stamina() { return playerData().stamina(); }
    @Override default double maxStamina() { return playerData().maxStamina(); }
    @Override default void reduceStamina(double amount) { playerData().reduceStamina(amount); }

    @Override default Inventory inventory() { return entity().getInventory(); }

    static PlayerUser of(Player player) { return () -> player; }
}