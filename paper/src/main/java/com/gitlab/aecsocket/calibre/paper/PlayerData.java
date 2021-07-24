package com.gitlab.aecsocket.calibre.paper;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

public final class PlayerData {
    private final CalibrePlugin plugin;
    private final Player player;
    private @Nullable Item shaderDataEntity;
    private @Nullable ItemStack shaderData;
    private long stamina = -1;
    private long staminaRecoverAt;
    private boolean wasStabilized;

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

    public void paperTick(TaskContext ctx) {
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
}
