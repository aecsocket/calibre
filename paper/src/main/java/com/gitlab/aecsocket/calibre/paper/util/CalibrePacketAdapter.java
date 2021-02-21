package com.gitlab.aecsocket.calibre.paper.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.system.BukkitItemEvents;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CalibrePacketAdapter extends PacketAdapter {
    private final CalibrePlugin plugin;

    public CalibrePacketAdapter(CalibrePlugin plugin) {
        super(plugin,
                PacketType.Play.Server.SET_SLOT,
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.ENTITY_EQUIPMENT,
                PacketType.Play.Server.ANIMATION);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        if (type == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = packet.getItemModifier().read(0);
            if (plugin.itemManager().hidden(item)) {
                event.setCancelled(true);
            }
        }

        if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            List<ItemStack> items = packet.getItemListModifier().read(0);
            for (int i = 0; i < items.size(); i++) {
                if (plugin.itemManager().hidden(items.get(i))) {
                    items.set(i, new ItemStack(Material.STICK));
                    break;
                }
            }
        }

        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            Entity holder = packet.getEntityModifier(event).read(0);
            for (Pair<EnumWrappers.ItemSlot, ItemStack> equipment : packet.getSlotStackPairLists().read(0)) {
                ItemStack item = equipment.getSecond();
                PaperComponent component = plugin.itemManager().get(item);
                if (component != null) {
                    if (component.tree().call(new BukkitItemEvents.ShowItem(item, component, player, holder)).cancelled())
                        event.setCancelled(true);
                }
            }
        }

        if (type == PacketType.Play.Server.ANIMATION) {
            Entity entity = packet.getEntityModifier(event).read(0);
            if (!(entity instanceof LivingEntity))
                return;
            LivingEntity holder = (LivingEntity) entity;
            if (handleAnimation(EquipmentSlot.HAND, player, holder))
                event.setCancelled(true);
            if (handleAnimation(EquipmentSlot.OFF_HAND, player, holder))
                event.setCancelled(true);
        }
    }

    private boolean handleAnimation(EquipmentSlot slot, Player player, LivingEntity holder) {
        ItemStack item = holder.getEquipment().getItem(slot);
        PaperComponent component = plugin.itemManager().get(item);
        if (component != null)
            return component.tree().call(new BukkitItemEvents.Swing(item, component, player, holder)).cancelled();
        return false;
    }
}
