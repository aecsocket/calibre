package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.unifiedframework.event.Event;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
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
                                        player,
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
    }
}
