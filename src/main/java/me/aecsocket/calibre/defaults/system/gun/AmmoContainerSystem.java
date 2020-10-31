package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.CalibreComponentSupplier;
import me.aecsocket.calibre.defaults.system.ComponentStorageSystem;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AmmoContainerSystem extends ComponentStorageSystem implements AmmoStorageSystem {
    public static final String ID = "ammo_container";

    @LoadTimeOnly private int capacity;
    @LoadTimeOnly private String icon;
    @LoadTimeOnly private String emptyIcon;
    private transient ItemSystem itemSystem;

    public AmmoContainerSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    @Override public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override public String getEmptyIcon() { return emptyIcon; }
    public void setEmptyIcon(String emptyIcon) { this.emptyIcon = emptyIcon; }

    @Override public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        itemSystem = parent.getSystemService(ItemSystem.class);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemSystem.Events.SectionCreate.class, this::onEvent, 1);

        icon = icon == null ? "" : TextUtils.translateColor(icon);
        emptyIcon = emptyIcon == null ? "" : TextUtils.translateColor(emptyIcon);
    }

    private void onEvent(ItemSystem.Events.SectionCreate event) {
        if (!parent.isRoot()) return;
        Player player = event.getPlayer();
        List<String> sections = event.getSections();
        String lastSection = sections.get(sections.size() - 1);
        sections.set(sections.size() - 1, plugin.gen(player, "system.ammo_container.capacity",
                "lines", lastSection,
                "size", size(),
                "capacity", capacity)
        );
    }

    @Override
    public void reload(CalibreComponentSlot slot, GunSystem.Events.PreReload event) {
        slot.set(null);
        if (event.getUser() instanceof PlayerItemUser) {
            Player player = ((PlayerItemUser) event.getUser()).getEntity();
            Utils.giveItem(player, parent.withSimpleTree().createItem(player));
        }

        Utils.useService(CalibreComponentSupplier.Service.class, s ->
                slot.set(s.supply(slot, event.getUser(), this, true)));

        itemSystem.doAction(this, "reload", event.getUser(), event.getSlot());
        event.updateItem();
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public AmmoContainerSystem clone() { return (AmmoContainerSystem) super.clone(); }
    @Override public AmmoContainerSystem copy() { return (AmmoContainerSystem) super.copy(); }
}
