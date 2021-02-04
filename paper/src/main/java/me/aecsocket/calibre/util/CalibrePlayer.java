package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.system.BukkitItemEvents;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spongepowered.configurate.serialize.SerializationException;

public class CalibrePlayer implements Tickable {
    public static final ParticleData[] OFFSET_PARTICLE = {
            new ParticleData().particle(Particle.FIREWORKS_SPARK)
    };
    public static final PotionEffect EFFECT_MINING_FATIGUE = new PotionEffect(PotionEffectType.SLOW_DIGGING, 2, 127, false, false, false);

    private final CalibrePlugin plugin;
    private final Player player;
    private Vector3D offset;
    private ItemAnimation.Instance animation;

    private int cancelInteractTick;
    private double inaccuracy;

    private Vector2D recoil = new Vector2D();
    private double recoilSpeed;
    private double recoilRecovery;
    private double recoilRecoverySpeed;
    private long recoilRecoveryAfter;
    private Vector2D recoilToRecover = new Vector2D();

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public CalibrePlugin plugin() { return plugin; }
    public Player player() { return player; }

    public Vector3D offset() { return offset; }
    public void offset(Vector3D offset) { this.offset = offset; }

    public ItemAnimation.Instance animation() { return animation; }
    public void animation(ItemAnimation.Instance animation) { this.animation = animation; }

    public int cancelInteractTick() { return cancelInteractTick; }
    public boolean cancelledInteract() { return Bukkit.getCurrentTick() <= cancelInteractTick + 3; }
    public void cancelInteract() { cancelInteractTick = Bukkit.getCurrentTick(); }

    public double inaccuracy() { return inaccuracy; }
    public void inaccuracy(double inaccuracy) { this.inaccuracy = inaccuracy; }

    public Vector2D recoil() { return recoil; }
    public void recoil(Vector2D recoil) { this.recoil = recoil; }

    public double recoilSpeed() { return recoilSpeed; }
    public void recoilSpeed(double recoilSpeed) { this.recoilSpeed = recoilSpeed; }

    public double recoilRecovery() { return recoilRecovery; }
    public void recoilRecovery(double recoilRecovery) { this.recoilRecovery = recoilRecovery; }

    public double recoilRecoverySpeed() { return recoilRecoverySpeed; }
    public void recoilRecoverySpeed(double recoilRecoverySpeed) { this.recoilRecoverySpeed = recoilRecoverySpeed; }

    public long recoilRecoveryAfter() { return recoilRecoveryAfter; }
    public void recoilRecoveryAfter(long recoilRecoveryAfter) { this.recoilRecoveryAfter = recoilRecoveryAfter; }

    public Vector2D recoilToRecover() { return recoilToRecover; }
    public void recoilToRecover(Vector2D recoilRecoveryRemaining) { this.recoilToRecover = recoilRecoveryRemaining; }

    public void applyRecoil(Vector2D recoil, double recoilSpeed, double recoilRecovery, double recoilRecoverySpeed, long recoilRecoveryAfter) {
        this.recoil = this.recoil.add(recoil.y(-recoil.y()));
        this.recoilSpeed = recoilSpeed;
        this.recoilRecovery = recoilRecovery;
        this.recoilRecoverySpeed = recoilRecoverySpeed;
        this.recoilRecoveryAfter = System.currentTimeMillis() + recoilRecoveryAfter;
    }

    @Override
    public void tick(TickContext tickContext) {
        if (player.isDead())
            return;

        // offset
        if (offset != null) {
            Location eyeLoc = player.getEyeLocation();
            Location location = VectorUtils.toBukkit(Utils.relativeOffset(VectorUtils.toUF(eyeLoc.toVector()), VectorUtils.toUF(eyeLoc.getDirection()), offset)).toLocation(player.getWorld());
            try {
                ParticleData.spawn(player, location, plugin.setting("offset_particle").get(ParticleData[].class, OFFSET_PARTICLE));
            } catch (SerializationException ignore) {}
        }

        // equipped
        EntityEquipment equipment = player.getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            PaperComponent component = plugin.itemManager().component(equipment.getItem(slot));
            if (component != null)
                component.tree().call(BukkitItemEvents.BukkitEquipped.of(component, player, slot, tickContext));
        }

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

        // recoil
        if (recoil.manhattanLength() > 0.01) {
            Vector2D rotation = recoil.multiply(recoilSpeed);
            CalibreProtocol.rotate(player, rotation.x(), rotation.y());
            recoil = recoil.multiply(1 - recoilSpeed);
            recoilToRecover = recoilToRecover.add(rotation.multiply(-recoilRecovery));
        }

        if (System.currentTimeMillis() >= recoilRecoveryAfter && recoilToRecover.manhattanLength() > 0.01) {
            Vector2D rotation = recoilToRecover.multiply(recoilRecoverySpeed);
            CalibreProtocol.rotate(player, rotation.x(), rotation.y());
            recoilToRecover = recoilToRecover.multiply(1 - recoilRecoverySpeed);
        }

        // animation
        if (animation != null) {
            tickContext.tick(animation);
            if (!animation.finished())
                player.addPotionEffect(EFFECT_MINING_FATIGUE);
        }
    }
}
