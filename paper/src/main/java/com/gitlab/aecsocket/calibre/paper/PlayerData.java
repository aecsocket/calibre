package com.gitlab.aecsocket.calibre.paper;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlayerData {
    private final CalibrePlugin plugin;
    private final Player player;
    private @Nullable Item shaderDataEntity;
    private @Nullable ItemStack shaderData;

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

    public void paperTick(TaskContext ctx) {
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
