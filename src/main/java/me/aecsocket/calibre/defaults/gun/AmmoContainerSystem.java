package me.aecsocket.calibre.defaults.gun;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.util.componentlist.CalibreComponentList;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * Simple implementation of {@link AmmoProviderSystem}.
 */
public class AmmoContainerSystem implements CalibreSystem<AmmoContainerSystem>,
        AmmoProviderSystem,
        ItemEvents.ItemCreation.Listener {
    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private CalibreComponentList ammo;

    public AmmoContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public LinkedList<CalibreComponent> getAmmo() { return ammo; }
    public void setAmmo(LinkedList<CalibreComponent> ammo) { this.ammo = new CalibreComponentList(ammo); }

    @Override public String getId() { return "ammo_container"; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) { this.parent = parent; }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.ItemCreation.class, this, 0);
    }

    private void addEntry(StringJoiner lore, Player player, CalibreComponent component, int amount) {
        if (component == null) return;
        lore.add(plugin.gen(player, "ammo_container.lore",
                "name", component.getLocalizedName(player),
                "amount", amount));
    }

    @Override
    public void onEvent(ItemEvents.ItemCreation<?> event) {
        /*
        TODO: this groups components improperly.
        to check if component A should be grouped with B, 2 component descriptors are made
        if they are equal, they are grouped.
        the issue is, the descriptors contain a Map<String, Object>, and that Object does not necessarily
        have a sane #equals method. so, we have to find another way of grouping -_-
         */
        if (!parent.isRoot()) return;
        Player player = event.getPlayer();
        StringJoiner lore = new StringJoiner(plugin.gen(player, "ammo_container.lore.separator"));
        CalibreComponent component = null;
        ComponentDescriptor descriptor = null;
        int amount = 1;
        for (CalibreComponent cComponent : ammo) {
            ComponentDescriptor cDescriptor = ComponentDescriptor.of(cComponent);
            if (cDescriptor.equals(descriptor))
                ++amount;
            else {
                addEntry(lore, player, component, amount);
                component = cComponent;
                descriptor = cDescriptor;
                amount = 1;
            }
        }
        addEntry(lore, player, component, amount);
        event.getSections().add(lore.toString());
    }

    @Override public @Nullable CalibreComponent pop() { return hasNext() ? ammo.removeLast() : null; }
    @Override public @Nullable CalibreComponent peek() { return hasNext() ? ammo.peekLast() : null; }

    @Override public void add(CalibreComponent chamber) { ammo.addLast(chamber); }

    @Override public TypeToken<AmmoContainerSystem> getDescriptorType() { return new TypeToken<>(){}; }

    @Override
    public void acceptDescriptor(AmmoContainerSystem descriptor) {
        ammo = descriptor.ammo;
    }

    @Override public AmmoContainerSystem createDescriptor() { return this; }

    public AmmoContainerSystem clone() { try { return (AmmoContainerSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public AmmoContainerSystem copy() { return clone(); }
}
