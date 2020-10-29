package me.aecsocket.calibre.defaults.system;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.item.util.slot.EquipmentItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.user.AnimatableItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.calibre.util.CalibreSoundData;
import me.aecsocket.calibre.util.stat.ItemAnimationStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemSystem extends BaseSystem {
    public static final String ID = "item";
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("fov_multiplier", new NumberStat.Double(0.1d))
            .init("move_speed_multiplier", new NumberStat.Double(1d))
            .init("armor", new NumberStat.Double(0d))

            .init("draw_delay", new NumberStat.Long(0L))
            .init("draw_sound", new SoundStat())
            .init("draw_animation", new ItemAnimationStat())
            .get();
    public static final UUID ARMOR_UUID = new UUID(69, 420);
    public static final UUID MOVE_SPEED_UUID = new UUID(420, 69);

    @LoadTimeOnly private String itemNameKey;
    @LoadTimeOnly private String descriptionKey;
    private long nextAvailable;

    public ItemSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    public String getItemNameKey() { return itemNameKey; }
    public void setItemNameKey(String itemNameKey) { this.itemNameKey = itemNameKey; }

    public String getDescriptionKey() { return descriptionKey; }
    public void setDescriptionKey(String descriptionKey) { this.descriptionKey = descriptionKey; }

    public long getNextAvailable() { return nextAvailable; }
    public void setNextAvailable(long nextAvailable) { this.nextAvailable = nextAvailable; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        parent.registerSystemService(ItemSystem.class, this);

        if (itemNameKey == null) itemNameKey = parent.getNameKey();
        if (descriptionKey == null) descriptionKey = parent.getNameKey() + ".description";

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemEvents.Create.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Update.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Draw.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Holster.class, this::onEvent, 0);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    public void updateFov(ItemUser user) {
        if (user instanceof PlayerItemUser)
            CalibreProtocol.fovMultiplier(((PlayerItemUser) user).getEntity(), stat("fov_multiplier"));
    }

    public void resetFov(ItemUser user) {
        if (user instanceof PlayerItemUser)
            CalibreProtocol.resetFov(((PlayerItemUser) user).getEntity());
    }

    private void onEvent(ItemEvents.Create event) {
        if (!parent.isRoot()) return;
        ItemMeta meta = event.getMeta();
        Player player = event.getPlayer();

        // Attributes
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(ARMOR_UUID, "generic.armor", stat("armor"), AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(MOVE_SPEED_UUID, "generic.movement_speed", (double) stat("move_speed_multiplier") - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Lore
        List<String> sections = new ArrayList<>();

        plugin.rgen(player, itemNameKey).ifPresent(meta::setDisplayName);
        plugin.rgen(player, descriptionKey).ifPresent(sections::add);

        callEvent(new Events.SectionCreate(
                event.getPlayer(),
                event.getAmount(),
                event.getItem(),
                meta,
                sections
        ));
        List<String> lore = Arrays.asList(String.join(plugin.gen(player, "system.item.section_separator"), sections).split("\n"));
        if (lore.size() > 0 && !lore.get(0).equals(""))
            meta.setLore(lore);
    }

    private void onEvent(ItemEvents.Update event) {
        updateFov(event.getUser());
    }

    private void onEvent(ItemEvents.Draw event) {
        updateFov(event.getUser());
        doAction(this, "draw", event.getUser(), event.getSlot());
    }

    private void onEvent(ItemEvents.Holster event) {
        resetFov(event.getUser());
    }

    public void doAction(CalibreSystem system, String actionName, ItemUser user, ItemSlot slot, Location location) {
        ComponentTree tree = system.getParent().getTree();
        if (location == null)
            location = user.getLocation();

        Long delay = tree.stat(actionName + "_delay");
        CalibreSoundData[] sound = tree.stat(actionName + "_sound");
        ParticleData[] particle = tree.stat(actionName + "_particle");
        ItemAnimation animation = tree.stat(actionName + "_animation");

        if (delay != null)
            applyDelay(delay);
        if (sound != null)
            SoundData.play(location, sound);
        if (particle != null)
            ParticleData.spawn(location, particle);
        if (animation != null && user instanceof AnimatableItemUser && slot instanceof EquipmentItemSlot)
            ((AnimatableItemUser) user).startAnimation(animation, ((EquipmentItemSlot) slot).getEquipmentSlot());
    }

    public void doAction(CalibreSystem system, String actionName, ItemUser user, ItemSlot slot) {
        doAction(system, actionName, user, slot, null);
    }

    public void applyDelay(long ms) { nextAvailable = System.currentTimeMillis() + ms; }
    public long getDelay() { return nextAvailable - System.currentTimeMillis(); }
    public boolean isAvailable() { return System.currentTimeMillis() >= nextAvailable; }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public ItemSystem clone() { return (ItemSystem) super.clone(); }
    @Override public ItemSystem copy() { return clone(); }

    public static final class Events {
        private Events() {}

        public static class SectionCreate {
            private final @Nullable Player player;
            private final int amount;
            private final ItemStack item;
            private final ItemMeta meta;
            private final List<String> sections;

            public SectionCreate(@Nullable Player player, int amount, ItemStack item, ItemMeta meta, List<String> sections) {
                this.player = player;
                this.amount = amount;
                this.item = item;
                this.meta = meta;
                this.sections = sections;
            }

            public Player getPlayer() { return player; }
            public int getAmount() { return amount; }
            public ItemStack getItem() { return item; }
            public ItemMeta getMeta() { return meta; }
            public List<String> getSections() { return sections; }
        }
    }
}
