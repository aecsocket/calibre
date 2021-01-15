package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.SoundData;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperComponentContainerSystem extends ComponentContainerSystem<BukkitItem> {
    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public PaperComponentContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperComponentContainerSystem() {}

    public PaperComponentContainerSystem(ComponentContainerSystem<BukkitItem> o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperComponentContainerSystem(PaperComponentContainerSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override protected int listenerPriority() { return plugin.setting("system", ID, "component_container").getInt(1500); }

    @Override
    protected void remove(ItemEvents.Click<BukkitItem> event, Quantifier<CalibreComponent<BukkitItem>> last) {
        super.remove(event, last);
        if (event.user() instanceof BukkitItemUser) {
            BukkitItemUser user = (BukkitItemUser) event.user();
            SoundData.play(() -> VectorUtils.toBukkit(event.user().position()).toLocation(user.world()), last.get().tree().stat("remove_sound"));
        }
    }

    @Override
    protected void insert(ItemEvents.Click<BukkitItem> event, int amount, BukkitItem rawCursor, CalibreComponent<BukkitItem> cursor) {
        super.insert(event, amount, rawCursor, cursor);
        if (event.user() instanceof BukkitItemUser) {
            BukkitItemUser user = (BukkitItemUser) event.user();
            SoundData.play(() -> VectorUtils.toBukkit(event.user().position()).toLocation(user.world()), cursor.tree().stat("insert_sound"));
        }
    }

    @Override public PaperComponentContainerSystem copy() { return new PaperComponentContainerSystem(this); }
}
