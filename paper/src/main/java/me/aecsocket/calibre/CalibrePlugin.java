package me.aecsocket.calibre;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.netty.WirePacket;
import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.blueprint.PaperBlueprint;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.builtin.Formatter;
import me.aecsocket.calibre.system.builtin.*;
import me.aecsocket.calibre.system.gun.*;
import me.aecsocket.calibre.system.gun.reload.external.PaperSingleChamberReloadSystem;
import me.aecsocket.calibre.system.gun.reload.external.PaperRemoveReloadSystem;
import me.aecsocket.calibre.system.gun.reload.internal.PaperInsertReloadSystem;
import me.aecsocket.calibre.util.*;
import me.aecsocket.calibre.util.item.ItemManager;
import me.aecsocket.unifiedframework.gui.GUIManager;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.locale.LocaleLoader;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.locale.Translation;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.ThreadLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.Ref;
import me.aecsocket.unifiedframework.registry.loader.ConfigurateRegistryLoader;
import me.aecsocket.unifiedframework.resource.result.LoggingResult;
import me.aecsocket.unifiedframework.serialization.configurate.*;
import me.aecsocket.unifiedframework.serialization.configurate.descriptor.NumberDescriptorSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.descriptor.Vector2DDescriptorSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.descriptor.Vector3DDescriptorSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector2DSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector2ISerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector3DSerializer;
import me.aecsocket.unifiedframework.serialization.configurate.vector.Vector3ISerializer;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.util.*;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector2DDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector3DDescriptor;
import me.aecsocket.unifiedframework.util.log.LabelledLogger;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector2I;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import me.aecsocket.unifiedframework.util.vector.Vector3I;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingSchemes;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Core plugin class for Paper servers.
 */
public class CalibrePlugin extends JavaPlugin implements Tickable {
    public static final String SETTINGS_FILE = "settings.conf";
    public static final int THREAD_LOOP_PERIOD = 10;
    public static final List<String> SAVED_RESOURCES = Arrays.asList(
            "README.txt",
            SETTINGS_FILE,
            "lang/default_en_us.conf"
    );

    private static CalibrePlugin instance;
    public static CalibrePlugin instance() { return instance; }

    private final List<CalibreHook> hooks = new ArrayList<>();
    private final Map<Player, CalibrePlayer> players = new HashMap<>();
    private final LabelledLogger logger = new LabelledLogger(getLogger());
    private final LocaleManager localeManager = new LocaleManager();
    private final ItemManager itemManager = new ItemManager(this);
    private final SchedulerSystem.Scheduler systemScheduler = new SchedulerSystem.Scheduler(10000, 100);
    private final CasingManager casingManager = new CasingManager(this);
    private final LocationalDamageManager locationalDamageManager = new LocationalDamageManager(this);
    private final VelocityTracker velocityTracker = new VelocityTracker();
    private final CalibreRegistry registry = new CalibreRegistry();
    private final Map<Class<?>, Formatter<?>> statFormatters = new HashMap<>();
    private final SchedulerLoop schedulerLoop = new SchedulerLoop(this);
    private final ThreadLoop threadLoop = new ThreadLoop(THREAD_LOOP_PERIOD);
    private ConfigurationOptions configOptions;
    private PaperCommandManager commandManager;
    private GUIManager guiManager;
    private BukkitAudiences audiences;
    private ProtocolManager protocol;
    private MapFont font;
    private ConfigurationNode settings;
    private StatMapSerializer statMapSerializer;

