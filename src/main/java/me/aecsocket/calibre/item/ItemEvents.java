package me.aecsocket.calibre.item;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.util.damagecause.BukkitDamageCause;
import me.aecsocket.calibre.item.util.damagecause.DamageCause;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.slot.PlayerItemSlot;
import me.aecsocket.calibre.item.util.user.AnimatableItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

        // TODO shorthand to update the item in the slot. document this.
        public final ItemStack updateItem(CalibreSystem system, boolean hideUpdated) {
            ComponentTree tree = system.getParent().getTree();
            ItemStack updated = tree.getRoot().createItem(
                    user instanceof PlayerItemUser ? ((PlayerItemUser) user).getEntity() : null,
                    slot.get().getAmount()
            );
            tree.callEvent(new Update(updated, slot, user));
            if (hideUpdated)
                CalibreComponent.setHidden(tree.getRoot().getPlugin(), updated, true);
            slot.set(updated);
            return updated;
        }

        public final ItemStack updateItem(CalibreSystem system) {
            return updateItem(system, user instanceof AnimatableItemUser && ((AnimatableItemUser) user).getAnimation() != null);
        }
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

    public static class Update extends Event {
        public Update(ItemStack stack, ItemSlot slot, ItemUser user) {
            super(stack, slot, user);
        }
    }

    public static class Equip extends Event {
        private final TickContext tickContext;

        public Equip(ItemStack stack, ItemSlot slot, ItemUser user, TickContext tickContext) {
            super(stack, slot, user);
            this.tickContext = tickContext;
        }

        public TickContext getTickContext() { return tickContext; }
    }

    public static class Interact extends Event {
        private final Action action;
        private final Block clickedBlock;
        private final BlockFace blockFace;

        private org.bukkit.event.Event.Result handResult;
        private org.bukkit.event.Event.Result blockResult;

        public Interact(ItemStack stack, ItemSlot slot, ItemUser user, Action action, Block clickedBlock, BlockFace blockFace, org.bukkit.event.Event.Result handResult, org.bukkit.event.Event.Result blockResult) {
            super(stack, slot, user);
            this.action = action;
            this.clickedBlock = clickedBlock;
            this.blockFace = blockFace;
            this.handResult = handResult;
            this.blockResult = blockResult;
        }

        public Action getAction() { return action; }
        public Block getClickedBlock() { return clickedBlock; }
        public BlockFace getBlockFace() { return blockFace; }

        public org.bukkit.event.Event.Result getHandResult() { return handResult; }
        public void setHandResult(org.bukkit.event.Event.Result handResult) { this.handResult = handResult; }

        public org.bukkit.event.Event.Result getBlockResult() { return blockResult; }
        public void setBlockResult(org.bukkit.event.Event.Result blockResult) { this.blockResult = blockResult; }
    }

    public static class BukkitInteract extends Interact {
        private final PlayerInteractEvent event;

        public BukkitInteract(ItemStack stack, ItemSlot slot, ItemUser user, Action action, Block clickedBlock, BlockFace blockFace, org.bukkit.event.Event.Result handResult, org.bukkit.event.Event.Result blockResult, PlayerInteractEvent event) {
            super(stack, slot, user, action, clickedBlock, blockFace, handResult, blockResult);
            this.event = event;
        }

        public PlayerInteractEvent getEvent() { return event; }

        public static BukkitInteract of(CalibrePlugin plugin, PlayerInteractEvent event, EquipmentSlot slot) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItem(slot);
            return new BukkitInteract(item, new EntityItemSlot(player, slot), plugin.userOf(player),
                    event.getAction(), event.getClickedBlock(), event.getBlockFace(),
                    event.useItemInHand(), event.useInteractedBlock(), event);
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

    public static class Damage extends Event {
        private final double finalDamage;
        private final DamageCause cause;
        private double damage;

        public Damage(ItemStack stack, ItemSlot slot, ItemUser user, double finalDamage, DamageCause cause, double damage) {
            super(stack, slot, user);
            this.finalDamage = finalDamage;
            this.cause = cause;
            this.damage = damage;
        }

        public double getFinalDamage() { return finalDamage; }
        public DamageCause getCause() { return cause; }

        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }
    }

    public static class BukkitDamage extends Damage {
        private final EntityDamageEvent event;

        public BukkitDamage(ItemStack stack, ItemSlot slot, ItemUser user, double finalDamage, DamageCause cause, double damage, EntityDamageEvent event) {
            super(stack, slot, user, finalDamage, cause, damage);
            this.event = event;
        }

        public EntityDamageEvent getEvent() { return event; }

        public static BukkitDamage of(CalibrePlugin plugin, EntityDamageEvent event, EquipmentSlot slot) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            ItemStack item = entity.getEquipment().getItem(slot);
            return new BukkitDamage(item, new EntityItemSlot(entity, slot), plugin.userOf(entity),
                    event.getFinalDamage(), BukkitDamageCause.of(event.getCause()), event.getDamage(), event);
        }
    }

    public static class Attack extends Event {
        private final Entity victim;
        private final double finalDamage;
        private final DamageCause cause;
        private double damage;

        public Attack(ItemStack stack, ItemSlot slot, ItemUser user, Entity victim, double finalDamage, DamageCause cause, double damage) {
            super(stack, slot, user);
            this.victim = victim;
            this.finalDamage = finalDamage;
            this.cause = cause;
            this.damage = damage;
        }

        public Entity getVictim() { return victim; }
        public double getFinalDamage() { return finalDamage; }
        public DamageCause getCause() { return cause; }

        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }
    }

    public static class BukkitAttack extends Attack {
        private final EntityDamageByEntityEvent event;

        public BukkitAttack(ItemStack stack, ItemSlot slot, ItemUser user, Entity victim, double finalDamage, DamageCause cause, double damage, EntityDamageByEntityEvent event) {
            super(stack, slot, user, victim, finalDamage, cause, damage);
            this.event = event;
        }

        public EntityDamageByEntityEvent getEvent() { return event; }

        public static BukkitAttack of(CalibrePlugin plugin, EntityDamageByEntityEvent event, EquipmentSlot slot) {
            LivingEntity damager = (LivingEntity) event.getDamager();
            ItemStack item = damager.getEquipment().getItem(slot);
            return new BukkitAttack(item, new EntityItemSlot(damager, slot), plugin.userOf(damager),
                    event.getEntity(), event.getFinalDamage(), BukkitDamageCause.of(event.getCause()), event.getDamage(), event);
        }
    }
}
