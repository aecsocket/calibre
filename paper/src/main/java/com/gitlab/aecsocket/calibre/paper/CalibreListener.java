package com.gitlab.aecsocket.calibre.paper;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.gui.SlotViewGUI;
import com.gitlab.aecsocket.calibre.paper.util.CalibrePacketAdapter;
import com.gitlab.aecsocket.calibre.paper.util.CalibrePlayerData;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.EntityEquipmentSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.InventorySlot;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.system.BukkitItemEvents;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.BukkitSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.gitlab.aecsocket.unifiedframework.paper.gui.GUIView;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CalibreListener implements Listener {
    private final CalibrePlugin plugin;

    public CalibreListener(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    private void callOn(ItemStack item, Consumer<PaperComponent> function) {
        PaperComponent component = plugin.itemManager().get(item);
        if (component == null)
            return;
        function.accept(component);
    }

    private void callOn(LivingEntity entity, EquipmentSlot slot, BiConsumer<PaperComponent, EquipmentSlot> function) {
        if (entity instanceof Player && ((Player) entity).getGameMode() == GameMode.SPECTATOR)
            return;
        callOn(entity.getEquipment().getItem(slot), comp -> function.accept(comp, slot));
    }

    private void callOn(Player player, int slot, BiConsumer<PaperComponent, Integer> function) {
        if (player.getGameMode() == GameMode.SPECTATOR)
            return;
        callOn(player.getInventory().getItem(slot), comp -> function.accept(comp, slot));
    }

    @EventHandler
    public void onEvent(PlayerItemBreakEvent event) {
        if (plugin.itemManager().get(event.getBrokenItem()) != null) {
            // TODO
        }
    }

    @EventHandler
    public void onEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        callOn(player, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(new BukkitItemEvents.BukkitSwitch(
                comp, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot), ItemEvents.Switch.TO)));
    }

    @EventHandler
    public void onEvent(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        for (EquipmentSlot slot : EquipmentSlot.values())
            callOn(player, slot, (comp, __) -> comp.tree().call(BukkitItemEvents.BukkitJump.of(event, comp, slot)));
    }

    @EventHandler
    public void onEvent(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        for (EquipmentSlot slot : EquipmentSlot.values())
            callOn(player, slot, (comp, __) -> comp.tree().call(BukkitItemEvents.BukkitToggleSprint.of(event, comp, slot)));
    }

    @EventHandler
    public void onEvent(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Inventory inv = player.getInventory();
        callOn(player, event.getPreviousSlot(), (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitScroll.of(event, comp)));
        handleSwitch(player, () -> event.setCancelled(true),
                InventorySlot.of(inv, event.getPreviousSlot()),
                InventorySlot.of(inv, event.getNewSlot())
        );
    }

    @EventHandler
    public void onEvent(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        callOn(player, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitSwapHand.of(event, comp, slot)));
        callOn(player, EquipmentSlot.OFF_HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitSwapHand.of(event, comp, slot)));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEvent(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) // creative mode: quit inventory = drop
            return;
        CalibrePlayerData data = plugin.playerData(event.getPlayer());
        data.cancelInteract();
        boolean inventoryDrop = data.isInventoryDrop();
        callOn(event.getItemDrop().getItemStack(), comp -> comp.tree().call(BukkitItemEvents.BukkitDrop.of(event, comp, !inventoryDrop)));
    }

    @EventHandler
    public void onEvent(EntityDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        plugin.itemManager().hide(item, false);
    }

    @EventHandler
    public void onEvent(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        plugin.itemManager().hide(item, false);
    }

    @EventHandler
    public void onEvent(InventoryClickEvent event) {
        if (event.getWhoClicked().getGameMode() == GameMode.SPECTATOR)
            return;
        ItemStack clickedStack = event.getCurrentItem();
        ItemStack cursorStack = event.getCursor();
        PaperComponent clicked = plugin.itemManager().get(clickedStack);
        PaperComponent cursor = plugin.itemManager().get(cursorStack);
        ClickType type = event.getClick();
        Player player = (Player) event.getWhoClicked();
        CalibrePlayerData data = plugin.playerData(player);
        data.cancelInteract();
        data.setInventoryDrop();

        GUIView view = plugin.guiManager().getView(player);
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            if (event.getClickedInventory() == event.getView().getTopInventory())
                return;
        }

        if (clicked != null && clicked.tree().call(BukkitItemEvents.BukkitItemClick.of(event, clicked)).cancelled())
            return;

        if (clicked != null && cursor != null && type == ClickType.LEFT && plugin.setting(n -> n.getBoolean(true), "quick_modify", "enabled")) {
            CalibreSlot slot = clicked.combine(cursor, plugin.setting(n -> n.getBoolean(true), "quick_modify", "limited"));
            if (slot != null) {
                SoundData.play(player::getLocation, cursor.tree().stat("modify_sound"));
                clicked.buildTree();
                event.setCancelled(true);
                Locale locale = player.locale();

                int cursorAmount = cursorStack.getAmount();
                int clickedAmount = clickedStack.getAmount();
                if (cursorAmount >= clickedAmount) {
                    event.setCurrentItem(clicked.create(locale, clickedAmount).item());
                    cursorStack.subtract(clickedAmount);
                } else {
                    clickedStack.subtract(cursorAmount);
                    event.getView().setCursor(clicked.create(locale, cursorAmount).item());
                }
            }
            return;
        }

        if (clicked != null && cursor == null && type == ClickType.RIGHT && plugin.setting(n -> n.getBoolean(true), "slot_view", "enabled")) {
            event.setCancelled(true);
            SlotViewGUI.of(plugin, clicked, BukkitSlot.of(event::getCurrentItem, event::setCurrentItem)).open(player);
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        PlayerInventory playerInv = player.getInventory();
        int clickedSlot = event.getSlot();
        int heldSlot = player.getInventory().getHeldItemSlot();
        if (type == ClickType.NUMBER_KEY) {
            /*
            TODO number key stuff

            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot == heldSlot) {
                handleSwitch(player, () -> event.setCancelled(true),
                        InventorySlot.of(clickedInv, hotbarSlot),
                        playerInvClicked ? BukkitSlot.of(event::getCurrentItem, event::setCurrentItem) : null
                );
            }
            if (clickedSlot == heldSlot) {
                handleSwitch(player, () -> event.setCancelled(true),
                        playerInvClicked ? BukkitSlot.of(event::getCurrentItem, event::setCurrentItem) : null,
                        InventorySlot.of(clickedInv, hotbarSlot)
                );
            }*/
        } else if (clickedInv == playerInv && clickedSlot == heldSlot) {
            handleSwitch(player, () -> event.setCancelled(true),
                    BukkitSlot.of(event::getCurrentItem, event::setCurrentItem),
                    BukkitSlot.of(event::getCursor, event.getView()::setCursor)
            );
        }
    }

    @EventHandler
    public void onEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        callOn(player, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitBreakBlock.of(event, comp, slot)));
        callOn(player, EquipmentSlot.OFF_HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitBreakBlock.of(event, comp, slot)));
    }

    @EventHandler
    public void onEvent(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        callOn(player, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitPlaceBlock.of(event, comp, slot)));
        callOn(player, EquipmentSlot.OFF_HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitPlaceBlock.of(event, comp, slot)));
    }

    @EventHandler
    public void onEvent(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        callOn(entity, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitDeath.of(event, comp, slot)));
        callOn(entity, EquipmentSlot.OFF_HAND, (comp, slot) -> comp.tree().call(BukkitItemEvents.BukkitDeath.of(event, comp, slot)));
        if (entity instanceof Player)
            plugin.playerData((Player) entity).reset();
    }

    private void handleSwitch(Player player, Runnable cancelled, ItemSlot<BukkitItem> from, ItemSlot<BukkitItem> to) {
        PlayerUser user = PlayerUser.of(player);
        PaperComponent fromComp;
        if (from != null && from.get() != null && (fromComp = plugin.itemManager().get(from.get().item())) != null) {
            BukkitItemEvents.BukkitSwitch event = new BukkitItemEvents.BukkitSwitch(fromComp, user, from, ItemEvents.Switch.FROM);
            fromComp.tree().call(event);
            if (event.cancelled()) {
                cancelled.run();
                return;
            }
        }

        PaperComponent toComp;
        if (to != null && to.get() != null && (toComp = plugin.itemManager().get(to.get().item())) != null) {
            BukkitItemEvents.BukkitSwitch event = new BukkitItemEvents.BukkitSwitch(toComp, user, to, ItemEvents.Switch.TO);
            toComp.tree().call(event);
            if (event.cancelled())
                cancelled.run();
        }
    }

    @EventHandler
    public void onEvent(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (plugin.playerData(player).cancelledInteract())
            return;

        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING)
            return;

        class AnimationClick extends ItemEvents.Base<BukkitItem> implements ItemEvents.GameClick<BukkitItem> {
            public AnimationClick(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
                super(component, user, slot);
            }

            @Override public boolean cancelled() { return event.isCancelled(); }
            @Override public void cancel() { event.setCancelled(true); }

            @Override public int type() { return ItemEvents.GameClick.LEFT; }
            @Override public boolean holding() { return false; }
        }

        callOn(player, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(new AnimationClick(comp, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot))));
        callOn(player, EquipmentSlot.OFF_HAND, (comp, slot) -> comp.tree().call(new AnimationClick(comp, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot))));
    }

    @EventHandler
    public void onEvent(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        Player player = event.getPlayer();
        if (plugin.playerData(player).cancelledInteract())
            return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
            return; // handled in player animation

        CalibrePlayerData data = plugin.playerData(player);
        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (data.cancelledBlockInteract())
                return;
            data.cancelBlockInteract();
        }

        boolean holding = Bukkit.getCurrentTick() - data.lastRightClick() <= 5;
        class InteractClick extends ItemEvents.Base<BukkitItem> implements ItemEvents.GameClick<BukkitItem> {
            public InteractClick(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
                super(component, user, slot);
            }

            @Override public boolean cancelled() { return event.useItemInHand() == Event.Result.DENY && event.useInteractedBlock() == Event.Result.DENY; }
            @Override public void cancel() { event.setCancelled(true); }

            @Override public int type() { return ItemEvents.GameClick.RIGHT; }
            @Override public boolean holding() { return holding; }
        }

        data.lastRightClick(Bukkit.getCurrentTick());

        callOn(player, EquipmentSlot.HAND, (comp, slot) -> comp.tree().call(new InteractClick(comp, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot))));
        callOn(player, EquipmentSlot.OFF_HAND, (comp, slot) -> comp.tree().call(new InteractClick(comp, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot))));
    }
}
