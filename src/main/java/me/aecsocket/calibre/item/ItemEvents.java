package me.aecsocket.calibre.item;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.util.damagecause.BukkitDamageCause;
import me.aecsocket.calibre.item.util.damagecause.DamageCause;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.slot.PlayerItemSlot;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

// todo docs
public final class ItemEvents {
    private ItemEvents() {}

    //region Base

    public static class Event {
        private final ItemStack stack;
        private final ItemSlot slot;
        private final ItemUser user;

        public Event(ItemStack stack, ItemSlot slot, ItemUser user) {
            this.stack = stack;
            this.slot = slot;
            this.user = user;
        }

        public ItemStack getStack() { return stack; }
        public ItemSlot getSlot() { return slot; }
        public ItemUser getUser() { return user; }
    }

    //endregion

    public static class Create {
        private final @Nullable Player player;
        private final int amount;
        private final ItemStack item;
        private final ItemMeta meta;

        public Create(@Nullable Player player, int amount, ItemStack item, ItemMeta meta) {
            this.player = player;
            this.amount = amount;
            this.item = item;
            this.meta = meta;
        }

        public Player getPlayer() { return player; }
        public int getAmount() { return amount; }
        public ItemStack getItem() { return item; }
        public ItemMeta getMeta() { return meta; }
    }

    public static class Equip extends Event {
        private final TickContext tickContext;

        public Equip(ItemStack stack, ItemSlot slot, ItemUser user, TickContext tickContext) {
            super(stack, slot, user);
            this.tickContext = tickContext;
        }

        public TickContext getTickContext() { return tickContext; }
    }

    public static class Holster extends Event {
        public Holster(ItemStack stack, ItemSlot slot, ItemUser user) {
            super(stack, slot, user);
        }
    }

    public static class BukkitHolster extends Holster {
        private final PlayerItemHeldEvent event;

        public BukkitHolster(ItemStack stack, ItemSlot slot, ItemUser user, PlayerItemHeldEvent event) {
            super(stack, slot, user);
            this.event = event;
        }

        public static BukkitHolster of(CalibrePlugin plugin, PlayerItemHeldEvent event) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItem(event.getPreviousSlot());
            return new BukkitHolster(item, new PlayerItemSlot(player, event.getPreviousSlot()), plugin.userOf(player), event);
        }
    }

    public static class Draw extends Event {
        public Draw(ItemStack stack, ItemSlot slot, ItemUser user) {
            super(stack, slot, user);
        }
    }

    public static class BukkitDraw extends Draw {
        private final PlayerItemHeldEvent event;

        public BukkitDraw(ItemStack stack, ItemSlot slot, ItemUser user, PlayerItemHeldEvent event) {
            super(stack, slot, user);
            this.event = event;
        }

        public PlayerItemHeldEvent getEvent() { return event; }

        public static BukkitDraw of(CalibrePlugin plugin, PlayerItemHeldEvent event) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItem(event.getNewSlot());
            return new BukkitDraw(item, new PlayerItemSlot(player, event.getNewSlot()), plugin.userOf(player), event);
        }
    }

    public static class Damage extends Event {
        private final double damage;
        private final DamageCause cause;

        public Damage(ItemStack stack, ItemSlot slot, ItemUser user, double damage, DamageCause cause) {
            super(stack, slot, user);
            this.damage = damage;
            this.cause = cause;
        }

        public double getDamage() { return damage; }
        public DamageCause getCause() { return cause; }
    }

    public static class BukkitDamage extends Damage {
        private final EntityDamageEvent event;

        public BukkitDamage(ItemStack stack, ItemSlot slot, ItemUser user, double damage, DamageCause cause, EntityDamageEvent event) {
            super(stack, slot, user, damage, cause);
            this.event = event;
        }

        public EntityDamageEvent getEvent() { return event; }

        public static BukkitDamage of(CalibrePlugin plugin, EntityDamageEvent event, EquipmentSlot slot) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            ItemStack item = entity.getEquipment().getItem(slot);
            return new BukkitDamage(item, new EntityItemSlot(entity, slot), plugin.userOf(entity),
                    event.getFinalDamage(), BukkitDamageCause.of(event.getCause()), event);
        }
    }

    public static class Attack extends Event {
        private final Entity victim;
        private final double damage;
        private final DamageCause cause;

        public Attack(ItemStack stack, ItemSlot slot, ItemUser user, Entity victim, double damage, DamageCause cause) {
            super(stack, slot, user);
            this.victim = victim;
            this.damage = damage;
            this.cause = cause;
        }

        public Entity getVictim() { return victim; }
        public double getDamage() { return damage; }
        public DamageCause getCause() { return cause; }
    }

    public static class BukkitAttack extends Attack {
        private final EntityDamageByEntityEvent event;

        public BukkitAttack(ItemStack stack, ItemSlot slot, ItemUser user, Entity victim, double damage, DamageCause cause, EntityDamageByEntityEvent event) {
            super(stack, slot, user, victim, damage, cause);
            this.event = event;
        }

        public EntityDamageByEntityEvent getEvent() { return event; }

        public static BukkitAttack of(CalibrePlugin plugin, EntityDamageByEntityEvent event, EquipmentSlot slot) {
            LivingEntity damager = (LivingEntity) event.getDamager();
            ItemStack item = damager.getEquipment().getItem(slot);
            return new BukkitAttack(item, new EntityItemSlot(damager, slot), plugin.userOf(damager),
                    event.getEntity(), event.getFinalDamage(), BukkitDamageCause.of(event.getCause()), event);
        }
    }
}
