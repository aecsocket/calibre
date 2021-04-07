package com.gitlab.aecsocket.calibre.paper.util;

import com.gitlab.aecsocket.calibre.paper.gui.SlotViewGUI;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.system.BukkitItemEvents;
import com.gitlab.aecsocket.unifiedframework.paper.gui.GUIView;
import com.gitlab.aecsocket.unifiedframework.core.loop.MinecraftSyncLoop;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.loop.Tickable;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.ParticleData;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import com.gitlab.aecsocket.unifiedframework.paper.util.plugin.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.UUID;

public final class CalibrePlayerData implements PlayerData {
    public static final ParticleData[] OFFSET_PARTICLE = {
            new ParticleData().particle(Particle.FIREWORKS_SPARK)
    };

    private final CalibrePlugin plugin;
    private final UUID uuid;
    private Player player;

    private final EnumMap<EquipmentSlot, PaperComponent> componentCache = new EnumMap<>(EquipmentSlot.class);

    private Vector3D offset;
    private ItemAnimation.Instance animation;

    private int cancelInteractTick;
    private int cancelBlockInteract;
    private int inventoryDrop;
    private int lastRightClick;

    private double inaccuracy;
    private boolean showInaccuracy;

    private double stamina;
    private double maxStamina;
    private long staminaRecoverAt;
    private boolean stabilizeBlocked;

    private Vector2D recoil = new Vector2D();
    private double recoilSpeed;
    private double recoilRecovery;
    private double recoilRecoverySpeed;
    private long recoilRecoveryAt;
    private Vector2D recoilToRecover = new Vector2D();

    public CalibrePlayerData(CalibrePlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        maxStamina = plugin.setting(n -> n.getDouble(5000), "stamina", "max");
        stamina = maxStamina;
    }

    public CalibrePlayerData(CalibrePlugin plugin, Player player) {
        this(plugin, player.getUniqueId());
        this.player = player;
    }

    public void reset() {
        stamina = maxStamina;
        recoil = new Vector2D();
        recoilToRecover = new Vector2D();
        inaccuracy = 0;
    }

    public CalibrePlugin plugin() { return plugin; }
    public Player player() { return player; }

    public Vector3D offset() { return offset; }
    public void offset(Vector3D offset) { this.offset = offset; }

    public ItemAnimation.Instance animation() { return animation; }
    public void animation(ItemAnimation.Instance animation) { this.animation = animation; }

    public int cancelInteractTick() { return cancelInteractTick; }
    public boolean cancelledInteract() { return Bukkit.getCurrentTick() <= cancelInteractTick; }
    public void cancelInteract() { cancelInteractTick = Bukkit.getCurrentTick() + 3; }

    public int cancelBlockInteractTick() { return cancelBlockInteract; }
    public boolean cancelledBlockInteract() { return Bukkit.getCurrentTick() <= cancelBlockInteract ; }
    public void cancelBlockInteract() { cancelBlockInteract = Bukkit.getCurrentTick() + 1; }

    public int inventoryDropTick() { return inventoryDrop; }
    public boolean isInventoryDrop() { return Bukkit.getCurrentTick() <= inventoryDrop; }
    public void setInventoryDrop() { inventoryDrop = Bukkit.getCurrentTick() + 1; }

    public int lastRightClick() { return lastRightClick; }
    public void lastRightClick(int lastRightClick) { this.lastRightClick = lastRightClick; }

    public double inaccuracy() { return inaccuracy; }
    public void inaccuracy(double inaccuracy) { this.inaccuracy = inaccuracy; }

    public boolean showInaccuracy() { return showInaccuracy; }
    public void showInaccuracy(boolean showInaccuracy) { this.showInaccuracy = showInaccuracy; }

    public double stamina() { return stamina; }
    public void stamina(double stamina) { this.stamina = stamina; }

    public double maxStamina() { return maxStamina; }
    public void maxStamina(double maxStamina) { this.maxStamina = maxStamina; }

    public long staminaRecoverAt() { return staminaRecoverAt; }
    public void staminaRecoverAt(long staminaRecoverAt) { this.staminaRecoverAt = staminaRecoverAt; }

    public boolean stabilizeBlocked() { return stabilizeBlocked; }
    public void stabilizeBlocked(boolean stabilizeBlocked) { this.stabilizeBlocked = stabilizeBlocked; }

    public Vector2D recoil() { return recoil; }
    public void recoil(Vector2D recoil) { this.recoil = recoil; }

    public double recoilSpeed() { return recoilSpeed; }
    public void recoilSpeed(double recoilSpeed) { this.recoilSpeed = recoilSpeed; }

    public double recoilRecovery() { return recoilRecovery; }
    public void recoilRecovery(double recoilRecovery) { this.recoilRecovery = recoilRecovery; }

    public double recoilRecoverySpeed() { return recoilRecoverySpeed; }
    public void recoilRecoverySpeed(double recoilRecoverySpeed) { this.recoilRecoverySpeed = recoilRecoverySpeed; }

