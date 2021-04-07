package com.gitlab.aecsocket.calibre.paper.util;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.core.util.data.Tuple2;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LogLevel;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationalDamageManager {
    private static final Map<Material, Double> armorValues = MapInit.of(new HashMap<Material, Double>())
            .init(Material.LEATHER_HELMET, 1d)
            .init(Material.CHAINMAIL_HELMET, 2d)
            .init(Material.IRON_HELMET, 2d)
            .init(Material.GOLDEN_HELMET, 2d)
            .init(Material.DIAMOND_HELMET, 3d)
            .init(Material.NETHERITE_HELMET, 3d)

            .init(Material.LEATHER_CHESTPLATE, 3d)
            .init(Material.CHAINMAIL_CHESTPLATE, 5d)
            .init(Material.IRON_CHESTPLATE, 6d)
            .init(Material.GOLDEN_CHESTPLATE, 5d)
            .init(Material.DIAMOND_CHESTPLATE, 8d)
            .init(Material.NETHERITE_CHESTPLATE, 8d)

            .init(Material.LEATHER_LEGGINGS, 2d)
            .init(Material.CHAINMAIL_LEGGINGS, 4d)
            .init(Material.IRON_LEGGINGS, 5d)
            .init(Material.GOLDEN_LEGGINGS, 3d)
            .init(Material.DIAMOND_LEGGINGS, 6d)
            .init(Material.NETHERITE_LEGGINGS, 6d)

            .init(Material.LEATHER_BOOTS, 1d)
            .init(Material.CHAINMAIL_BOOTS, 1d)
            .init(Material.IRON_BOOTS, 2d)
            .init(Material.GOLDEN_BOOTS, 3d)
            .init(Material.DIAMOND_BOOTS, 1d)
            .init(Material.NETHERITE_BOOTS, 3d)
            .get();

    @ConfigSerializable
    public static final class Location {
        private final Double min;
        private final Double max;
        private final double multiplier;
        private final EquipmentSlot armorSlot;
        private final double armorMultiplier;
        private final int armorWear;
        private final double armorDamageWear;

        private Location() {
            min = null;
            max = null;
            multiplier = 1;
            armorSlot = null;
            armorMultiplier = 1;
            armorWear = 0;
            armorDamageWear = 1;
        }

        public Double min() { return min; }
        public Double max() { return max; }
        public double multiplier() { return multiplier; }
        public EquipmentSlot armorSlot() { return armorSlot; }
        public double armorMultiplier() { return armorMultiplier; }
        public int armorWear() { return armorWear; }
        public double armorDamageWear() { return armorDamageWear; }
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
            for (var entry : plugin.setting(n -> n.get(new TypeToken<Map<EntityType, ConfigurationNode>>(){}, Collections.emptyMap()), "locational_damage", "locations").entrySet()) {
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

    public double damage(final double damage, LivingEntity entity, Entity source, @Nullable Location location) {
        if (damage < 0)
            return 0;
        if (entity.isInvulnerable())
            return 0;
        if (entity instanceof HumanEntity) {
            GameMode gameMode = ((HumanEntity) entity).getGameMode();
            if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR)
                return 0;
        }

        double result = damage;
        PotionEffect resistance = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        if (resistance != null)
            result *= Utils.clamp(1 - (resistance.getAmplifier() / 4d), 0, 1);

        // Deal damage
        entity.damage(1e-16, source);
        entity.setHealth(Math.max(0, entity.getHealth() - result));
        entity.setNoDamageTicks(0);
        entity.setVelocity(new Vector());

        double fResult = result;
        if (location != null && location.armorSlot != null) {
            // Damage armor
            ItemStack armor = entity.getEquipment().getItem(location.armorSlot);
            //noinspection ConstantConditions
            if (armor != null) {
                BukkitUtils.modMeta(armor, meta -> {
                    if (meta instanceof Damageable) {
                        Damageable dmg = (Damageable) meta;
                        dmg.setDamage(dmg.getDamage() + location.armorWear + (int) (fResult * location.armorDamageWear));
                    }
                });
            }
        }

        return result;
    }

    public double armor(LivingEntity entity, Location location) {
        if (location == null || location.armorSlot == null) {
            AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_ARMOR);
            if (attr != null)
                return attr.getValue();
        } else {
            // TODO dont do this when something is done about paper #5311
            ItemStack item = entity.getEquipment().getItem(location.armorSlot);
            // because yes, this *can* be null
            //noinspection ConstantConditions
            if (item != null) {
                return armorValues.getOrDefault(item.getType(), 0d) * location.armorMultiplier;
            }
        }
        return 0;
    }

    public double damage(final double damage, LivingEntity entity, Entity source, Vector3D position, double armorPenetration) {
        double result = damage;
        Tuple2<String, Location> locationData = location(entity, position);
        Location location = locationData.b();
        if (location != null) {
            result *= location.multiplier;
        }
        double armor = armor(entity, location);
        double armorMultiplier = armorMultiplier(armor, armorPenetration);
        result *= armorMultiplier;

        result = damage(result, entity, source, location);
        if (debug && source instanceof Player) {
            Player player = (Player) source;
            Locale locale = player.locale();

            double y = position.y();

            player.sendMessage(
                    location == null
                            ? plugin.gen(locale, "locational_damage_debug.no_location",
                                    "raw", String.format(locale, "%.2f", damage),
                                    "damage", String.format(locale, "%.2f", result),
                                    "y", String.format(locale, "%.3f", y),
                                    "armor_penetration", String.format(locale, "%.2f", armorPenetration),
                                    "armor_multiplier", String.format(locale, "%.2f", armorMultiplier)
                            )

                            : plugin.gen(locale, "locational_damage_debug.location",
                                    "raw", String.format(locale, "%.2f", damage),
                                    "damage", String.format(locale, "%.2f", result),
                                    "y", String.format(locale, "%.3f", y),
                                    "location", locationData.a(),
                                    "min", location.min + "",
                                    "max", location.max + "",
                                    "armor", String.format(locale, "%.2f", armor),
                                    "armor_penetration", String.format(locale, "%.2f", armorPenetration),
                                    "armor_multiplier", String.format(locale, "%.2f", armorMultiplier),
                                    "multiplier", location.multiplier + ""
                            )
            );
        }
        return damage;
    }

    public static double armorMultiplier(double armor, double armorPenetration) {
        return 1 - (armor / Math.max(Math.max(1, armor), armorPenetration));
    }
}
