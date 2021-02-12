package me.aecsocket.calibre.system.gun.reload.external;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.system.gun.GunSystem;
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

public class PaperSwapReloadSystem extends SwapReloadSystem implements PaperSystem {
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(SwapReloadSystem.DEFAULT_STATS)
            .init("reload_out_sound", new SoundDataStat())
            .init("reload_out_animation", new ItemAnimation.Stat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    public PaperSwapReloadSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperSwapReloadSystem() {}

    public PaperSwapReloadSystem(PaperSwapReloadSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public <I extends Item> void reload(GunSystem.Events.ExternalReload<I> event) {
        super.reload(event);
        ItemUser user = event.user();
        if (user instanceof BukkitItemUser && event.result() == ItemEvents.Result.SUCCESS) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("reload_out_sound"));
            if (user instanceof PlayerUser) {
                Player player = ((PlayerUser) user).entity();
                ItemAnimation.start(player, player.getInventory().getHeldItemSlot(), tree().stat("reload_out_animation"));
            }
        }
    }

    @Override public PaperSwapReloadSystem copy() { return new PaperSwapReloadSystem(this); }
}