    private final Map<String, NamespacedKey> keys = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin instanceof CalibreHook)
                hooks.add((CalibreHook) plugin);
        }

        hooks.forEach(hook -> hook.onEnable(this));

        guiManager = new GUIManager(this);
        audiences = BukkitAudiences.create(this);
        protocol = ProtocolLibrary.getProtocolManager();
        font = new MinecraftFont();

        createConfigOptions();
        createCommandManager();
        registerDefaults();

        saveDefaults();
        logAll(load());

        schedulerLoop.register(this);
        schedulerLoop.register(systemScheduler);
        schedulerLoop.register(casingManager);
        schedulerLoop.register(velocityTracker);
        schedulerLoop.start();

        threadLoop.register(this);
        threadLoop.start();

        hooks.forEach(CalibreHook::postEnable);
    }

    @Override
    public void onDisable() {
        hooks.forEach(CalibreHook::onDisable);
        if (schedulerLoop.running())
            schedulerLoop.stop();
        if (threadLoop.running())
            threadLoop.stop();
    }

    public List<CalibreHook> hooks() { return hooks; }
    public Map<Player, CalibrePlayer> players() { return players; }
    public LabelledLogger pluginLogger() { return logger; }
    public LocaleManager localeManager() { return localeManager; }
    public ItemManager itemManager() { return itemManager; }
    public SchedulerSystem.Scheduler systemScheduler() { return systemScheduler; }
    public CasingManager casingManager() { return casingManager; }
    public LocationalDamageManager locationalDamageManager() { return locationalDamageManager; }
    public VelocityTracker velocityTracker() { return velocityTracker; }
    public CalibreRegistry registry() { return registry; }
    public Map<Class<?>, Formatter<?>> statFormatters() { return statFormatters; }
    public SchedulerLoop schedulerLoop() { return schedulerLoop; }
    public ThreadLoop threadLoop() { return threadLoop; }
    public ConfigurationOptions configOptions() { return configOptions; }
    public PaperCommandManager commandManager() { return commandManager; }
    public GUIManager guiManager() { return guiManager; }
    public BukkitAudiences audiences() { return audiences; }
    public ProtocolManager protocol() { return protocol; }
    public MapFont font() { return font; }
    public ConfigurationNode settings() { return settings; }
    public StatMapSerializer statMapSerializer() { return statMapSerializer; }

    @SuppressWarnings("unchecked")
    public <T> Formatter<T> statFormatter(Class<T> type) { return (Formatter<T>) statFormatters.get(type); }
    public <T> CalibrePlugin statFormatter(Class<T> type, Formatter<T> formatter) { statFormatters.put(type, formatter); return this; }

    public CalibrePlayer playerData(Player player) {
        return players.computeIfAbsent(player, __ -> new CalibrePlayer(this, player));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void createConfigOptions() {
        configOptions = ConfigurationOptions.defaults()
                .serializers(builder -> {
                    hooks.forEach(hook -> hook.registerSerializers(builder));
                    statMapSerializer = new StatMapSerializer();
                    ObjectMapper.Factory mapper = ObjectMapper.factoryBuilder()
                            .defaultNamingScheme(NamingSchemes.SNAKE_CASE)
                            .build();
                    TypeSerializer systemSerializer = new CalibreSystem.Serializer(Utils.delegate(CalibreSystem.class, builder, mapper), NamingSchemes.SNAKE_CASE);
                    builder
                            .register(Color.class, ColorSerializer.INSTANCE)
                            .register(BlockData.class, BlockDataSerializer.INSTANCE)
                            .register(Particle.DustOptions.class, DustOptionsSerializer.INSTANCE)
                            .register(ItemStack.class, ItemStackSerializer.INSTANCE)
                            .register(Location.class, LocationSerializer.INSTANCE)
                            .register(NamespacedKey.class, NamespacedKeySerializer.INSTANCE)

                            .register(NumberDescriptor.Byte.class, NumberDescriptorSerializer.Byte.INSTANCE)
                            .register(NumberDescriptor.Short.class, NumberDescriptorSerializer.Short.INSTANCE)
                            .register(NumberDescriptor.Integer.class, NumberDescriptorSerializer.Integer.INSTANCE)
                            .register(NumberDescriptor.Long.class, NumberDescriptorSerializer.Long.INSTANCE)
                            .register(NumberDescriptor.Float.class, NumberDescriptorSerializer.Float.INSTANCE)
                            .register(NumberDescriptor.Double.class, NumberDescriptorSerializer.Double.INSTANCE)
                            .register(Vector2DDescriptor.class, Vector2DDescriptorSerializer.INSTANCE)
                            .register(Vector3DDescriptor.class, Vector3DDescriptorSerializer.INSTANCE)

                            .register(Vector.class, VectorSerializer.INSTANCE)
                            .register(GUIVector.class, GUIVectorSerializer.INSTANCE)
                            .register(Vector3D.class, Vector3DSerializer.INSTANCE)
                            .register(Vector3I.class, Vector3ISerializer.INSTANCE)
                            .register(Vector2D.class, Vector2DSerializer.INSTANCE)
                            .register(Vector2I.class, Vector2ISerializer.INSTANCE)

                            .register(ParticleData.class, ParticleDataSerializer.INSTANCE)
                            .register(SoundData.class, new SoundDataSerializer(this))

                            .register(StatMap.class, statMapSerializer)
                            .register(StatCollection.class, StatCollection.Serializer.INSTANCE)
                            .register(CasingManager.Category.class, new CasingManager.Category.Serializer(this, Utils.delegate(CasingManager.Category.class, builder, mapper)))

                            .register(ComponentContainerSystem.class, new ComponentContainerSystem.Serializer((TypeSerializer<ComponentContainerSystem>) systemSerializer))
                            .register(SchedulerSystem.class, new SchedulerSystem.Serializer((TypeSerializer<SchedulerSystem>) systemSerializer))
                            .register(CalibreSystem.class, systemSerializer)

                            .register(new TypeToken<Quantifier<ComponentTree>>(){}, new QuantifierSerializer<>())
                            .register(ItemAnimation.class, new ItemAnimation.Serializer(this))
                            .register(SightPath.class, SightPath.Serializer.INSTANCE)
                            .register(FireModePath.class, FireModePath.Serializer.INSTANCE)
                            .register(PaperComponent.class, new PaperComponent.Serializer(Utils.delegate(PaperComponent.class, builder, mapper)))
                            .register(ComponentTree.class, new ComponentTree.AbstractSerializer() {
                                @Override protected <T extends CalibreIdentifiable> T byId(String id, Class<T> type) { return registry.get(id, type); }
                                @Override protected boolean preserveInvalidData() { return setting("preserve_invalid_data").getBoolean(true); }
                            })
                            .registerAnnotatedObjects(mapper);
                });
    }

    protected void registerDefaults() {
        Bukkit.getPluginManager().registerEvents(new CalibreListener(this), this);
        protocol.addPacketListener(new CalibrePacketAdapter(this));

        statFormatters.put(NumberDescriptor.Byte.class, new PaperFormatter.NumberDescriptorFormatter<Byte, NumberDescriptor.Byte>(this));
        statFormatters.put(NumberDescriptor.Short.class, new PaperFormatter.NumberDescriptorFormatter<Short, NumberDescriptor.Short>(this));
        statFormatters.put(NumberDescriptor.Integer.class, new PaperFormatter.NumberDescriptorFormatter<Integer, NumberDescriptor.Integer>(this));
        statFormatters.put(NumberDescriptor.Long.class, new PaperFormatter.NumberDescriptorFormatter<Long, NumberDescriptor.Long>(this));
        statFormatters.put(NumberDescriptor.Float.class, new PaperFormatter.NumberDescriptorFormatter<Float, NumberDescriptor.Float>(this));
        statFormatters.put(NumberDescriptor.Double.class, new PaperFormatter.NumberDescriptorFormatter<Double, NumberDescriptor.Double>(this));
        statFormatters.put(Vector2DDescriptor.class, new PaperFormatter.Vector2DDescriptorFormatter(this));

        registry.onLoad(r -> {
            PaperStatDisplaySystem statDisplay = new PaperStatDisplaySystem(this, this::statFormatter);
            r.register(statDisplay);
            r.register(new PaperSlotDisplaySystem(this));
            r.register(new PaperComponentContainerSystem(this));
            r.register(new PaperCapacityComponentContainerSystem(this));
            r.register(new PaperNameFromChildSystem(this));
            r.register(new PaperSchedulerSystem(this, systemScheduler));

            r.register(new GenericStatsSystem(this));
            r.register(new InventoryComponentAccessorSystem(this));

            r.register(new PaperGunSystem(this));
            r.register(new PaperGunInfoSystem(this));
            r.register(new PaperSwayStabilizationSystem(this));
            r.register(new PaperFireModeSystem(this));
            r.register(new PaperSightSystem(this));
            r.register(new PaperChamberSystem(this));
            r.register(new PaperSingleChamberReloadSystem(this));
            r.register(new PaperInsertReloadSystem(this));
            r.register(new PaperRemoveReloadSystem(this));
            r.register(new BulletSystem(this));
        });
    }

    protected void createCommandManager() {
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
            String input = ctx.popFirstArg();

            ComponentTree tree;
            try {
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                        .source(() -> new BufferedReader(new StringReader(input)))
                        .build();
                tree = loader.load().get(ComponentTree.class);
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
    }

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
        hooks.forEach(CalibreHook::preLoad);
        LoggingResult result = loadSettings()
                .combine(loadLocales())
                .combine(loadRegistry());
        hooks.forEach(CalibreHook::postLoad);
        return result;
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
                    .addFailure(LogLevel.ERROR, e, "Could not load settings from " + SETTINGS_FILE);
        }

        logger.setLevel(LogLevel.valueOfDefault(setting("log_level").getString("VERBOSE")));

        settings.node("font_map").childrenMap().forEach((key, node) ->
                font.setChar(key.toString().charAt(0), new MapFont.CharacterSprite(node.getInt(), 0, new boolean[0])));

        systemScheduler.cleanDelay(setting("scheduler", "clean_delay").getLong(10000));
        systemScheduler.cleanThreshold(setting("scheduler", "clean_threshold").getLong(100));

        casingManager.load();
        locationalDamageManager.load();

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
                result.addFailure(LogLevel.WARN, e, "Could not load locales from " + entry.getKey());
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
                .with("blueprint", PaperBlueprint.class)
                .load(registry).forEach(entry -> {
            if (entry.isSuccessful()) {
                Identifiable id = ((Ref<?>) entry.getResult()).get();
                result.addSuccess(LogLevel.VERBOSE, "Loaded " + id.getClass().getSimpleName() + " " + id.id());
            } else {
                Exception e = ((Exception) entry.getResult());
                result.addFailure(LogLevel.WARN, e, "Could not load registry from %s", entry.getKey());
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
                result.addFailure(LogLevel.WARN, e, "Could not link %s", id.getClass().getSimpleName());
            }
        });

        registry.resolve().forEach(entry -> {
            Identifiable id = entry.getKey().get();
            if (entry.isSuccessful()) {
                result.addSuccess(LogLevel.VERBOSE, "Resolved " + id.getClass().getSimpleName() + " " + id.id());
            } else {
                Exception e = ((Exception) entry.getResult());
                result.addFailure(LogLevel.WARN, e, "Could not resolve %s %s", id.getClass().getSimpleName(), id.id());
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

    public void sendPacket(PacketContainer packet, Player target, boolean wire) {
        try {
            if (wire) {
                WirePacket wirePacket = WirePacket.fromPacket(packet);
                protocol.sendWirePacket(target, wirePacket);
            } else
                protocol.sendServerPacket(target, packet);
        } catch (InvocationTargetException | RuntimeException e) {
            log(LogLevel.WARN, e, "Could not send packet to %s (%s)", target.getName(), target.getUniqueId());
        }
    }

    public void sendPacket(PacketContainer packet, Player target) {
        sendPacket(packet, target, false);
    }

    public void sendPacket(Player target, PacketType type, boolean wire, Consumer<PacketContainer> builder) {
        PacketContainer packet = new PacketContainer(type);
        builder.accept(packet);
        sendPacket(packet, target, wire);
    }

    public void sendPacket(Player target, PacketType type, Consumer<PacketContainer> builder) {
        sendPacket(target, type, false, builder);
    }

    public double hardness(Block block) {
        Material material = block.getType();
        return setting("hardness", material.getKey().getKey()).getDouble(material.getBlastResistance());
    }
    //endregion

    @Override
    public void tick(TickContext tickContext) {
        Bukkit.getOnlinePlayers().forEach(player -> tickContext.tick(playerData(player)));
    }
}