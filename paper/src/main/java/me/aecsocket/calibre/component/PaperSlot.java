package me.aecsocket.calibre.component;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.proto.Tree;
import me.aecsocket.calibre.util.ItemCreationException;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

@ConfigSerializable
public class PaperSlot extends CalibreSlot {
    private transient final CalibrePlugin plugin;
    private transient Tree.Component invalidComponent;
    private GUIVector offset;

    public PaperSlot(boolean required, boolean fieldModifiable, CalibrePlugin plugin) {
        super(required, fieldModifiable);
        this.plugin = plugin;
    }

    public PaperSlot(boolean required, CalibrePlugin plugin) {
        super(required);
        this.plugin = plugin;
    }

    public PaperSlot(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperSlot(CalibreSlot o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperSlot() {
        plugin = CalibrePlugin.getInstance();
    }

    public GUIVector offset() { return offset; }
    public PaperSlot offset(GUIVector offset) { this.offset = offset; return this; }

    public Tree.Component invalidComponent() { return invalidComponent; }
    public PaperSlot invalidComponent(Tree.Component invalidComponent) { this.invalidComponent = invalidComponent; return this; }

    public ItemStack createViewItem(String locale, String slotKey, CalibreComponent<BukkitItem> cursor) {
        ItemStack item;
        try {
            String itemNodePath = cursor == null
                    ? (required ? "required" : "normal")
                    : (isCompatible(cursor) ? "compatible" : "incompatible");
            ConfigurationNode itemNode = plugin.setting("slot_view", "icon", itemNodePath);
            if (itemNode.virtual())
                throw new ItemCreationException("No slot item provided for [" + itemNodePath + "]");
            item = itemNode.get(ItemDescriptor.class).create();
        } catch (SerializationException e) {
            throw new ItemCreationException(e);
        }

        return BukkitUtils.modMeta(item, meta -> {
            // TODO wait for Paper to update
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(plugin.gen(locale, "slot_view.slot",
                    "key", plugin.gen(locale, "slot." + slotKey))));
        });
    }

    @Override
    public CalibreSlot copy() {
        return new PaperSlot(super.copy(), plugin).offset(offset);
    }
}
