package me.aecsocket.calibre.hook;

import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.ActionSystem;
import me.aecsocket.calibre.defaults.DefaultPacketAdapter;
import me.aecsocket.calibre.defaults.gui.SlotViewGUI;
import me.aecsocket.calibre.defaults.melee.MeleeSystem;
import me.aecsocket.calibre.defaults.DefaultEventHandle;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.gui.GUIManager;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.util.json.JsonAdapters;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The default {@link CalibreHook}. This provides system types for the default objects like guns.
 */
public class CalibreDefaultHook implements CalibreHook {
    private CalibrePlugin plugin;
    private GUIManager guiManager;
    private DefaultPacketAdapter packetAdapter;

    public CalibrePlugin getPlugin() { return plugin; }
    @Override public void acceptPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public GUIManager getGUIManager() { return guiManager; }
    public DefaultPacketAdapter getPacketAdapter() { return packetAdapter; }

    @Override
    public void initialize() {
        guiManager = new GUIManager(plugin);
        packetAdapter = new DefaultPacketAdapter(plugin);

        Bukkit.getPluginManager().registerEvents(new DefaultEventHandle(plugin, this), plugin);
        plugin.getProtocolManager().addPacketListener(packetAdapter);
    }

    @Override
    public void initializeGson(GsonBuilder builder) {
        builder
                .registerTypeAdapter(GUIVector.class, JsonAdapters.GUI_VECTOR);
    }

    @Override
    public void preLoadRegister(Registry registry, LocaleManager localeManager, Settings settings) {
        registry.register(new ActionSystem(plugin));
        registry.register(new MeleeSystem(plugin));
    }

    public void updateSlotView(Player player, CalibreComponent newComponent) {
        GUIView view = guiManager.getView(player.getOpenInventory());
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            SlotViewGUI gui = (SlotViewGUI) view.getGUI();
            gui.setComponent(newComponent);
            gui.notifyModification(view);
        }
    }
}
