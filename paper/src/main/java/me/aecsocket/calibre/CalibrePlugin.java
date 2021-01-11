package me.aecsocket.calibre;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.system.BukkitItemEvents;
import me.aecsocket.calibre.system.builtin.*;
import me.aecsocket.calibre.system.gun.*;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.CalibreRegistry;
import me.aecsocket.calibre.util.StatCollection;
import me.aecsocket.unifiedframework.gui.GUIManager;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.locale.LocaleLoader;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.locale.Translation;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.Ref;
import me.aecsocket.unifiedframework.registry.loader.ConfigurateRegistryLoader;
import me.aecsocket.unifiedframework.resource.result.LoggingResult;
import me.aecsocket.unifiedframework.serialization.configurate.*;
import me.aecsocket.unifiedframework.serialization.configurate.descriptor.DoubleDescriptorSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.descriptor.Vector2DDescriptorSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector2DSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector2ISerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector3DSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector3ISerializer;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.impl.descriptor.DoubleDescriptorStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.Vector2DDescriptorStat;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.descriptor.DoubleDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.NumericalDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector2DDescriptor;
import me.aecsocket.unifiedframework.util.log.LabelledLogger;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector2I;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import me.aecsocket.unifiedframework.util.vector.Vector3I;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core plugin class for Paper servers.
 */
public class CalibrePlugin extends JavaPlugin implements Tickable {
    public static final String SETTINGS_FILE = "settings.conf";
    public static final List<String> SAVED_RESOURCES = Arrays.asList(
            "README.txt",
            SETTINGS_FILE,
            "lang/default_en_us.conf"
    );

    private static CalibrePlugin instance;
    public static CalibrePlugin getInstance() { return instance; }

    private final List<CalibreHook> hooks = new ArrayList<>();
    private final LabelledLogger logger = new LabelledLogger(getLogger());
    private final LocaleManager localeManager = new LocaleManager();
    private final CalibreRegistry registry = new CalibreRegistry();
    private final Map<String, NamespacedKey> keys = new HashMap<>();
    private final ObjectMapper.Factory mapperFactory = ObjectMapper.factoryBuilder()
            .build();
    private ConfigurationOptions configOptions;
    private PaperCommandManager commandManager;
    private GUIManager guiManager;
    private BukkitAudiences audiences;
    private ProtocolManager protocol;
    private MapFont font;
    private ConfigurationNode settings;
    private StatMapSerializer statMapSerializer;
    private SchedulerLoop schedulerLoop;

