package me.aecsocket.calibre.defaults;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ActionSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.calibre.util.itemuser.EntityItemUser;
import me.aecsocket.calibre.util.itemuser.PlayerItemUser;
import me.aecsocket.calibre.util.protocol.CalibreProtocol;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Allows modification of some item attributes and basic item actions.
 */
public class AttributeSystem implements CalibreSystem<Void> {
    public static final UUID MOVE_SPEED_UUID = new UUID(5318008, 69420);
    public static final UUID ARMOR_UUID = new UUID(69420, 5318008);
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("move_speed_multiplier", new NumberStat.Double(1d))
            .init("armor", new NumberStat.Double(0d))
            .init("fov_multiplier", new NumberStat.Double(0.1))

            .init("draw_delay", new NumberStat.Long(1L))
            .init("draw_sound", new DataStat.Sound())
            .init("draw_animation", new AnimationStat())
            .get();

    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private transient ActionSystem actionSystem;

    public AttributeSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public ActionSystem getActionSystem() { return actionSystem; }

    @Override public String getId() { return "attribute"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) {
        this.parent = parent;
        actionSystem = getSystem(ActionSystem.class);
    }

    @Override
    public @NotNull Collection<Class<?>> getDependencies() {
        return Arrays.asList(
                ActionSystem.class
        );
    }

    @Override
    public void acceptTree(ComponentTree tree) {
        EventDispatcher dispatcher = tree.getEventDispatcher();
        dispatcher.registerListener(ItemEvents.ItemCreation.class, this::onEvent, 0);
        dispatcher.registerListener(ItemEvents.ItemUpdate.class, this::onEvent, 0);
        dispatcher.registerListener(ItemEvents.Draw.class, this::onEvent, 0);
        dispatcher.registerListener(ItemEvents.Holster.class, this::onEvent, 0);
    }

    public void updateFOV(Player player) {
        CalibreProtocol.fovMultiplier(player, stat("fov_multiplier"));
    }

    public void onEvent(ItemEvents.ItemCreation event) {
        if (!isRoot()) return;
        ItemMeta meta = event.getMeta();
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(
                MOVE_SPEED_UUID,
                "generic.movement_speed",
                (double) stat("move_speed_multiplier") - 1,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                ARMOR_UUID,
                "generic.armor",
                stat("armor"),
                AttributeModifier.Operation.ADD_NUMBER
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    public void onEvent(ItemEvents.ItemUpdate event) {
        if (!isRoot()) return;
        if (event.getUser() instanceof PlayerItemUser)
            updateFOV(((PlayerItemUser) event.getUser()).getEntity());
    }

    public void onEvent(ItemEvents.Draw event) {
        if (!(event.getUser() instanceof PlayerItemUser)) return;
        PlayerItemUser user = (PlayerItemUser) event.getUser();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (user.getEntity().getInventory().getHeldItemSlot() == event.getItemSlot())
                actionSystem.startAction(
                        stat("draw_delay"),
                        event.getUser().getLocation(), stat("draw_sound"), null,
                        event.getUser(), event.getSlot(), stat("draw_animation"));
        }, 1);
        updateFOV(user.getEntity());
    }

    public void onEvent(ItemEvents.Holster event) {
        if (!(event.getUser() instanceof PlayerItemUser)) return;
        PlayerItemUser user = (PlayerItemUser) event.getUser();
        CalibreProtocol.resetFov(user.getEntity());
    }

    @Override public AttributeSystem clone() { try { return (AttributeSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public AttributeSystem copy() { return clone(); }
}
