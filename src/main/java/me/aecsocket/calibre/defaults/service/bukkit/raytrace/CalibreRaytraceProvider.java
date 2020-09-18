package me.aecsocket.calibre.defaults.service.bukkit.raytrace;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Default implementation of {@link CalibreRaytraceService}.
 */
public class CalibreRaytraceProvider implements CalibreRaytraceService {
    @Override
    public RayTraceResult rayTrace(Location location, Vector direction, double distance, double expansion) {
        return location.getWorld().rayTrace(location, direction, distance, FluidCollisionMode.NEVER, true, expansion, null);
    }
}
