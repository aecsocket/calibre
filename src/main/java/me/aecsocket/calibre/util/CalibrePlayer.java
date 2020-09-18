package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.util.itemuser.PlayerItemUser;
import me.aecsocket.calibre.util.protocol.CalibreProtocol;
import me.aecsocket.unifiedframework.event.Event;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.Vector2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Wrapper around {@link Player} for storing Calibre's data.
 */
public class CalibrePlayer implements Tickable {
    private final CalibrePlugin plugin;
    private final Player player;
    private Animation.Instance animation;
    private final EnumMap<EquipmentSlot, ItemRepresentation> cachedItems = new EnumMap<>(EquipmentSlot.class);
    private int lastClicked;
    private double spread;
    private long lastSpreadUpdate;

    private Vector2 recoil = new Vector2();
    private double recoilSpeed;
    private long recoverAfter;
    private double recoilRecovery;
    private double recoilRecoverySpeed;
    private Vector2 recoilToRecover = new Vector2();

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        for (EquipmentSlot slot : EquipmentSlot.values())
            cachedItems.put(slot, new ItemRepresentation());
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }

    public Animation.Instance getAnimation() { return animation; }
    public void setAnimation(Animation.Instance animation) { this.animation = animation; }

    public Map<EquipmentSlot, ItemRepresentation> getCachedItems() { return cachedItems; }

    public int getLastClicked() { return lastClicked; }
    public void setLastClicked(int lastClicked) { this.lastClicked = lastClicked; }

    public double getSpread() { return spread; }
    public void setSpread(double spread) { this.spread = spread; }

    public long getLastSpreadUpdate() { return lastSpreadUpdate; }
    public void setLastSpreadUpdate(long lastSpreadUpdate) { this.lastSpreadUpdate = lastSpreadUpdate; }

    public Vector2 getRecoil() { return recoil; }
    public void setRecoil(Vector2 recoil) { this.recoil = recoil; }

    public double getRecoilSpeed() { return recoilSpeed; }
    public void setRecoilSpeed(double recoilSpeed) { this.recoilSpeed = recoilSpeed; }

    public long getRecoverAfter() { return recoverAfter; }
    public void setRecoverAfter(long recoverAfter) { this.recoverAfter = recoverAfter; }

    public double getRecoilRecovery() { return recoilRecovery; }
    public void setRecoilRecovery(double recoilRecovery) { this.recoilRecovery = recoilRecovery; }

    public double getRecoilRecoverySpeed() { return recoilRecoverySpeed; }
    public void setRecoilRecoverySpeed(double recoilRecoverySpeed) { this.recoilRecoverySpeed = recoilRecoverySpeed; }

    public Vector2 getRecoilToRecover() { return recoilToRecover; }
    public void setRecoilToRecover(Vector2 recoilToRecover) { this.recoilToRecover = recoilToRecover; }

    private void callEvent(ItemStack stack, Event<?>... events) {
        CalibreItem item = plugin.getItem(stack, CalibreItem.class);
        if (item != null) {
            for (Event<?> event : events)
                item.callEvent(event);
        }
    }

    public Animation.Instance startAnimation(Animation animation, EquipmentSlot slot) {
        this.animation = animation.start(player, slot);
        return this.animation;
    }

    public void applyRecoil(Vector2 recoil, double recoilSpeed, long recoverAfter, double recoilRecovery, double recoilRecoverySpeed) {
        recoil = new Vector2(recoil.getX(), -recoil.getY());
        this.recoil.add(recoil);
        this.recoilSpeed = recoilSpeed;
        this.recoverAfter = System.currentTimeMillis() + recoverAfter;
        this.recoilRecovery = recoilRecovery;
        this.recoilRecoverySpeed = recoilRecoverySpeed;
    }

    @Override
    public void tick(TickContext tickContext) {
        if (tickContext.getLoop() instanceof SchedulerLoop) {
            EntityEquipment equipment = player.getEquipment();
            for (EquipmentSlot slot : EquipmentSlot.values())
                cachedItems.get(slot).set(equipment.getItem(slot), plugin);
        }

        cachedItems.forEach((slot, rep) -> {
                    if (rep.getItem() != null)
                        rep.getItem().callEvent(
                                new ItemEvents.Equip<>(
                                        rep.getStack(),
                                        slot,
                                        new PlayerItemUser(plugin, player),
                                        tickContext
                                )
                        );
                }
        );

        if (animation != null) {
            tickContext.tick(animation);
            if (animation != null && animation.isFinished())
                animation = null;
        }

        // Spread
        double spreadReduction = plugin.setting("gun.spread_reduction", double.class, 0.1) * (tickContext.getPeriod() / 1000d);
        spread *= 1 - spreadReduction;

        // Recoil
        if (recoil.manhattanLength() >= plugin.setting("gun.recoil_threshold", double.class, 0.01)) {
            Vector2 rotated = recoil.clone().multiply(recoilSpeed);
            CalibreProtocol.rotateCamera(player, rotated.getX(), rotated.getY());
            recoil.multiply(1 - recoilSpeed);
            recoilToRecover.add(rotated.multiply(-recoilRecovery));

            if (recoil.manhattanLength() < plugin.setting("gun.recoil_threshold", double.class, 0.01))
                recoil.zero();
        }

        if (recoverAfter > 0 && System.currentTimeMillis() >= recoverAfter) {
            Vector2 rotated = recoilToRecover.clone().multiply(recoilRecoverySpeed);
            CalibreProtocol.rotateCamera(player, rotated.getX(), rotated.getY());
            recoilToRecover.multiply(1 - recoilRecoverySpeed);

            if (recoilToRecover.manhattanLength() < plugin.setting("gun.recoil_threshold", double.class, 0.01)) {
                recoilToRecover.zero();
                recoverAfter = 0;
            }
        }
    }
}
