package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.builtin.ProjectileSystem;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.data.SoundDataStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.SoundData;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public class PaperGunSystem extends GunSystem {
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(GunSystem.DEFAULT_STATS)
            .init("fire_sound", new SoundDataStat())
            .get();
    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public PaperGunSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperGunSystem() {}

    public PaperGunSystem(GunSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperGunSystem(PaperGunSystem o) {
        this(o, o.plugin);
    }

    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override
    protected int listenerPriority() {
        return plugin.setting("system", ID, "listener_priority").getInt(500);
    }

    @Override
    protected void fireSuccess(Events.Fire<?> event, CalibreSlot chamberSlot, ChamberSystem chamberSystem, ProjectileSystem projectileSystem) {
        ItemUser user = event.user();
        if (user instanceof BukkitItemUser)
            SoundData.play(() -> VectorUtils.toBukkit(user.position()).toLocation(((BukkitItemUser) user).world()), tree().stat("fire_sound"));
        super.fireSuccess(event, chamberSlot, chamberSystem, projectileSystem);
    }

    @Override public PaperGunSystem copy() { return new PaperGunSystem(this); }
}
