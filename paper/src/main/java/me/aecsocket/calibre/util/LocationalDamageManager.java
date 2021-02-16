package me.aecsocket.calibre.util;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LocationalDamageManager {
    @ConfigSerializable
    public static final class Location {
        private final Double min;
        private final Double max;
        private final double multiplier;

        public Location(Double min, Double max, double multiplier) {
            this.min = min;
            this.max = max;
            this.multiplier = multiplier;
        }

        public Location() {
            min = null;
            max = null;
            multiplier = 1;
        }

        public Double min() { return min; }
        public Double max() { return max; }
        public double multiplier() { return multiplier; }
    }

    public interface LocationCollector {
        Map<String, Location> PASS_MAP = Collections.emptyMap();
        LocationCollector PASS = entity -> PASS_MAP;

        Map<String, Location> locations(LivingEntity entity);
    }

    private final CalibrePlugin plugin;
    private final Map<EntityType, LocationCollector> entities = new HashMap<>();

    public LocationalDamageManager(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin plugin() { return plugin; }
    public Map<EntityType, LocationCollector> entities() { return entities; }

    public void load() {
        try {
            for (var entry : plugin.setting("locational_damage").get(new TypeToken<Map<EntityType, ConfigurationNode>>(){}, Collections.emptyMap()).entrySet()) {
                EntityType type = entry.getKey();
                ConfigurationNode node = entry.getValue();
                if (type == EntityType.PLAYER) {
                    Map<String, Location> normalLocations = node.node("normal").get(new TypeToken<>(){});
                    Map<String, Location> sneakingLocations = node.node("sneaking").get(new TypeToken<>(){});
                    entities.put(type, entity -> ((Player) entity).isSneaking() ? sneakingLocations : normalLocations);
                } else {
                    Map<String, Location> locations = node.get(new TypeToken<>(){});
                    entities.put(type, entity -> locations);
                }
            }
        } catch (SerializationException e) {
            plugin.log(LogLevel.WARN, e, "Could not load locational damage settings");
        }
    }

    public void damage(double damage, LivingEntity entity, Entity source) {
        if (damage <= 0)
            return;
        if (entity instanceof HumanEntity) {
            GameMode gameMode = ((HumanEntity) entity).getGameMode();
            if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR)
                return;
        }
        PotionEffect resistance = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        if (resistance != null)
            damage *= Utils.clamp(1 - (resistance.getAmplifier() / 4d), 0, 1);

        entity.damage(1e-16, source);
        entity.setHealth(Math.max(0, entity.getHealth() - damage));
        entity.setNoDamageTicks(0);
        entity.setVelocity(new Vector());
    }

    public double multiplier(LivingEntity entity, Vector3D position) {
        double y = position.y() - entity.getLocation().getY();
        for (var entry : entities.getOrDefault(entity.getType(), LocationCollector.PASS).locations(entity).entrySet()) {
            Location location = entry.getValue();
            if (
                    (location.min == null || y > location.min)
                    && (location.max == null || y <= location.max)
            ) {
                return location.multiplier;
            }
        }
        return 1;
    }

    public void damage(double damage, LivingEntity entity, Entity source, Vector3D position) {
        damage(damage * multiplier(entity, position), entity, source);
    }
}
