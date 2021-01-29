package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.calibre.wrapper.user.LivingEntityUser;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.data.ParticleDataStat;
import me.aecsocket.unifiedframework.stat.impl.data.SoundDataStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import me.aecsocket.unifiedframework.util.data.Tuple2;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class PaperGunSystem extends GunSystem implements PaperSystem {
    public static final PotionEffect EFFECT_HASTE = new PotionEffect(PotionEffectType.FAST_DIGGING, 5, 127, false, false, false);
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(GunSystem.DEFAULT_STATS)
            .init("entity_aware_radius", NumberDescriptorStat.of(0d))

            .init("fire_particle", new ParticleDataStat())
            .init("fire_sound", new SoundDataStat())

            .init("chamber_sound", new SoundDataStat())
            .init("change_fire_mode_sound", new SoundDataStat())
            .init("aim_in_sound", new SoundDataStat())
            .init("aim_out_sound", new SoundDataStat())
            .init("change_sight_sound", new SoundDataStat())
            .init("fail_sound", new SoundDataStat())
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

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(ItemEvents.UpdateItem.class, this::onEvent, 0);
    }

    protected <I extends Item> void onEvent(ItemEvents.UpdateItem<I> event) {
        if (event.cause() instanceof Events.StartFire && event.item() instanceof BukkitItem)
            plugin.itemManager().hide(((BukkitItem) event.item()).item(), true);
    }

    @Override
    protected <I extends Item> void onEvent(ItemEvents.Equipped<I> event) {
        super.onEvent(event);

        if (!(event.user() instanceof LivingEntityUser))
            return;
        ((LivingEntityUser) event.user()).entity().addPotionEffect(EFFECT_HASTE);
    }

    @Override
    protected Tuple2<Vector3D, Vector3D> getBarrelOffset(ItemUser user, Vector3D offset) {
        if (user instanceof PlayerUser && ((PlayerUser) user).entity().getMainHand() == MainHand.LEFT)
            offset = offset.x(-offset.x());
        Tuple2<Vector3D, Vector3D> result = super.getBarrelOffset(user, offset);
        return result;
    }

    @Override
    public <I extends Item> void fire(Events.Fire<I> event) {
        super.fire(event);
        if (event.projectileSystem() == null)
            return;

        ItemUser user = event.user();
        if (user instanceof BukkitItemUser) {
            SoundData.play(((BukkitItemUser) user)::location, tree().stat("fire_sound"));

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
    }

    @Override
    protected <I extends Item> void fireSuccess(Events.FireSuccess<I> event) {
        super.fireSuccess(event);
        ItemUser user = event.user();
        if (user instanceof BukkitItemUser)
            ParticleData.spawn(VectorUtils.toBukkit(event.position()).toLocation(((BukkitItemUser) user).world()), tree().stat("fire_particle"));
    }

    @Override
    protected <I extends Item> void chamber(Events.Chamber<I> event, List<CalibreSlot> chamberSlots) {
        super.chamber(event, chamberSlots);
        if (!(event.user() instanceof BukkitItemUser))
            return;
        BukkitItemUser user = (BukkitItemUser) event.user();
        if (event.result() == ItemEvents.Result.SUCCESS) {
            SoundData.play(user::location, tree().stat("chamber_sound"));
        } else {
            SoundData.play(user::location, tree().stat("fail_sound"));
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
    protected void test(ItemUser user, double fov) {
        CalibreProtocol.fov(((PlayerUser) user).entity(), fov);
    }

    @Override public PaperGunSystem copy() { return new PaperGunSystem(this); }
}
