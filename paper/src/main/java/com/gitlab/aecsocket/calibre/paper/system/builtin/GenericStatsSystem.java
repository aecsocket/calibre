package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.*;
import com.gitlab.aecsocket.calibre.core.system.builtin.SchedulerSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.world.slot.EquippableSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.paper.util.CalibreProtocol;
import com.gitlab.aecsocket.calibre.paper.util.ItemAnimation;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.core.world.user.CameraUser;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.core.world.user.MovementUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.loop.MinecraftSyncLoop;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.BooleanStat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ConfigSerializable
public final class GenericStatsSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "generic_stats";
    public static final int LISTENER_PRIORITY = 100000;
    public static final UUID ATTR_MOVE_SPEED = new UUID(3871, 4920);
    public static final UUID ATTR_ATTACK_DAMAGE = new UUID(3894, 1859);
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("zoom", NumberDescriptorStat.of(0.1))
            .init("move_speed", NumberDescriptorStat.of(1d))
            .init("attack_damage", NumberDescriptorStat.of(0d))
            .init("allow_sprint", new BooleanStat(true))
            .init("allow_jump", new BooleanStat(true))

            .init("switch_to_delay", NumberDescriptorStat.of(0L))
            .init("switch_to_sound", new SoundDataStat())
            .init("switch_to_animation", new ItemAnimation.Stat())

            .init("switch_to_after", NumberDescriptorStat.of(0L))
            .init("switch_to_end_sound", new SoundDataStat())
            .init("switch_to_end_animation", new ItemAnimation.Stat())

            .init("switch_from_sound", new SoundDataStat())
            .init("switch_from_animation", new ItemAnimation.Stat())

            .init("sprint_start_sound", new SoundDataStat())
            .init("sprint_start_animation", new ItemAnimation.Stat())

            .init("sprint_stop_sound", new SoundDataStat())
            .init("sprint_stop_animation", new ItemAnimation.Stat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;
    private transient SchedulerSystem scheduler;

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
    private GenericStatsSystem() {
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

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(SchedulerSystem.class);
    }

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

        scheduler = require(SchedulerSystem.class);
    }

    private void update(ItemUser user) {
        if (user instanceof CameraUser)
            ((CameraUser) user).zoom(tree().<NumberDescriptor.Double>stat("zoom").apply());
        if (user instanceof PlayerUser && tree().<Boolean>stat("allow_sprint"))
            CalibreProtocol.allowSprint(((PlayerUser) user).entity(), true);
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
            double moveSpeed = tree().<NumberDescriptor.Double>stat("move_speed").apply();
            if (moveSpeed != 1) {
                meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(ATTR_MOVE_SPEED, "generic.movement_speed",
                        moveSpeed - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            }
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(ATTR_ATTACK_DAMAGE, "generic.attack_damage",
                    tree().<NumberDescriptor.Double>stat("attack_damage").apply(), AttributeModifier.Operation.ADD_NUMBER));
        });
    }

    public <I extends Item> void onEvent(ItemEvents.UpdateItem<I> event) {
        update(event.user());
        ItemUser user = event.user();
        if (!(user instanceof PlayerUser) || plugin.playerData(((PlayerUser) user).entity()).animation() == null)
            return;
        ItemSlot<I> slot = event.slot();
        if (!(slot instanceof EquippableSlot) || !((EquippableSlot<I>) slot).equipped())
            return;

        plugin.itemManager().hide(((BukkitItem) event.item()).item(), true);
    }

    public <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        ItemUser user = event.user();
        if (user instanceof PlayerUser && !tree().<Boolean>stat("allow_sprint"))
            CalibreProtocol.allowSprint(((PlayerUser) user).entity(), false);
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
            update(user);
            String prefix = user instanceof MovementUser && ((MovementUser) user).sprinting() ? "sprint_start" : "switch_to";
            if (user instanceof BukkitItemUser)
                SoundData.play(((BukkitItemUser) user)::location, tree().stat(prefix + "_sound"));
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                    () -> ItemAnimation.start(user, event.slot(), tree().stat(prefix + "_animation")), 1);

            scheduler.delay(tree().<NumberDescriptor.Long>stat("switch_to_delay").apply());
            long after = tree().<NumberDescriptor.Long>stat("switch_to_after").apply();
            if (after > 0) {
                scheduler.schedule(this, after, (self, equip, ctx) -> {
                    if (user instanceof BukkitItemUser)
                        SoundData.play(((BukkitItemUser) user)::location, tree().stat("switch_to_end_sound"));
                    ItemAnimation.start(equip.user(), equip.slot(), tree().stat("switch_to_end_animation"));
                });
                update(event);
            }
        } else {
            if (user instanceof BukkitItemUser)
                SoundData.play(((BukkitItemUser) user)::location, tree().stat("switch_from_sound"));
            if (user instanceof PlayerUser)
                plugin.playerData(((PlayerUser) user).entity()).animation(null);
            ItemAnimation.start(user, event.slot(), tree().stat("switch_from_animation"));
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