    @Override
    public void onEnable() {
        instance = this;

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin instanceof CalibreHook)
                hooks.add((CalibreHook) plugin);
        }

        hooks.forEach(hook -> hook.onEnable(this));

        configOptions = ConfigurationOptions.defaults()
                .serializers(builder -> {
                    hooks.forEach(hook -> hook.registerSerializers(builder));
                    statMapSerializer = new StatMapSerializer(Utils.serializer(StatMap.class, builder), key -> setting("stat_config", key));
                    builder
                            .register(Color.class, ColorSerializer.INSTANCE)
                            .register(ItemStack.class, ItemStackSerializer.INSTANCE)
                            .register(Location.class, LocationSerializer.INSTANCE)
                            .register(NamespacedKey.class, NamespacedKeySerializer.INSTANCE)
                            .register(DoubleDescriptor.class, DoubleDescriptorSerializer.INSTANCE)
                            .register(Vector2DDescriptor.class, Vector2DDescriptorSerializer.INSTANCE)
                            .register(Vector.class, VectorSerializer.INSTANCE)
                            .register(GUIVector.class, GUIVectorSerializer.INSTANCE)
                            .register(Vector3D.class, Vector3DSerializer.INSTANCE)
                            .register(Vector3I.class, Vector3ISerializer.INSTANCE)
                            .register(Vector2D.class, Vector2DSerializer.INSTANCE)
                            .register(Vector2I.class, Vector2ISerializer.INSTANCE)
                            .register(ParticleData.class, ParticleDataSerializer.INSTANCE)
                            .register(StatMap.class, statMapSerializer)
                            .register(StatCollection.class, StatCollection.Serializer.INSTANCE)

                            // DO NOT TOUCH THIS LINE
                            // java compiler will get pissy at you if you do
                            .register(new TypeToken<ComponentContainerSystem<?>>(){}, new ComponentContainerSystem.Serializer(builder.build().get(new TypeToken<ComponentContainerSystem<?>>(){})))
                            .register(new TypeToken<Quantifier<ComponentTree>>(){}, new QuantifierSerializer<>())
                            .register(SightRef.class, SightRef.Serializer.INSTANCE)
                            .register(FireModeRef.class, FireModeRef.Serializer.INSTANCE)
                            .register(PaperComponent.class, new PaperComponent.Serializer(Utils.serializer(PaperComponent.class, builder)))
                            .register(ComponentTree.class, new ComponentTree.AbstractSerializer() {
                                @Override protected <T extends CalibreIdentifiable> T byId(String id, Class<T> type) { return registry.get(id, type); }
                            })
                            .registerAnnotatedObjects(mapperFactory);
                });

        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.getCommandCompletions().registerCompletion("registry", ctx -> {
            String sType = ctx.getConfig("type");
            Class<?> type = null;
            if (sType != null) {
                try {
                    type = Class.forName(sType);
                } catch (ClassNotFoundException | ClassCastException ignore) {}
            }

            final Class<?> fType = type;
            return registry.getRegistry().entrySet().stream()
                    .filter(entry -> fType == null || fType.isInstance(entry.getValue().get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        });
        commandManager.getCommandContexts().registerContext(CalibreIdentifiable.class, ctx -> {
            String sType = ctx.getFlagValue("type", (String) null);
            Class<?> type = null;
            if (sType != null) {
                try {
                    type = Class.forName(sType);
                } catch (ClassNotFoundException | ClassCastException ignore) {}
            }

            CommandSender sender = ctx.getSender();
            String locale = locale(sender);
            String id = ctx.popFirstArg();
            Ref<? extends CalibreIdentifiable> result = registry.getRef(id);
            if (result == null) {
                sendMessage(sender, gen(locale, "command.error.no_object",
                        "id", id));
                throw new InvalidCommandArgument(false);
            }
            CalibreIdentifiable obj = result.get();
            if (type != null && !type.isInstance(result.get())) {
                sendMessage(sender, gen(locale, "command.error.not_type",
                        "id", id,
                        "found", obj.getClass().getSimpleName(),
                        "expected", type.getSimpleName()));
                throw new InvalidCommandArgument(false);
            }
            return obj;
        });
        commandManager.getCommandContexts().registerContext(ComponentTree.class, ctx -> {
            CommandSender sender = ctx.getSender();
            String locale = locale(sender);
            String json = ctx.popFirstArg();

            ComponentTree tree;
            try {
                tree = ComponentTree.deserialize(json, configOptions);
            } catch (ConfigurateException e) {
                sendMessage(sender, gen(locale, "command.error.parse_tree",
                        "error", TextUtils.combineMessages(e)));
                throw new InvalidCommandArgument(false);
            }

            if (tree == null) {
                sendMessage(sender, gen(locale, "command.error.parse_tree",
                        "error", "null"));
                throw new InvalidCommandArgument(false);
            }

            return tree;
        });
        commandManager.registerCommand(new CalibreCommand(this));

        guiManager = new GUIManager(this);

        audiences = BukkitAudiences.create(this);

        protocol = ProtocolLibrary.getProtocolManager();

        font = new MinecraftFont();

        Bukkit.getPluginManager().registerEvents(new CalibreListener(this), this);

        hooks.forEach(hook -> hook.onRegistryLoad(registry));
        // Defaults
        registry.onLoad(r -> {
            r.register(new PaperGunSystem(this));
            r.register(new PaperStatDisplaySystem(this));
            r.register(new PaperSlotDisplaySystem(this));
            r.register(new PaperSightSystem(this));
            r.register(new PaperFireModeSystem(this));
            r.register(new PaperComponentContainerSystem(this));
            r.register(new PaperCapacityComponentContainerSystem(this));
            r.register(new PaperNameFromChildSystem(this));
        });

        StatDisplaySystem.renderer(DoubleDescriptor.class, (inst, locale, key) -> {
            DoubleDescriptor desc = (DoubleDescriptor) inst.raw();
            DoubleDescriptorStat stat = (DoubleDescriptorStat) inst.stat();
            Component bar = null;
            if (desc.operation() == NumericalDescriptor.Operation.SET && stat.min() != null && stat.max() != null) {
                double min = stat.min();
                double max = stat.max();
                double value = desc.value();
                bar = bar(
                        locale, "system.stat_display.bar." + key,
                        (value - min) / (max - min), 0,
                        setting("system", StatDisplaySystem.ID, "bar_lengths").node(0).getInt()
                );
            }
            Component value = Component.text(stat.valueToString(desc));
            return bar == null ? value : bar.append(value);
        });
        StatDisplaySystem.renderer(Vector2DDescriptor.class, (inst, locale, key) -> {
            Vector2DDescriptor desc = (Vector2DDescriptor) inst.raw();
            Vector2DDescriptorStat stat = (Vector2DDescriptorStat) inst.stat();
            Component bar = null;
            if (desc.operation() == NumericalDescriptor.Operation.SET && stat.min() != null && stat.max() != null) {
                double min = stat.min();
                double max = stat.max();
                double x = desc.value().x();
                double y = desc.value().y();

                int barLength = setting("system", StatDisplaySystem.ID, "bar_lengths").node(1).getInt();
                bar = Component.text()
                        .append(bar(locale, "system.stat_display.bar." + key, (x - min) / (max - min), 0, barLength))
                        .append(bar(locale, "system.stat_display.bar." + key, (y - min) / (max - min), 0, barLength))
                        .build();
            }
            Component value = Component.text(stat.valueToString(desc));
            return bar == null ? value : bar.append(value);
        });

        saveDefaults();
        logAll(load());

        hooks.forEach(CalibreHook::postEnable);

        schedulerLoop = new SchedulerLoop(this);
        schedulerLoop.nextTick(this);
        schedulerLoop.start();
    }

    @Override
    public void onDisable() {
        hooks.forEach(CalibreHook::onDisable);
    }

    public List<CalibreHook> getHooks() { return hooks; }
    public LabelledLogger getPluginLogger() { return logger; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public CalibreRegistry getRegistry() { return registry; }
    public ObjectMapper.Factory getMapperFactory() { return mapperFactory; }
    public ConfigurationOptions getConfigOptions() { return configOptions; }
    public PaperCommandManager getCommandManager() { return commandManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public BukkitAudiences getAudiences() { return audiences; }
    public ProtocolManager getProtocol() { return protocol; }
    public MapFont getFont() { return font; }
    public ConfigurationNode getSettings() { return settings; }
    public StatMapSerializer getStatMapSerializer() { return statMapSerializer; }

    public void saveDefaults() {
        getDataFolder().mkdirs();
        if (!new File(getDataFolder(), SETTINGS_FILE).exists()) {
            for (String path : SAVED_RESOURCES)
                saveResource(path, true);
        }
    }

    public LoggingResult logAll(LoggingResult result) {
        result.forEach(this::log);
        return result;
    }

    //region Load
    public LoggingResult load() {
        return loadSettings()
                .combine(loadLocales())
                .combine(loadRegistry());
    }

    private LoggingResult loadSettings() {
        try {
            settings = HoconConfigurationLoader.builder()
                    .file(BukkitUtils.getRelative(this, SETTINGS_FILE))
                    .defaultOptions(configOptions)
                    .build()
                    .load();
        } catch (ConfigurateException e) {
            settings = BasicConfigurationNode.root();
            return new LoggingResult()
                    .addFailure(LogLevel.ERROR, "Could not load settings from " + SETTINGS_FILE, e);
        }

        logger.setLevel(LogLevel.valueOfDefault(setting("log_level").getString("VERBOSE")));

        settings.node("font_map").childrenMap().forEach((key, node) ->
                font.setChar(key.toString().charAt(0), new MapFont.CharacterSprite(node.getInt(), 0, new boolean[0])));

        return new LoggingResult()
                .addSuccess(LogLevel.INFO, "Loaded settings from " + SETTINGS_FILE);
    }

    private LoggingResult loadLocales() {
        LoggingResult result = new LoggingResult();
        localeManager.unregisterAll();

        LocaleLoader.hocon(BukkitUtils.getRelative(this, "lang"), localeManager).forEach(entry -> {
            if (entry.isSuccessful())
                result.addSuccess(LogLevel.INFO, "Loaded " + ((Translation) entry.getResult()).getLocale() + " from " + entry.getKey());
            else {
                Exception e = ((Exception) entry.getResult());
                result.addFailure(LogLevel.WARN, "Could not load locales from " + entry.getKey(), e);
            }
        });
        return result;
    }

    private LoggingResult loadRegistry() {
        LoggingResult result = new LoggingResult();
        registry.unregisterAll();

        new ConfigurateRegistryLoader.Hocon<CalibreIdentifiable>(getDataFolder())
                .options(configOptions)
                .with("component", PaperComponent.class)
                //.with("blueprint", Blueprint.class)
                .load(registry).forEach(entry -> {
            if (entry.isSuccessful()) {
                Identifiable id = ((Ref<?>) entry.getResult()).get();
                result.addSuccess(LogLevel.VERBOSE, "Loaded " + id.getClass().getSimpleName() + " " + id.id());
            } else {
                Exception e = ((Exception) entry.getResult());
                result.addFailure(LogLevel.WARN, "Could not load registry from " + entry.getKey(), e);
            }
        });

        registry.link().forEach(entry -> {
            Identifiable id = entry.getKey().get();
            if (entry.isSuccessful()) {
                Identifiable dep = ((Ref<?>) entry.getResult()).get();
                result.addSuccess(LogLevel.VERBOSE, "Linked " + id.getClass().getSimpleName() + " " + id.id() +
                        " with " + dep.getClass().getSimpleName() + " " + dep.id());
            } else {
                Exception e = ((Exception) entry.getResult());
                result.addFailure(LogLevel.WARN, "Could not link " + id.getClass().getSimpleName(), e);
            }
        });

        registry.resolve().forEach(entry -> {
            Identifiable id = entry.getKey().get();
            if (entry.isSuccessful()) {
                result.addSuccess(LogLevel.VERBOSE, "Resolved " + id.getClass().getSimpleName() + " " + id.id());
            } else {
                Exception e = ((Exception) entry.getResult());
                result.addFailure(LogLevel.WARN, "Could not resolve " + id.getClass().getSimpleName() + " " + id.id(), e);
            }
        });

        registry.removeUnlinked();

        registry.getRegistry().forEach((id, ref) ->
                result.addSuccess(LogLevel.INFO, "Registered " + ref.get().getClass().getSimpleName() + " " + id));

        return result;
    }
    //endregion

    //region Utils
    public String getDefaultLocale() { return localeManager.getDefaultLocale(); }

    public ConfigurationNode setting(Object... path) { return settings.node(path); }
    public NamespacedKey key(String name) { return keys.computeIfAbsent(name, __ -> new NamespacedKey(this, name)); }

    public Component gen(String locale, String key, Object... args) { return localeManager.gen(locale, key, args); }
    public String locale(CommandSender sender) { return sender instanceof Player ? ((Player) sender).getLocale() : localeManager.getDefaultLocale(); }
    public void sendMessage(CommandSender sender, Component component) { BukkitAudiences.create(this).sender(sender).sendMessage(component); }

    public void log(LogLevel level, String text, Object... args) { logger.log(level, text, args); }
    public void log(LoggingResult.Entry entry) {
        if (setting("print_detailed").getBoolean(true))
            entry.logDetailed(logger);
        else
            entry.logBasic(logger);
    }
    public void log(LogLevel level, Throwable detail, String message, Object... args) {
        log(new LoggingResult.Entry(level, LabelledLogger.format(message, args), detail));
    }
    public void rlog(LogLevel level, String text) { logger.rlog(level, text); }

    private String repeat(String text, double amount) {
        return amount < 1 ? "" : text.repeat((int) amount);
    }

    public Component bar(String locale, String key, double fullPercent, double partPercent, int width) {
        String full = setting("symbol", "full_bar").getString("=");
        String part = setting("symbol", "part_bar").getString("~");
        String empty = setting("symbol", "empty_bar").getString("-");

        int fullWidth = (int) (Utils.clamp01(fullPercent) * width);
        int partWidth = (int) ((Utils.clamp01(partPercent + fullPercent) * width) - fullWidth);
        int emptyWidth = Math.max(0, width - partWidth - fullWidth);

        return gen(locale, key,
                "full", full.repeat(fullWidth),
                "part", part.repeat(partWidth),
                "empty", empty.repeat(emptyWidth));
    }

    public PaperComponent getComponent(ItemStack item) throws ConfigurateException {
        if (item == null || !item.hasItemMeta())
            return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        String json = container.get(key("tree"), PersistentDataType.STRING);
        if (json == null)
            return null;
        return ComponentTree.deserialize(json, configOptions).root();
    }

    public PaperComponent getComponentOrNull(ItemStack item) {
        if (item == null)
            return null;
        try {
            return getComponent(item);
        } catch (ConfigurateException e) {
            return null;
        }
    }

    public void sendPacket(PacketContainer packet, Player target) {
        try {
            protocol.sendServerPacket(target, packet);
        } catch (InvocationTargetException e) {
            log(LogLevel.WARN, e, "Could not send packet to %s (%s)", target.getName(), target.getUniqueId());
        }
    }
    //endregion

    @Override
    public void tick(TickContext tickContext) {
        Bukkit.getOnlinePlayers().forEach(player -> tickPlayer(player, tickContext));
        tickContext.reschedule(this);
    }

    public void tickPlayer(Player player, TickContext tickContext) {
        EntityEquipment equipment = player.getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            PaperComponent component = getComponentOrNull(equipment.getItem(slot));
            if (component != null)
                component.tree().call(BukkitItemEvents.BukkitEquipped.of(component, player, slot, tickContext));
        }

        GUIView view = guiManager.getView(player);
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            SlotViewGUI gui = (SlotViewGUI) view.getGUI();
            if (!gui.check())
                view.getView().close();
        }
    }
}