    public long recoilRecoveryAt() { return recoilRecoveryAt; }
    public void recoilRecoveryAt(long recoilRecoveryAfter) { this.recoilRecoveryAt = recoilRecoveryAfter; }

    public Vector2D recoilToRecover() { return recoilToRecover; }
    public void recoilToRecover(Vector2D recoilRecoveryRemaining) { this.recoilToRecover = recoilRecoveryRemaining; }

    @Override public PlayerData load() { return this; }
    @Override public PlayerData save() { return this; }

    @Override
    public PlayerData join(Player player) {
        this.player = player;
        return this;
    }


    public void applyRecoil(Vector2D recoil, double recoilSpeed, double recoilRecovery, double recoilRecoverySpeed, long recoilRecoveryAfter) {
        this.recoil = this.recoil.add(recoil.y(-recoil.y()));
        this.recoilSpeed = recoilSpeed;
        this.recoilRecovery = recoilRecovery;
        this.recoilRecoverySpeed = recoilRecoverySpeed;
        recoilRecoveryAt = System.currentTimeMillis() + recoilRecoveryAfter;
    }

    public void applyRotation(Vector2D vector) {
        CalibreProtocol.rotate(player, vector.x(), vector.y());
    }

    public void reduceStamina(double amount) {
        stamina -= amount;
        if (stamina <= 0) {
            stamina = 0;
            stabilizeBlocked = true;
        }
        staminaRecoverAt = System.currentTimeMillis() + plugin.setting(n -> n.getLong(2000), "stamina", "recover_after");
    }

    public boolean stabilize() {
        return player.isSneaking() && !stabilizeBlocked;
    }

    public void bukkitSyncTick(TickContext tickContext) {
        // equipped
        EntityEquipment equipment = player.getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            PaperComponent component = plugin.itemManager().get(equipment.getItem(slot));
            componentCache.put(slot, null);
            if (component != null) {
                component.tree().call(BukkitItemEvents.BukkitEquipped.of(component, player, slot, tickContext));
                componentCache.put(slot, component);
            }
        }

        // offset
        if (offset != null) {
            Location eyeLoc = player.getEyeLocation();
            Location location = VectorUtils.toBukkit(Utils.relativeOffset(VectorUtils.toUF(eyeLoc.toVector()), VectorUtils.toUF(eyeLoc.getDirection()), offset)).toLocation(player.getWorld());
            ParticleData.spawn(player, location, plugin.setting(n -> n.get(ParticleData[].class, OFFSET_PARTICLE), "offset_particle"));
        }

        // show inaccuracy
        if (showInaccuracy)
            player.sendTitle("", String.format("%.3f", inaccuracy), 0, 5, 0);

        // slot view
        GUIView view = plugin.guiManager().getView(player);
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            SlotViewGUI gui = (SlotViewGUI) view.getGUI();
            gui.update(view);
        }

        // inaccuracy
        inaccuracy -= tickContext.delta() / 1000d;
        if (inaccuracy < 0)
            inaccuracy = 0;

        // stamina
        if (!player.isSneaking())
            stabilizeBlocked = false;

        if (stamina < maxStamina) {
            if (System.currentTimeMillis() >= staminaRecoverAt) {
                stamina += plugin.setting(n -> n.getDouble(1000), "stamina", "recover") * (tickContext.delta() / 1000d);
                if (stamina > maxStamina)
                    stamina = maxStamina;
            }

            if (plugin.setting(n -> n.getBoolean(true), "stamina", "show_in_air_bar"))
                CalibreProtocol.air(player, stamina / maxStamina);
        }

        // animation
        if (animation != null) {
            tickContext.tick(animation);
        }
    }

    public void threadTick(TickContext tickContext) {
        // equipped
        for (var entry : Collections.unmodifiableMap(componentCache).entrySet()) {
            PaperComponent component = entry.getValue();
            if (component != null)
                component.tree().call(BukkitItemEvents.BukkitEquipped.of(component, player, entry.getKey(), tickContext));
        }

        // recoil
        if (recoil.manhattanLength() > 0.05) {
            Vector2D rotation = recoil.multiply(recoilSpeed);
            CalibreProtocol.rotate(player, rotation.x(), rotation.y());
            recoil = recoil.multiply(1 - recoilSpeed);
            recoilToRecover = recoilToRecover.add(rotation.multiply(-recoilRecovery));
        }

        if (System.currentTimeMillis() >= recoilRecoveryAt && recoilToRecover.manhattanLength() > 0.05) {
            Vector2D rotation = recoilToRecover.multiply(recoilRecoverySpeed);
            CalibreProtocol.rotate(player, rotation.x(), rotation.y());
            recoilToRecover = recoilToRecover.multiply(1 - recoilRecoverySpeed);
        }
    }

    @Override
    public void tick(TickContext tickContext) {
        if (player == null)
            return;

        if (player.isDead())
            return;
        if (player.getGameMode() == GameMode.SPECTATOR)
            return;

        if (tickContext.loop() instanceof MinecraftSyncLoop) {
            bukkitSyncTick(tickContext);
        } else {
            threadTick(tickContext);
        }
    }
}
