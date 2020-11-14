package me.aecsocket.calibre.defaults.system.gun.ammo;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ComponentStorageSystem;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AbstractAmmoStorageSystem extends ComponentStorageSystem implements AmmoStorageSystem {
    @LoadTimeOnly private int capacity;
    @LoadTimeOnly private String icon;
    @LoadTimeOnly private String emptyIcon;

    public AbstractAmmoStorageSystem(CalibrePlugin plugin) {
        super(plugin);
    }
    public AbstractAmmoStorageSystem() { this(null); }

    @Override public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override public String getEmptyIcon() { return emptyIcon; }
    public void setEmptyIcon(String emptyIcon) { this.emptyIcon = emptyIcon; }

    @Override public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

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

    @Override public AbstractAmmoStorageSystem clone() { return (AbstractAmmoStorageSystem) super.clone(); }
    @Override public AbstractAmmoStorageSystem copy() { return (AbstractAmmoStorageSystem) super.copy(); }
}
