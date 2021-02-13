package me.aecsocket.calibre.system.gun.reload.internal;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.system.builtin.SchedulerSystem;
import me.aecsocket.calibre.system.gun.GunSystem;
import me.aecsocket.calibre.system.gun.reload.external.RemoveReloadSystem;
import me.aecsocket.calibre.util.ItemAnimation;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.data.SoundDataStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class PaperInsertReloadSystem extends InsertReloadSystem implements PaperSystem {
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(InsertReloadSystem.DEFAULT_STATS)
            .init("load_sound", new SoundDataStat())
            .init("load_animation", new ItemAnimation.Stat())

            .init("load_end_sound", new SoundDataStat())
            .init("load_end_animation", new ItemAnimation.Stat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    public PaperInsertReloadSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperInsertReloadSystem() {}

    public PaperInsertReloadSystem(PaperInsertReloadSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public <I extends Item> void reload(GunSystem.Events.InternalReload<I> event) {
        super.reload(event);
        ItemUser user = event.user();
        if (user instanceof BukkitItemUser && event.result() == ItemEvents.Result.SUCCESS) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("load_sound"));
            if (user instanceof PlayerUser) {
                Player player = ((PlayerUser) user).entity();
                ItemAnimation.start(player, player.getInventory().getHeldItemSlot(), tree().stat("load_animation"));
            }
        }
    }

    @Override
    protected <I extends Item> void insertAmmo(ItemEvents.Equipped<I> equip, SchedulerSystem.TreeContext ctx, GunSystem.Events.InternalReload<I> event) {
        super.insertAmmo(equip, ctx, event);
        ItemUser user = event.user();
        if (user instanceof BukkitItemUser) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("load_end_sound"));
            ItemAnimation.start(user, equip.slot(), tree().stat("load_end_animation"));
        }
    }

    @Override public PaperInsertReloadSystem copy() { return new PaperInsertReloadSystem(this); }
}
