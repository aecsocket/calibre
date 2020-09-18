package me.aecsocket.calibre.defaults.service.bukkit.raytrace;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * Provides ray tracing capabilities.
 */
public interface CalibreRaytraceService {
    /**
     * Ray traces for entities and blocks from the specified start position towards the direction.
     * If using {@link org.bukkit.World#rayTrace(Location, Vector, double, FluidCollisionMode, boolean, double, Predicate)}, implies
     * {@link FluidCollisionMode#NEVER}, ignores passable blocks, and <code>null</code> predicate.
     * @param location The start position.
     * @param direction The direction to raytrace in.
     * @param distance The distance to raytrace up to.
     * @param expansion The expansion of entity hitboxes (default 0.0).
     * @return The {@link RayTraceResult}.
     */
    RayTraceResult rayTrace(Location location, Vector direction, double distance, double expansion);
}
