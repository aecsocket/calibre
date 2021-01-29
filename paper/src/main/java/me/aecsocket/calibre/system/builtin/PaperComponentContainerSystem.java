package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperComponentContainerSystem extends ComponentContainerSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperComponentContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperComponentContainerSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperComponentContainerSystem(PaperComponentContainerSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override
    protected <I extends Item> void remove(ItemEvents.Click<I> event, Quantifier<CalibreComponent<I>> last) {
        super.remove(event, last);
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, last.get().tree().stat("remove_sound"));
    }

    @Override
    protected <I extends Item> void insert(ItemEvents.Click<I> event, int amount, I rawCursor, CalibreComponent<I> cursor) {
        super.insert(event, amount, rawCursor, cursor);
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, cursor.tree().stat("insert_sound"));
    }

    @Override public PaperComponentContainerSystem copy() { return new PaperComponentContainerSystem(this); }
}
