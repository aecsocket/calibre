package me.aecsocket.calibre.defaults.system;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.CalibreRaytracing;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.item.util.user.EntityItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.LivingEntityItemUser;
import me.aecsocket.calibre.util.CalibreParticleData;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class LaserSystem extends BaseSystem {
    public static final String ID = "laser";

    @LoadTimeOnly private CalibreParticleData[] trail;
    @LoadTimeOnly private CalibreParticleData[] hit;
    @LoadTimeOnly private double step;
    @LoadTimeOnly private double maxDistance;
    @LoadTimeOnly private Vector defaultOffset = new Vector();

    public LaserSystem(CalibrePlugin plugin) { super(plugin); }
    public LaserSystem() { this(null); }

    public CalibreParticleData[] getTrail() { return trail; }
    public void setTrail(CalibreParticleData[] trail) { this.trail = trail; }

    public CalibreParticleData[] getHit() { return hit; }
    public void setHit(CalibreParticleData[] hit) { this.hit = hit; }

    public double getStep() { return step; }
    public void setStep(double step) { this.step = step; }

    public double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(double maxDistance) { this.maxDistance = maxDistance; }

    public Vector getDefaultOffset() { return defaultOffset; }
    public void setDefaultOffset(Vector defaultOffset) { this.defaultOffset = defaultOffset; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemEvents.Equip.class, this::onEvent, 0);
    }

    public Location getStartLocation(ItemUser user) {
        Location location = user.getLocation();
        if (user instanceof LivingEntityItemUser) {
            Vector offset = stat("barrel_offset");
            if (offset == null) offset = defaultOffset;
            location = Utils.getFacingRelative(((LivingEntityItemUser) user).getEntity(), offset);
        }
        return location;
    }

    private void onEvent(ItemEvents.Equip event) {
        if (!(event.getTickContext().getLoop() instanceof SchedulerLoop)) return;
        Utils.useService(CalibreRaytracing.class, s -> {
            double distance = 0;
            Location location = getStartLocation(event.getUser());
            Vector direction = location.getDirection().multiply(step);
            while (distance < maxDistance) {
                ParticleData.spawn(location, trail);

                double travelDistance = Math.min(maxDistance - distance, step);
                RayTraceResult ray = s.rayTrace(
                        location, direction, travelDistance, 0,
                        event.getUser() instanceof EntityItemUser ? ((EntityItemUser) event.getUser()).getEntity() : null
                );

                if (ray == null) {
                    distance += travelDistance;
                    location.add(direction);
                } else {
                    Vector delta = ray.getHitPosition().subtract(location.toVector());
                    distance += delta.length();
                    location.add(delta);

                    if (ray.getHitBlock() != null && ray.getHitBlock().getType().isOccluding())
                        break;
                }
            }
            ParticleData.spawn(location, hit);
        });
    }

    @Override public String getId() { return ID; }
    @Override public LaserSystem clone() { return (LaserSystem) super.clone(); }
    @Override public LaserSystem copy() { return clone(); }
}
