package me.aecsocket.calibre.defaults;

import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.CalibreHook;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.gui.SlotViewGUI;
import me.aecsocket.calibre.defaults.gun.*;
import me.aecsocket.calibre.defaults.gun.sight.SightableSystem;
import me.aecsocket.calibre.defaults.gun.sight.SimpleSightSystem;
import me.aecsocket.calibre.defaults.gun.sight.SimpleSightSystemAdapter;
import me.aecsocket.calibre.defaults.melee.MeleeSystem;
import me.aecsocket.calibre.defaults.service.bukkit.damage.CalibreDamageService;
import me.aecsocket.calibre.defaults.service.bukkit.damage.CalibreDamageProvider;
import me.aecsocket.calibre.defaults.service.bukkit.raytrace.CalibreRaytraceProvider;
import me.aecsocket.calibre.defaults.service.bukkit.raytrace.CalibreRaytraceService;
import me.aecsocket.calibre.defaults.service.bukkit.spread.CalibreSpreadProvider;
import me.aecsocket.calibre.defaults.service.bukkit.spread.CalibreSpreadService;
import me.aecsocket.calibre.defaults.service.system.SimpleActionSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.util.componentlist.CalibreComponentList;
import me.aecsocket.calibre.util.componentlist.CalibreComponentListAdapter;
import me.aecsocket.unifiedframework.gui.GUIManager;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.util.Vector2;
import me.aecsocket.unifiedframework.util.json.JsonAdapters;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * The default {@link CalibreHook}. This provides system types for the default objects like guns.
 */
public class CalibreDefaultHook implements CalibreHook {
    private CalibrePlugin plugin;
    private GUIManager guiManager;
    private DefaultPacketAdapter packetAdapter;
    private final Map<Object, Object> inbuiltProviders = new HashMap<>();

    public CalibrePlugin getPlugin() { return plugin; }
    @Override public void acceptPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public GUIManager getGUIManager() { return guiManager; }
    public DefaultPacketAdapter getPacketAdapter() { return packetAdapter; }
    public Map<Object, Object> getInbuiltProviders() { return inbuiltProviders; }

    @Override
    public void initialize() {
        guiManager = new GUIManager(plugin);
        packetAdapter = new DefaultPacketAdapter(plugin);

        Bukkit.getPluginManager().registerEvents(new DefaultEventHandle(plugin, this), plugin);
        plugin.getProtocolManager().addPacketListener(packetAdapter);

        registerInbuiltService(CalibreDamageService.class, new CalibreDamageProvider(plugin));
        registerInbuiltService(CalibreRaytraceService.class, new CalibreRaytraceProvider());
        registerInbuiltService(CalibreSpreadService.class, new CalibreSpreadProvider());
    }

    @Override
    public void initializeGson(GsonBuilder builder) {
        builder
                .registerTypeAdapterFactory(new SimpleSightSystemAdapter(plugin))
                .registerTypeAdapter(Vector.class, JsonAdapters.VECTOR)
                .registerTypeAdapter(GUIVector.class, JsonAdapters.GUI_VECTOR)
                .registerTypeAdapter(Vector2.class, JsonAdapters.VECTOR2)
                .registerTypeAdapter(CalibreComponentList.class, new CalibreComponentListAdapter(plugin.getRegistry()));
    }

    @Override
    public void preLoadRegister(Registry registry, LocaleManager localeManager, Settings settings) {
        registry.register(new SimpleActionSystem(plugin));
        registry.register(new AttributeSystem(plugin));

        registry.register(new MeleeSystem(plugin));

        registry.register(new FireableSystem(plugin));
        registry.register(new SightableSystem(plugin));
        registry.register(new SimpleSightSystem(plugin));
        registry.register(new BulletSystem(plugin));
        registry.register(new AmmoContainerSystem(plugin));
    }

    public void updateSlotView(Player player, CalibreComponent newComponent) {
        GUIView view = guiManager.getView(player.getOpenInventory());
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            SlotViewGUI gui = (SlotViewGUI) view.getGUI();
            gui.setComponent(newComponent);
            gui.notifyModification(view);
        }
    }

    private <T> void registerInbuiltService(Class<T> serviceType, T provider) {
        inbuiltProviders.put(serviceType, provider);
        Bukkit.getServicesManager().register(serviceType, provider, plugin, ServicePriority.Lowest);
    }
}
