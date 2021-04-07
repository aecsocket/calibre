package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.builtin.NameFromChildSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.RangefinderSystem;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitProjectile;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public final class PaperRangefinderSystem extends RangefinderSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperRangefinderSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    private PaperRangefinderSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperRangefinderSystem(PaperRangefinderSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override
    protected Double getDistance(ItemUser user) {
        if (user instanceof LivingEntityUser) {
            LivingEntity entity = ((LivingEntityUser) user).entity();
            Location location = entity.getEyeLocation();
            RayTraceResult ray = entity.getWorld().rayTrace(
                    location, location.getDirection(),
                    maxDistance, FluidCollisionMode.NEVER, true, 0,
                    e -> BukkitProjectile.PREDICATE.test(e) && e != entity);
            return ray == null ? null : ray.getHitPosition().distance(location.toVector());
        }
        return null;
    }

    @Override public PaperRangefinderSystem copy() { return new PaperRangefinderSystem(this); }
}
