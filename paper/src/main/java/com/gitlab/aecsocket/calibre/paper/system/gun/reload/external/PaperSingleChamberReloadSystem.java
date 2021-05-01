package com.gitlab.aecsocket.calibre.paper.system.gun.reload.external;

import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.reload.external.SingleChamberReloadSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.paper.util.ItemAnimation;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.core.scheduler.TaskContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PaperSingleChamberReloadSystem extends SingleChamberReloadSystem implements PaperSystem {
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(SingleChamberReloadSystem.STAT_TYPES)
            .init("reload_sound", new SoundDataStat())
            .init("reload_animation", new ItemAnimation.Stat())

            .init("reload_single_sound", new SoundDataStat())
            .init("reload_single_animation", new ItemAnimation.Stat())

            .init("reload_end_sound", new SoundDataStat())
            .init("reload_end_animation", new ItemAnimation.Stat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    public PaperSingleChamberReloadSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    private PaperSingleChamberReloadSystem() {}

    public PaperSingleChamberReloadSystem(PaperSingleChamberReloadSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }

    @Override
    public <I extends Item> void reload(GunSystem.Events.ExternalReload<I> event) {
        super.reload(event);
        ItemUser user = event.user();
        if (user instanceof BukkitItemUser && event.result() == ItemEvents.Result.SUCCESS) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("reload_sound"));
            ItemAnimation.start(event.user(), event.slot(), tree().stat("reload_animation"));
        }
    }

    @Override
    protected <I extends Item> boolean reloadSingle(ItemUser user, ItemSlot<I> slot, TaskContext ctx, SchedulerSystem scheduler, GunSystem.Events.ExternalReload<I> event) {
        boolean result = super.reloadSingle(user, slot, ctx, scheduler, event);
        if (!result) {
            if (user instanceof BukkitItemUser) {
                SoundData.play(((BukkitItemUser) user)::location, tree().stat("reload_single_sound"));
                ItemAnimation.start(user, slot, tree().stat("reload_single_animation"));
            }
        }
        return result;
    }

    @Override
    protected <I extends Item> void finishReload(ItemUser user, ItemSlot<I> slot, TaskContext ctx, GunSystem.Events.ExternalReload<I> event) {
        super.finishReload(user, slot, ctx, event);
        if (user instanceof BukkitItemUser) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("reload_end_sound"));
            ItemAnimation.start(user, slot, tree().stat("reload_end_animation"));
        }
    }

    @Override public PaperSingleChamberReloadSystem copy() { return new PaperSingleChamberReloadSystem(this); }
}
