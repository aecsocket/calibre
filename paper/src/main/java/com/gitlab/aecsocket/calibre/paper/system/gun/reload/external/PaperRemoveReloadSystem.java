package com.gitlab.aecsocket.calibre.paper.system.gun.reload.external;

import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.reload.external.RemoveReloadSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
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

public final class PaperRemoveReloadSystem extends RemoveReloadSystem implements PaperSystem {
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(RemoveReloadSystem.STAT_TYPES)
            .init("unload_sound", new SoundDataStat())
            .init("unload_animation", new ItemAnimation.Stat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    public PaperRemoveReloadSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    private PaperRemoveReloadSystem() {}

    public PaperRemoveReloadSystem(PaperRemoveReloadSystem o) {
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
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("unload_sound"));
            if (user instanceof PlayerUser) {
                Player player = ((PlayerUser) user).entity();
                ItemAnimation.start(player, player.getInventory().getHeldItemSlot(), tree().stat("unload_animation"));
            }
        }
    }

    @Override public PaperRemoveReloadSystem copy() { return new PaperRemoveReloadSystem(this); }
}
