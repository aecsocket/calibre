package me.aecsocket.calibre.defaults.service;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public interface CalibreRaytracing extends CalibreInbuilt {
    RayTraceResult rayTrace(Location location, Vector velocity, double distance, double expansion);

    class Provider implements CalibreRaytracing {
        @Override
        public RayTraceResult rayTrace(Location location, Vector velocity, double distance, double expansion) {
            return location.getWorld().rayTrace(
                    location, velocity, distance,
                    FluidCollisionMode.NEVER, true, expansion,
                    e -> !e.isDead() && !(e instanceof Item)
            );
        }
    }
}
