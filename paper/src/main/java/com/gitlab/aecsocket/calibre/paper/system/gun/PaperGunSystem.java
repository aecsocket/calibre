package com.gitlab.aecsocket.calibre.paper.system.gun;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.paper.proto.system.SystemsGun;
import com.gitlab.aecsocket.calibre.paper.system.BukkitItemEvents;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.FireModePath;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.SightPath;
import com.gitlab.aecsocket.calibre.paper.util.ItemDescriptor;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.core.world.user.InaccuracyUser;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.EntityEquipmentSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.google.protobuf.Any;
import com.gitlab.aecsocket.calibre.paper.util.CalibreProtocol;
import com.gitlab.aecsocket.calibre.paper.util.ItemAnimation;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.loop.MinecraftSyncLoop;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.BooleanStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.StringStat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.ParticleDataStat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.ParticleData;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class PaperGunSystem extends GunSystem implements PaperSystem {
    public static final PotionEffect EFFECT_HASTE = new PotionEffect(PotionEffectType.FAST_DIGGING, 5, 127, false, false, false);
    public static final int ITEM_DESPAWN = 5 * 60 * 20;
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(GunSystem.DEFAULT_STATS)
            .init("aim_item", new ItemDescriptor.Stat())
            .init("entity_aware_radius", NumberDescriptorStat.of(0d))
            .init("inaccuracy_velocity", NumberDescriptorStat.of(0d))

            .init("casing_can_pick_up", new BooleanStat(false))
            .init("casing_lifetime", NumberDescriptorStat.of(0d))
            .init("casing_category", new StringStat())

            .init("fire_particle", new ParticleDataStat())
            .init("fire_sound", new SoundDataStat())

            .init("chamber_sound", new SoundDataStat())
            .init("chamber_animation", new ItemAnimation.Stat())
            .init("chamber_end_sound", new SoundDataStat())
            .init("chamber_end_animation", new ItemAnimation.Stat())

            .init("aim_in_sound", new SoundDataStat())
            .init("aim_in_animation", new ItemAnimation.Stat())

            .init("aim_out_sound", new SoundDataStat())
            .init("aim_out_animation", new ItemAnimation.Stat())

            .init("change_fire_mode_sound", new SoundDataStat())
            .init("change_fire_mode_animation", new ItemAnimation.Stat())

            .init("change_sight_sound", new SoundDataStat())
            .init("change_sight_sound_animation", new ItemAnimation.Stat())

            .init("fail_sound", new SoundDataStat())
            .init("fail_animation", new ItemAnimation.Stat())
            .get();
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;
    private int ignoreSwitch;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperGunSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperGunSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperGunSystem(PaperGunSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.UpdateItem.class, this::onEvent, listenerPriority);
        events.registerListener(BukkitItemEvents.ShowItem.class, this::onEvent, listenerPriority);
        events.registerListener(BukkitItemEvents.Swing.class, this::onEvent, listenerPriority);
    }

    protected <I extends Item> void onEvent(ItemEvents.UpdateItem<I> event) {
        if (event.cause() instanceof Events.StartFire && event.item() instanceof BukkitItem) {
            plugin.itemManager().hide(((BukkitItem) event.item()).item(), true);
        }
    }

    protected void onEvent(BukkitItemEvents.ShowItem event) {
        if (aiming)
            event.cancel();
    }

    protected void onEvent(BukkitItemEvents.Swing event) {
        event.cancel();
    }

    @Override
    public double calculateInaccuracy(InaccuracyUser user) {
        double result = super.calculateInaccuracy(user);
        if (user instanceof PlayerUser)
            // use #lengthSquared because #length is expensive
            result += plugin.velocityTracker().velocity(((PlayerUser) user).entity()).lengthSquared()
                    * MinecraftSyncLoop.TICKS_PER_SECOND
                    * tree().<NumberDescriptor.Double>stat("inaccuracy_velocity").apply();
        return result;
    }

    @Override
    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        super.onEvent(event);

        if (event.tickContext().loop() instanceof MinecraftSyncLoop) {
            if (!(event.user() instanceof LivingEntityUser))
                return;
            ((LivingEntityUser) event.user()).entity().addPotionEffect(EFFECT_HASTE);
        }
    }

    @Override
    protected <I extends Item> void onEvent(ItemEvents.Scroll<I> event) {
        if (ignoreSwitch >= Bukkit.getCurrentTick()) {
            ignoreSwitch = 0;
            update(event);
            return;
        }
        ignoreSwitch = Bukkit.getCurrentTick() + 1;
        super.onEvent(event);
    }

    @Override
    public Vector3D offset(ItemUser user, ItemSlot<?> slot, Vector3D offset) {
        if (slot instanceof EntityEquipmentSlot && ((EntityEquipmentSlot) slot).slot() == EquipmentSlot.OFF_HAND)
            offset = offset.x(-offset.x());
        if (user instanceof PlayerUser && ((PlayerUser) user).entity().getMainHand() == MainHand.LEFT)
            offset = offset.x(-offset.x());
        return super.offset(user, slot, offset);
    }

    @Override
    public <I extends Item> void ejectCasing(CalibreComponent<I> chamber, ItemUser user, Vector3D position, Vector3D velocity) {
        if (!(user instanceof BukkitItemUser))
            return;
        World world = ((BukkitItemUser) user).world();

        Item created = chamber.create(user.locale());
        if (!(created instanceof BukkitItem))
            return;
        org.bukkit.entity.Item entity = world.dropItem(
                VectorUtils.toBukkit(position).toLocation(world),
                ((BukkitItem) created).item()
        );
        entity.setVelocity(VectorUtils.toBukkit(velocity));
        entity.setTicksLived(ITEM_DESPAWN - (int) (tree().<NumberDescriptor.Double>stat("casing_lifetime").apply() * MinecraftSyncLoop.TICKS_PER_SECOND));
        entity.setCanMobPickup(false);
        if (!tree().<Boolean>stat("casing_can_pick_up"))
            entity.setPickupDelay(Integer.MAX_VALUE);

        plugin.casingManager().register(entity, tree().stat("casing_category"));
    }

    @Override
    public <I extends Item> void chamber(Events.Chamber<I> event, List<CalibreSlot> chamberSlots) {
        super.chamber(event, chamberSlots);
        if (event.result() != ItemEvents.Result.SUCCESS)
            return;
        if (!(event.user() instanceof BukkitItemUser))
            return;
        BukkitItemUser user = (BukkitItemUser) event.user();
        SoundData.play(user::location, tree().stat("chamber_sound"));
        ItemAnimation.start(user, event.slot(), tree().stat("chamber_animation"));
    }

    @Override
    protected <I extends Item> void endChamber(ItemEvents.Equipped<I> event) {
        super.endChamber(event);
        if (!(event.user() instanceof BukkitItemUser))
            return;
        BukkitItemUser user = (BukkitItemUser) event.user();
        SoundData.play(user::location, tree().stat("chamber_end_sound"));
        ItemAnimation.start(user, event.slot(), tree().stat("chamber_end_animation"));
    }

    @Override
    public <I extends Item> void fireSuccess(Events.FireSuccess<I> event) {
        ItemUser user = event.user();

        if (user instanceof BukkitItemUser) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("fire_sound"));
            ParticleData.spawn(VectorUtils.toBukkit(event.position()).toLocation(((BukkitItemUser) user).world()), tree().stat("fire_particle"));

            if (user instanceof PlayerUser) {
                Player player = ((PlayerUser) user).entity();
                if (player.getGameMode() == GameMode.CREATIVE)
                    return;
                Location location = player.getLocation();
                location.getNearbyLivingEntities(tree().<NumberDescriptor.Double>stat("entity_aware_radius").apply()).forEach(other -> {
                    if (other instanceof Monster) {
                        Monster monster = (Monster) other;
                        if (monster.getTarget() == null)
                            monster.setTarget(player);
                    }
                });
            }
        }
        super.fireSuccess(event);
    }

    @Override
    public <I extends Item> void aim(Events.Aim<I> event) {
        super.aim(event);
        if (event.result() == ItemEvents.Result.SUCCESS && event.user() instanceof BukkitItemUser) {
            BukkitItemUser user = (BukkitItemUser) event.user();
            String infix = event.aim() ? "in" : "out";
            SoundData.play(user::location, tree().stat("aim_" + infix + "_sound"));
            ItemAnimation.start(user, event.slot(), tree().stat("aim_" + infix + "_animation"));

            if (event.aim() && event.user() instanceof PlayerUser) {
                ItemDescriptor aimItem = tree().stat("aim_item");
                if (aimItem == null)
                    return;

                ItemStack aimItemStack = aimItem.create();
                CalibreProtocol.aimAnimation(((PlayerUser) event.user()).entity(), aimItemStack, EquipmentSlot.HAND /* todo */);
            }
        }
    }

    @Override
    public <I extends Item> void changeFireMode(Events.ChangeFireMode<I> event) {
        super.changeFireMode(event);
        if (event.result() == ItemEvents.Result.SUCCESS && event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, tree().stat("change_fire_mode_sound"));
        ItemAnimation.start(event.user(), event.slot(), tree().stat("change_fire_mode_animation"));
    }

    @Override
    public <I extends Item> void changeSight(Events.ChangeSight<I> event) {
        super.changeSight(event);
        if (event.result() == ItemEvents.Result.SUCCESS && event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, tree().stat("change_sight_sound"));
        ItemAnimation.start(event.user(), event.slot(), tree().stat("change_sight_animation"));
    }

    @Override
    public <I extends Item> void fail(Events.Fail<I> event) {
        super.fail(event);
        if (!(event.user() instanceof BukkitItemUser))
            return;
        BukkitItemUser user = (BukkitItemUser) event.user();
        SoundData.play(user::location, tree().stat("fail_sound"));
        ItemAnimation.start(user, event.slot(), tree().stat("fail_animation"));
    }

    @Override public PaperGunSystem partialCopy() { return new PaperGunSystem(this); }

    @Override
    public PaperGunSystem copy() {
        PaperGunSystem sys = (PaperGunSystem) super.copy();
        sys.ignoreSwitch = ignoreSwitch;
        return sys;
    }

    @Override
    public Any writeProtobuf() {
        var builder = SystemsGun.GunSystem.newBuilder()
                .setAiming(aiming)
                .setIgnoreSwitch(ignoreSwitch);
        if (fireMode != null)
                builder.setFireMode(SystemsGun.Path.newBuilder()
                        .addAllPath(Arrays.asList(fireMode.path()))
                        .setIndex(fireMode.index()).build());
        if (sight != null)
                builder.setSight(SystemsGun.Path.newBuilder()
                        .addAllPath(Arrays.asList(sight.path()))
                        .setIndex(sight.index()).build());
        return Any.pack(builder.build());
    }

    @Override
    public PaperGunSystem readProtobuf(Any raw) {
        SystemsGun.GunSystem msg = unpack(raw, SystemsGun.GunSystem.class);
        PaperGunSystem sys = new PaperGunSystem(this);
        sys.aiming = msg.getAiming();
        sys.ignoreSwitch = msg.getIgnoreSwitch();
        if (msg.hasFireMode())
            sys.fireMode = new FireModePath(msg.getFireMode().getPathList().toArray(new String[0]), msg.getFireMode().getIndex());
        if (msg.hasSight())
            sys.sight = new SightPath(msg.getSight().getPathList().toArray(new String[0]), msg.getSight().getIndex());
        return sys;
    }
}
