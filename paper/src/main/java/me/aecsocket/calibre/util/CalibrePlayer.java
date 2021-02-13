package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.system.BukkitItemEvents;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.loop.MinecraftSyncLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.EnumMap;

public final class CalibrePlayer implements Tickable {
    public static final ParticleData[] OFFSET_PARTICLE = {
            new ParticleData().particle(Particle.FIREWORKS_SPARK)
    };

    private final CalibrePlugin plugin;
    private final Player player;
    private final EnumMap<EquipmentSlot, PaperComponent> componentCache = new EnumMap<>(EquipmentSlot.class);

    private Vector3D offset;
    private ItemAnimation.Instance animation;

    private int cancelInteractTick;
    private int cancelBlockInteract;
    private int inventoryDrop;
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

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        maxStamina = plugin.setting("stamina", "max").getDouble(5000);
        stamina = maxStamina;
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
        staminaRecoverAt = System.currentTimeMillis() + plugin.setting("stamina", "recover_after").getLong(2000);
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
            try {
                ParticleData.spawn(player, location, plugin.setting("offset_particle").get(ParticleData[].class, OFFSET_PARTICLE));
            } catch (SerializationException ignore) {}
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
                stamina += plugin.setting("stamina", "recover").getDouble(1000) * (tickContext.delta() / 1000d);
                if (stamina > maxStamina)
                    stamina = maxStamina;
            }

            if (plugin.setting("stamina", "show_in_air_bar").getBoolean(true))
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
