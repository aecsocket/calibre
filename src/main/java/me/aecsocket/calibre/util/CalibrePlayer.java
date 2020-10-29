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

            if (viewOffset != null)
                ParticleData.spawn(Utils.getFacingRelative(player.getEyeLocation(), viewOffset), VIEW_OFFSET_PARTICLE);

            GUIView view = plugin.getGUIManager().getView(player);
            if (view != null && view.getGUI() instanceof SlotViewGUI) {
                SlotViewGUI gui = (SlotViewGUI) view.getGUI();
                gui.validate(view);
            }
        }
        cachedItems.forEach((slot, entry) -> {
            if (entry != null)
                entry.getValue().callEvent(new ItemEvents.Equip(entry.getKey(), new EntityItemSlot(player, slot), user, tickContext));
        });
    }
}
