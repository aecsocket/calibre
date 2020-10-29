package me.aecsocket.calibre.util;

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
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.Map;

public class CalibrePlayer implements Tickable {
    public static final ParticleData[] VIEW_OFFSET_PARTICLE = { new ParticleData(Particle.REDSTONE, 0, new Vector(), 0, new Particle.DustOptions(Color.WHITE, 0.5f)) };

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

    @Override
    public void tick(TickContext tickContext) {
        if (animation != null) {
            tickContext.tick(animation);
            if (animation.isFinished())
                animation = null;
        }

        if (tickContext.getLoop() instanceof SchedulerLoop) {
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
            if (recoil.manhattanLength() > 0.005) {
                Vector2 rotation = recoil.clone().multiply(recoilSpeed);
                CalibreProtocol.rotateCamera(player, rotation.getX(), rotation.getY());
                recoil.multiply(1 - recoilSpeed);
                recoilToRecover.add(rotation.multiply(-recoilRecovery));

                if (recoil.manhattanLength() <= 0.005)
                    recoil.zero();
            }

            if (recoilRecoveryAfter > 0 && System.currentTimeMillis() >= recoilRecoveryAfter) {
                Vector2 rotation = recoilToRecover.clone().multiply(recoilRecoverySpeed);
                CalibreProtocol.rotateCamera(player, rotation.getX(), rotation.getY());
                recoilToRecover.multiply(1 - recoilRecoverySpeed);

                if (recoilToRecover.manhattanLength() <= 0.005) {
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
