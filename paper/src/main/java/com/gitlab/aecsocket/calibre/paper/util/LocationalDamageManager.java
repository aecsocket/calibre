package com.gitlab.aecsocket.calibre.paper.util;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.core.util.data.Tuple2;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LogLevel;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationalDamageManager {
    @ConfigSerializable
    public static final class Location {
        private final Double min;
        private final Double max;
        private final double multiplier;
        private final EquipmentSlot armorSlot;

        public Location(Double min, Double max, double multiplier, EquipmentSlot armorSlot) {
            this.min = min;
            this.max = max;
            this.multiplier = multiplier;
            this.armorSlot = armorSlot;
        }

        public Location() {
            min = null;
            max = null;
            multiplier = 1;
            armorSlot = null;
        }

        public Double min() { return min; }
        public Double max() { return max; }
        public double multiplier() { return multiplier; }
        public EquipmentSlot armorSlot() { return armorSlot; }
    }

    public interface LocationCollector {
        Map<String, Location> PASS_MAP = Collections.emptyMap();
        LocationCollector PASS = entity -> PASS_MAP;

        Map<String, Location> locations(LivingEntity entity);
    }

    private final CalibrePlugin plugin;
    private final Map<EntityType, LocationCollector> entities = new HashMap<>();
    private boolean debug;

    public LocationalDamageManager(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin plugin() { return plugin; }
    public Map<EntityType, LocationCollector> entities() { return entities; }

    public boolean debug() { return debug; }
    public void debug(boolean debug) { this.debug = debug; }

    public void load() {
        try {
            for (var entry : plugin.setting(n -> n.get(new TypeToken<Map<EntityType, ConfigurationNode>>(){}), Collections.emptyMap(), "locational_damage", "locations").entrySet()) {
                EntityType type = entry.getKey();
                ConfigurationNode node = entry.getValue();
                if (type == EntityType.PLAYER) {
                    Map<String, Location> normal = node.node("normal").get(new TypeToken<>() {});
                    Map<String, Location> sneaking = node.node("sneaking").get(new TypeToken<>() {});
                    entities.put(type, entity -> ((Player) entity).isSneaking() ? sneaking : normal);
                } else if (Ageable.class.isAssignableFrom(type.getEntityClass())) {
                    Map<String, Location> normal = node.node("normal").get(new TypeToken<>() {});
                    Map<String, Location> child = node.node("child").get(new TypeToken<>() {});
                    entities.put(type, entity -> ((Ageable) entity).isAdult() ? normal : child);
                } else {
                    Map<String, Location> locations = node.get(new TypeToken<>(){});
                    entities.put(type, entity -> locations);
                }
            }
            debug = plugin.setting(n -> n.getBoolean(false), "locational_damage", "debug");
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

    public Tuple2<String, Location> location(LivingEntity entity, Vector3D position) {
        double y = position.y() - entity.getLocation().getY();
        for (var entry : entities.getOrDefault(entity.getType(), LocationCollector.PASS).locations(entity).entrySet()) {
            Location location = entry.getValue();
            if (
                    (location.min == null || y > location.min)
                            && (location.max == null || y <= location.max)
            ) {
                return Tuple2.of(entry.getKey(), location);
            }
        }
        return Tuple2.of(null, null);
    }

    public double multiplier(LivingEntity entity, Vector3D position) {
        Location location = location(entity, position).b();
        return location == null ? 1 : location.multiplier;
    }

    public void damage(double damage, LivingEntity entity, Entity source, Vector3D position) {
        if (debug && source instanceof Player) {
            Player player = (Player) source;
            double y = position.y() - entity.getLocation().getY();
            double realDamage = damage * multiplier(entity, position);
            Tuple2<String, Location> result = location(entity, position);
            Location location = result.b();
            Locale locale = player.locale();
            player.sendMessage(
                    location == null
                            ? plugin.gen(locale, "locational_damage_debug.no_location",
                            "damage", realDamage + "",
                            "y", String.format(locale, "%.3f", y))
                            : plugin.gen(locale, "locational_damage_debug.location",
                            "damage", realDamage + "",
                            "y", String.format(locale, "%.3f", y),
                            "location", result.a(),
                            "min", location.min + "",
                            "max", location.max + "",
                            "multiplier", location.multiplier + "")
            );
        }
        damage(damage * multiplier(entity, position), entity, source);
    }
}
