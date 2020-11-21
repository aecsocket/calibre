package me.aecsocket.calibre.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.Vector2;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class CalibrePlayer implements Tickable {
    public static final CalibreParticleData[] VIEW_OFFSET_PARTICLE = { new CalibreParticleData(Particle.REDSTONE, 0, new Vector(), 0, new Particle.DustOptions(Color.WHITE, 0.5f)) };
    public static final double CAMERA_THRESHOLD = 0.2;

    private final CalibrePlugin plugin;
    private final Player player;
    private final PlayerItemUser user;
    private final Map<EquipmentSlot, Map.Entry<ItemStack, CalibreComponent>> cachedItems = new EnumMap<>(EquipmentSlot.class);

    private ItemAnimation.Instance animation;
    private Vector viewOffset;
    private Vector2 recoil = new Vector2();
    private double recoilSpeed;
    private double recoilRecovery;
    private long recoilRecoveryAfter;
    private double recoilRecoverySpeed;
    private Vector2 recoilToRecover = new Vector2();

    private int stamina = -1;
    private int maxStamina = -1;
    private long lastStaminaDrain;
    private boolean canUseStamina = true;

    // TODO maybe dont have "shader data" as a native feature lol
    private int shaderDataId = -1;
    private float shaderDataA = 1f;

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        user = new PlayerItemUser(player, this);
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }
    public PlayerItemUser getUser() { return user; }

    public ItemAnimation.Instance getAnimation() { return animation; }
    public void setAnimation(ItemAnimation.Instance animation) { this.animation = animation; }

    public Vector getViewOffset() { return viewOffset; }
    public void setViewOffset(Vector viewOffset) { this.viewOffset = viewOffset; }

    public Vector2 getRecoil() { return recoil; }
    public void setRecoil(Vector2 recoil) { this.recoil = recoil; }

    public double getRecoilSpeed() { return recoilSpeed; }
    public void setRecoilSpeed(double recoilSpeed) { this.recoilSpeed = recoilSpeed; }

    public double getRecoilRecovery() { return recoilRecovery; }
    public void setRecoilRecovery(double recoilRecovery) { this.recoilRecovery = recoilRecovery; }

    public long getRecoilRecoveryAfter() { return recoilRecoveryAfter; }
    public void setRecoilRecoveryAfter(long recoilRecoveryAfter) { this.recoilRecoveryAfter = recoilRecoveryAfter; }

    public double getRecoilRecoverySpeed() { return recoilRecoverySpeed; }
    public void setRecoilRecoverySpeed(double recoilRecoverySpeed) { this.recoilRecoverySpeed = recoilRecoverySpeed; }

    public Vector2 getRecoilToRecover() { return recoilToRecover; }
    public void setRecoilToRecover(Vector2 recoilToRecover) { this.recoilToRecover = recoilToRecover; }

    public int getStamina() { return stamina; }
    public void setStamina(int stamina) { this.stamina = stamina; }

    public int getMaxStamina() { return maxStamina; }
    public void setMaxStamina(int maxStamina) { this.maxStamina = maxStamina; }

    public long getLastStaminaDrain() { return lastStaminaDrain; }
    public void setLastStaminaDrain(long lastStaminaDrain) { this.lastStaminaDrain = lastStaminaDrain; }

    public boolean canUseStamina() { return canUseStamina; }
    public void setCanUseStamina(boolean canUseStamina) { this.canUseStamina = canUseStamina; }

    public int getShaderDataId() { return shaderDataId; }
    public void setShaderDataId(int shaderDataId) { this.shaderDataId = shaderDataId; }

    public float getShaderDataA() { return shaderDataA; }
    public void setShaderDataA(float shaderDataA) { this.shaderDataA = shaderDataA; }

    public ItemAnimation.Instance startAnimation(ItemAnimation animation, EquipmentSlot slot) {
        this.animation = animation.start(player, slot);
        return this.animation;
    }

    public void applyRecoil(Vector2 recoil, double recoilSpeed, double recoilRecovery, long recoilRecoveryAfter, double recoilRecoverySpeed) {
        this.recoil.add(recoil.getX(), -recoil.getY());
        this.recoilSpeed = recoilSpeed;
        this.recoilRecovery = recoilRecovery;
        this.recoilRecoveryAfter = System.currentTimeMillis() + recoilRecoveryAfter;
        this.recoilRecoverySpeed = recoilRecoverySpeed;
    }

    public void rotateCamera(double x, double y) {
        CalibreProtocol.rotateCamera(player, x, y);
    }

    @Override
    public void tick(TickContext tickContext) {
        if (animation != null && !animation.isFinished()) {
            tickContext.tick(animation);
        }

        if (tickContext.getLoop() instanceof SchedulerLoop) {
            //TODO testing
            ProtocolManager protocol = plugin.getProtocolManager();
            if (shaderDataId != -1) {
                PacketContainer destroyPacket = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroyPacket.getIntegerArrays().write(0, new int[] { shaderDataId });
                plugin.sendPacket(player, destroyPacket);
            }

            shaderDataId = (int) (Math.random() * Integer.MAX_VALUE);

            PacketContainer entityPacket = protocol.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            entityPacket.getIntegers().write(0, shaderDataId); // Entity ID
            entityPacket.getUUIDs().write(0, UUID.randomUUID()); // UUID
            entityPacket.getEntityTypeModifier().write(0, EntityType.DROPPED_ITEM); // Entity type

            Location eye = player.getEyeLocation();
            entityPacket.getDoubles().write(0, eye.getX()); // X
            entityPacket.getDoubles().write(1, eye.getY() - 0.5); // Y
            entityPacket.getDoubles().write(2, eye.getZ()); // Z

            entityPacket.getIntegers().write(1, 0); // Velocity X
            entityPacket.getIntegers().write(2, 0); // Velocity Y
            entityPacket.getIntegers().write(3, 0); // Velocity Z
            entityPacket.getIntegers().write(4, 0); // Pitch
            entityPacket.getIntegers().write(5, 0); // Yaw
            entityPacket.getIntegers().write(6, 0); // Data

            PacketContainer metaPacket = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            if (shaderDataA > 0)
                shaderDataA *= 0.75;
            ItemStack shaderItem = Utils.modMeta(new ItemStack(Material.WHITE_STAINED_GLASS), meta -> {
                meta.setCustomModelData((int) Utils.clamp(shaderDataA * 255, 1, 255));
            });
            metaPacket.getIntegers().write(0, shaderDataId); // Entity ID
            metaPacket.getWatchableCollectionModifier().write(0, Arrays.asList(
                    new WrappedWatchableObject(
                            new WrappedDataWatcher.WrappedDataWatcherObject(7, WrappedDataWatcher.Registry.getItemStackSerializer(false)),
                            BukkitConverters.getItemStackConverter().getGeneric(shaderItem)
                    )
            ));

            plugin.sendPacket(player, entityPacket);
            plugin.sendPacket(player, metaPacket);

            //END

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = player.getEquipment().getItem(slot);
                CalibreComponent component = plugin.fromItem(item);
                cachedItems.put(slot, component == null ? null : new AbstractMap.SimpleEntry<>(
                        item,
                        component
                ));
            }

            if (viewOffset != null)
                ParticleData.spawn(Utils.getFacingRelative(player.getEyeLocation(), viewOffset), VIEW_OFFSET_PARTICLE);

            GUIView view = plugin.getGUIManager().getView(player);
            if (view != null && view.getGUI() instanceof SlotViewGUI) {
                SlotViewGUI gui = (SlotViewGUI) view.getGUI();
                gui.validate(view);
            }
        } else {
            if (recoil.manhattanLength() > CAMERA_THRESHOLD) {
                Vector2 rotation = recoil.clone().multiply(recoilSpeed);
                rotateCamera(rotation.getX(), rotation.getY());
                recoil.multiply(1 - recoilSpeed);
                recoilToRecover.add(rotation.multiply(-recoilRecovery));

                if (recoil.manhattanLength() <= CAMERA_THRESHOLD)
                    recoil.zero();
            }

            if (recoilRecoveryAfter > 0 && System.currentTimeMillis() >= recoilRecoveryAfter) {
                Vector2 rotation = recoilToRecover.clone().multiply(recoilRecoverySpeed);
                rotateCamera(rotation.getX(), rotation.getY());
                recoilToRecover.multiply(1 - recoilRecoverySpeed);

                if (recoilToRecover.manhattanLength() <= CAMERA_THRESHOLD) {
                    recoilToRecover.zero();
                    recoilRecoveryAfter = 0;
                }
            }
        }

        cachedItems.forEach((slot, entry) -> {
            if (entry != null)
                entry.getValue().callEvent(new ItemEvents.Equip(entry.getKey(), new EntityItemSlot(player, slot), user, tickContext));
        });
    }
}
