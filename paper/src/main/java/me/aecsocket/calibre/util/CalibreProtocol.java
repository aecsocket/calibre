package me.aecsocket.calibre.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.MapInit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public final class CalibreProtocol {
    public static final int NO_SPRINT_FOOD = 6;

    private CalibreProtocol() {}

    private static CalibrePlugin plugin() { return CalibrePlugin.getInstance(); }

    public enum PlayerTeleportFlag {
        X, Y, Z, Y_ROT, X_ROT
    }

    public static final EnumMap<EquipmentSlot, Integer> SLOT_MAPPING = MapInit.of(new EnumMap<EquipmentSlot, Integer>(EquipmentSlot.class))
            .init(EquipmentSlot.OFF_HAND, 40)
            .init(EquipmentSlot.HEAD, 39)
            .init(EquipmentSlot.CHEST, 38)
            .init(EquipmentSlot.LEGS, 37)
            .init(EquipmentSlot.FEET, 36)
            .get();

    public static final Set<PlayerTeleportFlag> POSITION_FLAGS = new HashSet<>(Arrays.asList(
            PlayerTeleportFlag.X,
            PlayerTeleportFlag.Z,
            PlayerTeleportFlag.Y,
            PlayerTeleportFlag.X_ROT,
            PlayerTeleportFlag.Y_ROT
    ));

    public static final EquivalentConverter<PlayerTeleportFlag> TELEPORT_FLAG_CONVERTER = EnumWrappers.getGenericConverter(MinecraftReflection
            .getMinecraftClass("EnumPlayerTeleportFlags",
                    "PacketPlayOutPosition$EnumPlayerTeleportFlags"), PlayerTeleportFlag.class);

    // TODO figure out a formula for this
    public static double fovToProtocol(double fov) {
        return fov;
    }

    public static void fov(Player target, double fov) {
        plugin().sendPacket(target, PacketType.Play.Server.ABILITIES, packet -> {
            packet.getBooleans().write(0, target.isInvulnerable());
            packet.getBooleans().write(1, target.isFlying());
            packet.getBooleans().write(2, target.getAllowFlight());
            packet.getBooleans().write(3, target.getGameMode() == GameMode.CREATIVE);
            packet.getFloat().write(0, target.getFlySpeed() / 2);
            packet.getFloat().write(1, (float) fovToProtocol(fov));
        });
    }

    public static void food(Player target, int amount) {
        plugin().sendPacket(target, PacketType.Play.Server.UPDATE_HEALTH, packet -> {
            packet.getFloat().write(0, (float) target.getHealth());
            packet.getIntegers().write(0, amount);
            packet.getFloat().write(1, target.getSaturation());
        });
    }

    public static void allowSprint(Player target, boolean allow) {
        if (allow)
            food(target, target.getFoodLevel());
        else if (target.getFoodLevel() > NO_SPRINT_FOOD)
            food(target, NO_SPRINT_FOOD);
    }

    public static void rotate(Player target, double yaw, double pitch) {
        plugin().sendPacket(target, PacketType.Play.Server.POSITION, packet -> {
            packet.getDoubles().write(0, 0d);
            packet.getDoubles().write(1, 0d);
            packet.getDoubles().write(2, 0d);
            packet.getFloat().write(0, (float) yaw);
            packet.getFloat().write(1, (float) pitch);
            packet.getSets(TELEPORT_FLAG_CONVERTER).write(0, POSITION_FLAGS);
        });
    }

    public static void item(Player target, ItemStack item, int slot) {
        plugin().sendPacket(target, PacketType.Play.Server.SET_SLOT, packet -> {
            packet.getIntegers().write(0, -2);
            packet.getIntegers().write(1, slot);
            packet.getItemModifier().write(0, item);
        });
    }

    public static int slotOf(Player player, EquipmentSlot slot) {
        PlayerInventory inv = player.getInventory();
        if (slot == EquipmentSlot.HAND) return inv.getHeldItemSlot();
        return SLOT_MAPPING.get(slot);
    }

    public static void item(Player player, ItemStack item, EquipmentSlot slot) {
        item(player, item, slotOf(player, slot));
    }

    public static void air(Player target, int air) {
        plugin().sendPacket(target, PacketType.Play.Server.ENTITY_METADATA, packet -> {
            packet.getIntegers().write(0, target.getEntityId());
            packet.getWatchableCollectionModifier().write(0, Collections.singletonList(
                    new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(1, WrappedDataWatcher.Registry.get(Integer.class)), air)
            ));
        });
    }

    public static void air(Player target, double percent) {
        air(target, (int) (((Math.round(percent * 10d) / 10d) - 0.05) * target.getMaximumAir()));
    }


    public static void aimAnimation(Player target, ItemStack item, EquipmentSlot slot) {
        PacketContainer packetEquipment = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packetEquipment.getIntegers().write(0, target.getEntityId());
        packetEquipment.getSlotStackPairLists().write(0, Collections.singletonList(new Pair<>(
                slot == EquipmentSlot.OFF_HAND ? EnumWrappers.ItemSlot.OFFHAND : EnumWrappers.ItemSlot.MAINHAND,
                item
        )));

        PacketContainer metaPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metaPacket.getIntegers().write(0, target.getEntityId());
        metaPacket.getWatchableCollectionModifier().write(0, Collections.singletonList(
                new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(7, WrappedDataWatcher.Registry.get(Byte.class)), (byte) 1) // bow animation
        ));

        CalibrePlugin plugin = plugin();
        for (Player player : target.getWorld().getPlayers()) {
            if (player == target) continue;
            plugin.sendPacket(packetEquipment, player);
            plugin.sendPacket(metaPacket, player);
        }
    }
}
