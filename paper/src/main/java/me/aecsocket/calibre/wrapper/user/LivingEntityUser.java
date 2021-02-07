package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.world.user.RestableUser;
import me.aecsocket.calibre.world.user.SwimmableUser;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.LivingEntity;

public interface LivingEntityUser extends EntityUser, SwimmableUser, RestableUser {
    LivingEntity entity();

    @Override default Vector3D position() { return VectorUtils.toUF(entity().getEyeLocation().toVector()); }
    @Override default boolean swimming() {
        if (entity().isSwimming())
            return true;
        Block block = entity().getEyeLocation().getBlock();
        if (block.getType() == Material.WATER)
            return true;
        BlockData data = block.getBlockData();
        return data instanceof Waterlogged && ((Waterlogged) data).isWaterlogged();
    }

    @Override
    default boolean restsOn(Vector3D position) {
        return VectorUtils.toBukkit(position).toLocation(world()).getBlock().getType().isSolid();
    }

    static LivingEntityUser of(LivingEntity entity) { return () -> entity; }
}
