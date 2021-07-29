package com.gitlab.aecsocket.calibre.paper;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

public final class PlayerData {
    private final CalibrePlugin plugin;
    private final Player player;
    private Vector3 displayOffset;

    private @Nullable Item shaderDataEntity;
    private @Nullable ItemStack shaderData;

    private long stamina = -1;
    private long staminaRecoverAt;
    private boolean wasStabilized;

    private Vector2 recoil = Vector2.ZERO;
    private Vector2 recoilToRecover = Vector2.ZERO;
    private double recoilSpeed;
    private double recoilRecovery;
    private double recoilRecoverySpeed;
    private long recoilRecoveryAt;

    public PlayerData(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    void disable() {
        if (shaderDataEntity != null)
            shaderDataEntity.remove();
    }

    public CalibrePlugin plugin() { return plugin; }
    public Player player() { return player; }

    public Vector3 displayOffset() { return displayOffset; }
    public void displayOffset(Vector3 displayOffset) { this.displayOffset = displayOffset; }

    public Item shaderDataEntity() { return shaderDataEntity; }
    public ItemStack shaderData() { return shaderData; }

    public void shaderData(@Nullable ItemStack shaderData) {
        this.shaderData = shaderData;
        if (shaderData != null && shaderDataEntity != null && shaderDataEntity.isValid()) {
            shaderDataEntity.setItemStack(shaderData);
        }
    }

    public long stamina() { return stamina; }
    public void stamina(long stamina) { this.stamina = stamina; }
    public void drainStamina(long delta) {
        stamina -= delta;
        if (stamina < 0) {
            stamina = 0;
            wasStabilized = true;
        }
        staminaRecoverAt = System.currentTimeMillis() + plugin.setting(4000L, ConfigurationNode::getLong, "sway_stabilizer", "stamina_recover_after");
    }
    public boolean canStabilize() {
        return stamina > 0 && !wasStabilized;
    }

    public Vector2 recoil() { return recoil; }
    public void recoil(Vector2 recoil) { this.recoil = recoil; }

    public double recoilSpeed() { return recoilSpeed; }
    public void recoilSpeed(double recoilSpeed) { this.recoilSpeed = recoilSpeed; }

    public double recoilRecovery() { return recoilRecovery; }
    public void recoilRecovery(double recoilRecovery) { this.recoilRecovery = recoilRecovery; }

    public double recoilRecoverySpeed() { return recoilRecoverySpeed; }
    public void recoilRecoverySpeed(double recoilRecoverySpeed) { this.recoilRecoverySpeed = recoilRecoverySpeed; }

    public long recoilRecoveryAfter() { return recoilRecoveryAt; }
    public void recoilRecoveryAfter(long recoilRecoveryAfter) { this.recoilRecoveryAt = recoilRecoveryAfter; }

    public void applyRecoil(Vector2 recoil, double speed, double recovery, double recoverySpeed, long recoveryAfter) {
        this.recoil = this.recoil.add(recoil);
        recoilSpeed = speed;
        recoilRecovery = recovery;
        recoilRecoverySpeed = recoverySpeed;
        recoilRecoveryAt = System.currentTimeMillis() + recoveryAfter;
    }

    public void paperTick(TaskContext ctx) {
        if (displayOffset != null) {
            Location location = player.getEyeLocation();
            Vector3 fOffset = Vector3.offset(PaperUtils.toCommons(location.getDirection()), displayOffset);
            Vector3 pos = PaperUtils.toCommons(location).add(fOffset);
            player.spawnParticle(Particle.FLAME, PaperUtils.toBukkit(pos, player.getWorld()), 0);
        }

        if (!player.isSneaking())
            wasStabilized = false;
        long maxStamina = plugin.setting(3000L, ConfigurationNode::getLong, "sway_stabilizer", "max_stamina");
        if (stamina == -1)
            stamina = maxStamina;
        else if (stamina < maxStamina) {
            if (plugin.setting(true, ConfigurationNode::getBoolean, "sway_stabilizer", "display_stamina")
                    && player.getRemainingAir() >= player.getMaximumAir())
                plugin.air(player, (double) stamina / maxStamina);
            if (System.currentTimeMillis() >= staminaRecoverAt) {
                stamina += plugin.setting(1000L, ConfigurationNode::getLong, "sway_stabilizer", "stamina_recovery") * (ctx.delta() / 1000d);
                if (stamina > maxStamina)
                    stamina = maxStamina;
            }
        }

        if (shaderData == null) {
            if (shaderDataEntity != null && shaderDataEntity.isValid()) {
                shaderDataEntity.remove();
                shaderDataEntity = null;
            }
        } else {
            Location location = player.getEyeLocation();
            if (shaderDataEntity == null || !shaderDataEntity.isValid()) {
                if (shaderDataEntity != null)
                    shaderDataEntity.remove();
                shaderDataEntity = player.getWorld().spawn(location, Item.class, item -> {
                    item.setCanPlayerPickup(false);
                    item.setCanMobPickup(false);
                    item.setGravity(false);
                    item.setItemStack(shaderData);
                    player.addPassenger(item);
                });
            }
        }
    }

    private long last = 0;

    public void threadTick(TaskContext ctx) {
        if (recoil.manhattanLength() > 1e-4) {
            Vector2 delta = recoil.multiply(recoilSpeed);
            plugin.rotate(player, delta.x(), -delta.y());
            recoil = recoil.multiply(1 - recoilSpeed);
            recoilToRecover = recoilToRecover.add(delta.multiply(-recoilRecovery));
            last = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() >= recoilRecoveryAt && recoilToRecover.manhattanLength() > 1e-4) {
            Vector2 delta = recoilToRecover.multiply(recoilRecoverySpeed);
            plugin.rotate(player, delta.x(), -delta.y());
            recoilToRecover = recoilToRecover.multiply(1 - recoilRecoverySpeed);
        }
    }
}
