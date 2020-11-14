package me.aecsocket.calibre.defaults.system.gun.ammo;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.gun.GunSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.util.stat.ItemAnimationStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.util.MapInit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AmmoContainerSystem extends AbstractAmmoStorageSystem {
    private static final String ID = "ammo_container";
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("reload_delay", new NumberStat.Long(0L))
            .init("reload_sound", new SoundStat())
            .init("reload_animation", new ItemAnimationStat())
            .init("reload_out_after", new NumberStat.Long(0L))
            .init("reload_in_after", new NumberStat.Long(0L))
            .get();

    @LoadTimeOnly private boolean usable;
    private transient ItemSystem itemSystem;

    public AmmoContainerSystem(CalibrePlugin plugin) {
        super(plugin);
    }
    public AmmoContainerSystem() { this(null); }

    @Override public String getId() { return ID; }

    public boolean isUsable() { return usable; }
    public void setUsable(boolean usable) { this.usable = usable; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
        itemSystem = parent.getService(ItemSystem.class);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }
    @Override public Collection<String> getDependencies() { return Collections.singleton(ItemSystem.ID); }

    @Override
    public Collection<Class<? extends CalibreSystem>> getServiceTypes() {
        return usable ? super.getServiceTypes() : Collections.emptySet();
    }

    @Override
    public void reload(CalibreComponentSlot slot, GunSystem.Events.PreReload event) {

        /*slot.set(null);
        if (event.getUser() instanceof PlayerItemUser) {
            Player player = ((PlayerItemUser) event.getUser()).getEntity();
            Utils.giveItem(player, parent.withSimpleTree().createItem(player));
        }

        Utils.useService(CalibreComponentSupplier.class, s ->
                slot.set(s.supply(slot, event.getUser(), this, true)));

        itemSystem.doAction(this, "reload", event.getUser(), event.getSlot());
        event.updateItem();*/
    }
}
