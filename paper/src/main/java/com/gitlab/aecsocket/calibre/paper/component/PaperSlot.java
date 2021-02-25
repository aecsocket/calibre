package com.gitlab.aecsocket.calibre.paper.component;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.paper.proto.Tree;
import com.gitlab.aecsocket.calibre.core.util.ItemCreationException;
import com.gitlab.aecsocket.calibre.paper.util.ItemDescriptor;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.paper.gui.GUIVector;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;

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
        plugin = CalibrePlugin.instance();
    }

    public GUIVector offset() { return offset; }
    public PaperSlot offset(GUIVector offset) { this.offset = offset; return this; }

    public Tree.Component invalidComponent() { return invalidComponent; }
    public PaperSlot invalidComponent(Tree.Component invalidComponent) { this.invalidComponent = invalidComponent; return this; }

    public ItemStack createViewItem(Locale locale, String slotKey, CalibreComponent<BukkitItem> cursor) {
        ItemStack item;
        try {
            String itemNodePath = cursor == null
                    ? (required ? "required" : "normal")
                    : (isCompatible(cursor) ? "compatible" : "incompatible");
            ConfigurationNode itemNode = plugin.setting(n -> n, "slot_view", "icon", itemNodePath);
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
