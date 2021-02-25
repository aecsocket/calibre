package com.gitlab.aecsocket.calibre.paper.system.gun.reload.internal;

import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.reload.internal.InsertReloadSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.paper.util.ItemAnimation;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
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

    @Override public CalibrePlugin calibre() { return plugin; }

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
