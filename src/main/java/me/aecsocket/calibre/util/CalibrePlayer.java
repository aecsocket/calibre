package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.Map;

public class CalibrePlayer implements Tickable {
    private final CalibrePlugin plugin;
    private final Player player;
    private final PlayerItemUser user;
    private final Map<EquipmentSlot, Map.Entry<ItemStack, CalibreComponent>> cachedItems = new EnumMap<>(EquipmentSlot.class);

    private long availableIn;
    private ItemAnimation.Instance animation;

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        user = new PlayerItemUser(player, this);
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }
    public PlayerItemUser getUser() { return user; }

    public long getAvailableIn() { return availableIn; }
    public void setAvailableIn(long availableIn) { this.availableIn = availableIn; }

    public ItemAnimation.Instance getAnimation() { return animation; }
    public void setAnimation(ItemAnimation.Instance animation) { this.animation = animation; }

    public void applyDelay(long ms) { availableIn = System.currentTimeMillis() + ms; }
    public long getDelay() { return availableIn - System.currentTimeMillis(); }

    public ItemAnimation.Instance startAnimation(ItemAnimation animation, EquipmentSlot slot) {
        this.animation = animation.start(player, slot);
        return this.animation;
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
        }
        cachedItems.forEach((slot, entry) -> {
            if (entry != null)
                entry.getValue().callEvent(new ItemEvents.Equip(entry.getKey(), new EntityItemSlot(player, slot), user, tickContext));
        });
    }
}
