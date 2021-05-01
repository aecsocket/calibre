package com.gitlab.aecsocket.calibre.paper.wrapper.user;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.util.CalibrePlayerData;
import com.gitlab.aecsocket.calibre.core.world.user.*;
import com.gitlab.aecsocket.calibre.paper.util.CalibreProtocol;
import com.gitlab.aecsocket.unifiedframework.core.scheduler.TaskContext;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.Duration;
import java.util.Locale;

public interface PlayerUser extends LivingEntityUser, MovementUser, CameraUser, RecoilableUser, InaccuracyUser, StabilizableUser, InventoryUser {
    Player entity();

    default CalibrePlayerData playerData() { return CalibrePlugin.instance().playerData(entity()); }

    @Override default Locale locale() { return entity().locale(); }

    @Override
    default void showTitle(Component title, Component subtitle, long fadeIn, long stay, long fadeOut) {
        entity().showTitle(Title.title(
                title, subtitle,
                Title.Times.of(
                        Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut)
                )
        ));
    }

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

    @Override default boolean stabilize(TaskContext taskContext) { return playerData().stabilize(); }
    @Override default double stamina() { return playerData().stamina(); }
    @Override default double maxStamina() { return playerData().maxStamina(); }
    @Override default void reduceStamina(double amount) { playerData().reduceStamina(amount); }

    @Override default Inventory inventory() { return entity().getInventory(); }

    static PlayerUser of(Player player) { return () -> player; }
}
