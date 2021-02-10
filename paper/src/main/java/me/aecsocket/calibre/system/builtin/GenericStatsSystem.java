package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.util.ItemAnimation;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.slot.EquippableSlot;
import me.aecsocket.calibre.world.user.CameraUser;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.world.user.MovementUser;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.MinecraftSyncLoop;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.BooleanStat;
import me.aecsocket.unifiedframework.stat.impl.data.SoundDataStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.data.SoundData;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ConfigSerializable
public class GenericStatsSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "generic_stats";
    public static final int LISTENER_PRIORITY = 100000;
    public static final UUID ATTR_MOVE_SPEED = new UUID(3871, 4920);
    public static final UUID ATTR_ATTACK_DAMAGE = new UUID(3894, 1859);
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("zoom", NumberDescriptorStat.of(0.1))
            .init("move_speed", NumberDescriptorStat.of(1d))
            .init("attack_damage", NumberDescriptorStat.of(0d))
            .init("allow_sprint", new BooleanStat(true))
            .init("allow_jump", new BooleanStat(true))

            .init("switch_to_sound", new SoundDataStat())
            .init("switch_to_animation", new ItemAnimation.Stat())

            .init("sprint_start_sound", new SoundDataStat())
            .init("sprint_start_animation", new ItemAnimation.Stat())

            .init("sprint_stop_sound", new SoundDataStat())
            .init("sprint_stop_animation", new ItemAnimation.Stat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public GenericStatsSystem(CalibrePlugin plugin) {
        super(LISTENER_PRIORITY);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public GenericStatsSystem() {
        super(LISTENER_PRIORITY);
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

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.UpdateItem.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Equipped.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Jump.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.ToggleSprint.class, this::onEvent, listenerPriority);
        events.registerListener(ItemEvents.Switch.class, this::onEvent, listenerPriority);
    }

    private void update(ItemUser user) {
        if (user instanceof CameraUser)
            ((CameraUser) user).zoom(tree().<NumberDescriptor.Double>stat("zoom").apply());
        if (user instanceof PlayerUser)
            CalibreProtocol.allowSprint(((PlayerUser) user).entity(), tree().<Boolean>stat("allow_sprint"));
    }

    private void reset(ItemUser user) {
        if (user instanceof CameraUser)
            ((CameraUser) user).zoom(0.1);
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
        if (
                event.slot() instanceof EquippableSlot && ((EquippableSlot<I>) event.slot()).equipped()
                && event.user() instanceof PlayerUser && plugin.playerData(((PlayerUser) event.user()).entity()).animation() != null
        ) {
            plugin.itemManager().hide(((BukkitItem) event.item()).item(), true);
        }
    }

    public <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        if (event.tickContext().loop() instanceof MinecraftSyncLoop)
            update(event.user());
    }

    public <I extends Item> void onEvent(ItemEvents.Jump<I> event) {
        if (!tree().<Boolean>stat("allow_jump"))
            event.cancel();
    }

    public <I extends Item> void onEvent(ItemEvents.ToggleSprint<I> event) {
        String infix = event.sprinting() ? "start" : "stop";
        ItemUser user = event.user();
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("sprint_" + infix + "_sound"));
        ItemAnimation.start(user, event.slot(), tree().stat("sprint_" + infix + "_animation"));
    }

    public <I extends Item> void onEvent(ItemEvents.Switch<I> event) {
        if (event.cancelled())
            return;
        ItemUser user = event.user();
        if (event.position() == ItemEvents.Switch.TO) {
            update(event);
            String prefix = user instanceof MovementUser && ((MovementUser) user).sprinting() ? "sprint_start" : "switch_to";
            if (user instanceof BukkitItemUser)
                SoundData.play(((BukkitItemUser) user)::location, tree().stat(prefix + "_sound"));
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                    () -> ItemAnimation.start(user, event.slot(), tree().stat(prefix + "_animation")), 1);
        } else {
            if (user instanceof PlayerUser)
                plugin.playerData(((PlayerUser) user).entity()).animation(null);
            reset(user);
        }
    }

    @Override public GenericStatsSystem copy() { return new GenericStatsSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
