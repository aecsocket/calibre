package me.aecsocket.calibre.item.util.damagecause;

import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumMap;
import java.util.Map;

public class BukkitDamageCause implements DamageCause {
    private static final Map<EntityDamageEvent.DamageCause, BukkitDamageCause> instances = new EnumMap<>(EntityDamageEvent.DamageCause.class);

    private final EntityDamageEvent.DamageCause handle;

    private BukkitDamageCause(EntityDamageEvent.DamageCause handle) {
        this.handle = handle;
    }

    public EntityDamageEvent.DamageCause getHandle() { return handle; }

    public static BukkitDamageCause of(EntityDamageEvent.DamageCause cause) {
        return instances.computeIfAbsent(cause, __ -> new BukkitDamageCause(cause));
    }
}
