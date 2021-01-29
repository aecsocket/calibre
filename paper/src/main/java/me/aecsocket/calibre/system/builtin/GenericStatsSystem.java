package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.*;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.world.ZoomableUser;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.BooleanStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ConfigSerializable
public class GenericStatsSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "generic_stats";
    public static final UUID ATTR_MOVE_SPEED = new UUID(3871, 4920);
    public static final UUID ATTR_ATTACK_DAMAGE = new UUID(3894, 1859);
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("zoom", NumberDescriptorStat.of(0.1))
            .init("move_speed", NumberDescriptorStat.of(1d))
            .init("attack_damage", NumberDescriptorStat.of(0d))
            .init("allow_sprint", new BooleanStat(true))
            .init("allow_jump", new BooleanStat(true))
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public GenericStatsSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public GenericStatsSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public GenericStatsSystem(GenericStatsSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        int priority = setting("listener_priority").getInt(100000);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);
        events.registerListener(ItemEvents.UpdateItem.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Jump.class, this::onEvent, priority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, priority);
    }

    private void update(ItemUser user) {
        if (user instanceof ZoomableUser)
            ((ZoomableUser) user).zoom(tree().<NumberDescriptor.Double>stat("zoom").apply());
        if (user instanceof PlayerUser)
            CalibreProtocol.allowSprint(((PlayerUser) user).entity(), tree().<Boolean>stat("allow_sprint"));
    }

    private void reset(ItemUser user) {
        if (user instanceof ZoomableUser)
            ((ZoomableUser) user).zoom(0.1 /* todo update to defaults */);
        if (user instanceof PlayerUser)
            CalibreProtocol.allowSprint(((PlayerUser) user).entity(), true);
    }

    public <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        if (!(event.item() instanceof BukkitItem))
            return;
        BukkitUtils.modMeta(((BukkitItem) event.item()).item(), meta -> {
            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(ATTR_MOVE_SPEED, "generic.movement_speed",
                    tree().<NumberDescriptor.Double>stat("move_speed").apply() - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(ATTR_ATTACK_DAMAGE, "generic.attack_damage",
                    tree().<NumberDescriptor.Double>stat("attack_damage").apply(), AttributeModifier.Operation.ADD_NUMBER));
        });
    }

    public <I extends Item> void onEvent(ItemEvents.UpdateItem<I> event) {
        update(event.user());
    }

    public <I extends Item> void onEvent(ItemEvents.Jump<I> event) {
        if (!tree().<Boolean>stat("allow_jump"))
            event.cancel();
    }

    public <I extends Item> void onEvent(ItemEvents.Switch<I> event) {
        if (event.cancelled())
            return;
        if (event.position() == ItemEvents.Switch.TO)
            update(event.user());
        else
            reset(event.user());
    }

    @Override public GenericStatsSystem copy() { return new GenericStatsSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
