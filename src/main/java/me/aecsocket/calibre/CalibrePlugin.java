package me.aecsocket.calibre;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import me.aecsocket.calibre.defaults.DefaultCalibreHook;
import me.aecsocket.calibre.handle.CalibreCommand;
import me.aecsocket.calibre.handle.CalibrePacketAdapter;
import me.aecsocket.calibre.handle.EventHandle;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.calibre.item.blueprint.Blueprint;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.util.user.EntityItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.*;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.item.ItemAdapter;
import me.aecsocket.unifiedframework.item.ItemManager;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.locale.Translation;
import me.aecsocket.unifiedframework.locale.TranslationMap;
import me.aecsocket.unifiedframework.locale.TranslationMapAdapter;
import me.aecsocket.unifiedframework.loop.PreciseLoop;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.registry.LinkingException;
import me.aecsocket.unifiedframework.registry.Ref;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.resource.ResourceLoadException;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.Vector2;
import me.aecsocket.unifiedframework.util.json.GUIVectorAdapter;
import me.aecsocket.unifiedframework.util.json.Vector2Adapter;
import me.aecsocket.unifiedframework.util.json.VectorAdapter;
import me.aecsocket.unifiedframework.util.log.LabelledLogger;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main plugin class.
 */
public class CalibrePlugin extends JavaPlugin implements Tickable {
    /** A list of all resources in this JAR that will be automatically placed in if no data folder is found. */
    public static final List<String> DEFAULT_RESOURCES = Arrays.asList(
            "README.txt",
            "settings.json",
            "lang/default_en_us.json"
    );

    /** The list of hooks active on this plugin. */
    private final List<CalibreHook> hooks = new ArrayList<>();
    /** The plugin's custom logger. */
    private final LabelledLogger logger = new LabelledLogger(getLogger());
    /** The central settings for the plugin. */
    private final Settings settings = new Settings();
    /** The central registry for the plugin. */
    private final CalibreRegistry registry = new CalibreRegistry();
    /** The object used for translating messages. */
    private final LocaleManager localeManager = new LocaleManager();
    /** The plugin's Bukkit scheduler loop. */
    private final SchedulerLoop schedulerLoop = new SchedulerLoop(this);
     /** The plugin's thread-based loop. */
    private final PreciseLoop preciseLoop = new PreciseLoop(10);
    /** The default hook which is registered as a hook. */
    private final DefaultCalibreHook defaultHook = new DefaultCalibreHook();
    /** The configurable StatMapAdapter for this plugin's GSON. */
    private final StatMapAdapter statMapAdapter = new StatMapAdapter();
    /** The item manager for the plugin. */
    private final ItemManager itemManager = new ItemManager(this);
    /** The map of names to plugin keys. */
    private final Map<String, NamespacedKey> keys = new HashMap<>();
    /** The map of players to player data. */
    private final Map<Player, CalibrePlayer> players = new HashMap<>();

    /** The central Gson instance. */
    private Gson gson;
    /** The plugin's protocol manager instance. */
    private ProtocolManager protocolManager;
    /** The plugin's command manager for ACF commands. */
    private PaperCommandManager commandManager;
    /** The plugin's main command instance. */
    private CalibreCommand command;

    //region Plugin

    @Override
    public void onEnable() {
        // Hooks
        hooks.add(defaultHook); // todo way to disable
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin instanceof CalibreHook) {
                CalibreHook hook = (CalibreHook) plugin;
                hooks.add(hook);
            }
        }
        hooks.forEach(hook -> {
            hook.acceptPlugin(this);
            hook.onEnable();
            hook.getPreRegisters().forEach(registry::addPreRegister);
        });

        // GSON
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getAnnotation(Expose.class) != null && !f.getAnnotation(Expose.class).serialize();
                    }

                    @Override public boolean shouldSkipClass(Class<?> clazz) { return false; }
                })
                .registerTypeAdapter(TranslationMap.class, new TranslationMapAdapter())
                .registerTypeAdapter(StatMap.class, statMapAdapter)
                .registerTypeAdapter(GUIVector.class, GUIVectorAdapter.INSTANCE)
                .registerTypeAdapter(Vector.class, VectorAdapter.INSTANCE)
                .registerTypeAdapter(Vector2.class, Vector2Adapter.INSTANCE)
                .registerTypeAdapter(ComponentTree.class, new ComponentTree.Adapter(this))
                .registerTypeAdapter(ItemAnimation.class, new ItemAnimation.Adapter(this))
                .registerTypeAdapterFactory(new CalibreComponent.Adapter())
                .registerTypeAdapterFactory(new Blueprint.Adapter())
                .registerTypeAdapterFactory(new AcceptsCalibrePlugin.Adapter(this));
        hooks.forEach(hook -> hook.registerTypeAdapters(gsonBuilder));
        gson = gsonBuilder.create();

        // Commands
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.getCommandCompletions().registerCompletion("players", context -> Utils.getCommandPlayerEntries(context.getSender()));
        commandManager.getCommandCompletions().registerCompletion("registry", context -> {
            String sType = context.getConfig("type");
            Class<?> type = null;
            if (sType != null) {
                try {
                    type = Class.forName(sType);
                } catch (ClassNotFoundException ignore) {}
            }

            final Class<?> fType = type;
            return registry.getRegistry().entrySet().stream()
                    .filter(entry -> fType == null || fType.isInstance(entry.getValue().get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        });
        commandManager.getCommandContexts().registerContext(Player.class, context -> Utils.getCommandTarget(context.popFirstArg(), context.getSender()));
        commandManager.getCommandContexts().registerContext(Ref.class, context -> {
            String sType = context.getFlagValue("type", (String) null);
            Class<?> type = null;
            if (sType != null) {
                try {
                    type = Class.forName(sType);
                } catch (ClassNotFoundException ignore) {}
            }

            CommandSender sender = context.getSender();
            String id = context.popFirstArg();
            Ref<?> result = registry.getRef(id);
            if (result == null) throw new InvalidCommandArgument(gen(sender, "chat.command.not_found", "id", id));
            if (type != null && !type.isInstance(result.get())) throw new InvalidCommandArgument(gen(sender, "chat.command.wrong_type", "id", id));
            return result;
        });
        command = new CalibreCommand(this);
        commandManager.registerCommand(command);

        // Other
        itemManager.registerAdapter(new ItemAdapter<CalibreComponent>() {
            private long nextError = 0;

            @Override public String getItemType() { return CalibreComponent.ITEM_TYPE; }

            @Override
            public CalibreComponent deserialize(ItemStack stack, ItemMeta meta, PersistentDataContainer data) {
                if (!data.has(key("tree"), PersistentDataType.STRING)) return null;
                String json = data.get(key("tree"), PersistentDataType.STRING);
                try {
                    ComponentTree tree = gson.fromJson(json, ComponentTree.class);
                    return tree == null ? null : tree.getRoot();
                } catch (JsonParseException e) {
                    if (System.currentTimeMillis() >= nextError) {
                        elog(LogLevel.WARN, e, "Failed to parse item JSON: {msg}\n{json}",
                                "msg", e.getMessage(), "json", prettyPrinter().toJson(gson.fromJson(json, JsonElement.class)));
                        nextError = System.currentTimeMillis() + setting("repeat_error_delay", long.class, 60000L);
                    }
                    return null;
                }
            }
        });

        protocolManager = ProtocolLibrary.getProtocolManager();
        CalibreProtocol.initialize(this);
        protocolManager.addPacketListener(new CalibrePacketAdapter(this));

        Bukkit.getPluginManager().registerEvents(new EventHandle(this), this);

        // Loading
        registry.preRegister();
        logLoad();

        // Loops
        schedulerLoop.registerTickable(this);
        preciseLoop.registerTickable(this);
        schedulerLoop.start();
        preciseLoop.start();
    }

    @Override
    public void onDisable() {
        hooks.forEach(CalibreHook::onDisable);
        schedulerLoop.stop();
        preciseLoop.stop();
    }

    //endregion

    //region Getters

    public List<CalibreHook> getHooks() { return hooks; }
    public LabelledLogger getLabelledLogger() { return logger; }
    public Settings getSettings() { return settings; }
    public CalibreRegistry getRegistry() { return registry; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public SchedulerLoop getSchedulerLoop() { return schedulerLoop; }
    public PreciseLoop getPreciseLoop() { return preciseLoop; }
    public DefaultCalibreHook getDefaultHook() { return defaultHook; }
    public StatMapAdapter getStatMapAdapter() { return statMapAdapter; }
    public ItemManager getItemManager() { return itemManager; }
    public Map<Player, CalibrePlayer> getPlayers() { return players; }

    public Gson getGson() { return gson; }
    public ProtocolManager getProtocolManager() { return protocolManager; }
    public PaperCommandManager getCommandManager() { return commandManager; }
    public CalibreCommand getCommand() { return command; }

    //endregion

    //region Loading

    /**
     * Unloads all loaded data.
     */
    public void unload() {
        settings.clear();
        registry.unregisterAll();
        localeManager.unregisterAll();
    }

    public void createDefaults() {
        getDataFolder().mkdir();
        for (String resource : DEFAULT_RESOURCES)
            saveResource(resource, true);
    }

    /**
     * Loads all data to load.
     * @return The messages produced.
     */
    public LogMessageResult load() {
        LogMessageResult result = new LogMessageResult();
        if (!getDataFolder().exists()) {
            result.addFailureData(LogLevel.WARN, "No data directory found, creating");
            createDefaults();
        }

        return result.combine(loadSettings())
                .combine(loadRegistry())
                .combine(loadLocales());
    }

    /**
     * Loads all data to load, and logs messages to the console.
     * @return The messages produced.
     */
    public LogMessageResult logLoad() {
        LogMessageResult result = load();
        result.forEach(entry -> {
            LogMessageResult.Message msg = (LogMessageResult.Message) entry.getResult();
            logger.rlog(msg.getLevel(), msg.getMessage() + (msg.getDetail() != null && setting("print_detailed", boolean.class, true) ? "\n" + msg.getDetail() : ""));
        });
        return result;
    }

    /**
     * Unloads then loads all data to load.
     * @return The messages produced.
     */
    public LogMessageResult reload() {
        unload();
        return load();
    }

    /**
     * Unloads then loads all data to load, and logs messages to the console.
     * @return The messages produced.
     */
    public LogMessageResult logReload() {
        unload();
        return logLoad();
    }

    /**
     * Loads the settings, and initializes values based on them.
     * @return The messages produced.
     */
    public LogMessageResult loadSettings() {
        LogMessageResult result = new LogMessageResult();
        try {
            settings.loadFrom(this);
        } catch (ResourceLoadException e) {
            result.addFailureData(
                    LogLevel.ERROR,
                    "Could not load settings: " + e.getMessage(),
                    getTrace(e)
            );
        }

        setting("log_level", String.class).ifPresent(level -> {
            LogLevel level2 = LogLevel.valueOfDefault(level);
            if (level2 != null) logger.setLevel(level2);
        });
        setting("default_locale", String.class).ifPresent(localeManager::setDefaultLocale);

        return result;
    }

    /**
     * Loads the registry.
     * @return The messages produced.
     */
    public LogMessageResult loadRegistry() {
        LogMessageResult result = new LogMessageResult();
        CalibreRegistry.LoadContext load;
        try {
            load = registry.loadFrom(gson, this)
                    .with("component", CalibreComponent.class)
                    .with("blueprint", Blueprint.class)
                    .load();
        } catch (ResourceLoadException e) {
            result.addFailureData(
                    LogLevel.ERROR,
                    "Could not load registry: " + e.getMessage(),
                    getTrace(e)
            );
            return result;
        }

        load.getLoadResult().forEach(entry -> {
            if (entry.isSuccessful()) {
                @SuppressWarnings("unchecked")
                CalibreIdentifiable object = ((Ref<CalibreIdentifiable>) entry.getResult()).get();
                result.addSuccessData(
                        LogLevel.VERBOSE,
                        TextUtils.format("Loaded {type} {id}",
                        "type", object.getClass().getSimpleName(), "id", object.getId())
                );
            } else {
                Exception e = (Exception) entry.getResult();
                result.addFailureData(
                        LogLevel.WARN,
                        TextUtils.format("Could not load {path}: {msg}",
                        "path", entry.getKey(), "msg", e.getMessage()),
                        getTrace(e)
                );
            }
        });

        registry.link().forEach(entry -> {
            CalibreIdentifiable object = entry.getKey().get();
            if (entry.isSuccessful()) {
                @SuppressWarnings("unchecked")
                CalibreIdentifiable dep = ((Ref<CalibreIdentifiable>) entry.getResult()).get();
                result.addSuccessData(
                        LogLevel.VERBOSE,
                        TextUtils.format("Linked {type} {id} with {dtype} {did}",
                        "type", object.getClass().getSimpleName(), "id", object.getId(),
                        "dtype", dep.getClass().getSimpleName(), "did", dep.getId())
                );
            } else {
                LinkingException e = (LinkingException) entry.getResult();
                result.addFailureData(
                        LogLevel.WARN,
                        TextUtils.format("Could not link {type} {id}: {msg}",
                        "type", object.getClass().getSimpleName(), "id", object.getId(), "msg", e.getMessage()),
                        getTrace(e)
                );
            }
        });

        registry.resolve().forEach(entry -> {
            CalibreIdentifiable object = entry.getKey().get();
            if (entry.isSuccessful()) {
                result.addSuccessData(
                        LogLevel.VERBOSE,
                        TextUtils.format("Resolved {type} {id}",
                        "type", object.getClass().getSimpleName(), "id", object.getId())
                );
            } else {
                ResolutionException e = (ResolutionException) entry.getResult();
                result.addFailureData(
                        LogLevel.WARN,
                        TextUtils.format("Could not resolve {type} {id}: {msg}",
                        "type", object.getClass().getSimpleName(), "id", object.getId(), "msg", e.getMessage()),
                        getTrace(e)
                );
            }
        });

        return result;
    }

    /**
     * Loads the locales.
     * @return The messages produced.
     */
    public LogMessageResult loadLocales() {
        LogMessageResult result = new LogMessageResult();
        try {
            localeManager.loadFrom(gson, this).forEach(entry -> {
                if (entry.isSuccessful()) {
                    result.addSuccessData(
                            LogLevel.VERBOSE,
                            TextUtils.format("Loaded locale {locale} from {path}",
                            "locale", ((Translation) entry.getResult()).getLocale(), "path", entry.getKey())
                    );
                } else {
                    Exception e = (Exception) entry.getResult();
                    result.addFailureData(
                            LogLevel.WARN,
                            TextUtils.format("Could not load locale from {path}: {msg}",
                            "path", entry.getKey(), "msg", e.getMessage()),
                            getTrace(e)
                    );
                }
            });
        } catch (ResourceLoadException e) {
            result.addFailureData(
                    LogLevel.ERROR,
                    TextUtils.format("trace"),
                    getTrace(e)
            );
        }
        return result;
    }

    //endregion

    public CalibrePlayer getPlayerData(Player player) { return players.computeIfAbsent(player, __ -> new CalibrePlayer(this, player)); }
    public PlayerItemUser userOf(Player player) { return getPlayerData(player).getUser(); }
    public ItemUser userOf(Entity entity) { return entity instanceof Player ? userOf((Player) entity) : EntityItemUser.of(entity); }

    @Override
    public void tick(TickContext tickContext) {
        Bukkit.getOnlinePlayers().forEach(player ->
                tickContext.tick(getPlayerData(player)));
    }


    //region Utils

    public String getTrace(Throwable e) { return String.join("\n", TextUtils.prefixLines(Utils.getStackTrace(e), "    ")); }

    public void log(LogLevel level, String text, Object... args) { logger.log(level, text, args); }
    public void elog(LogLevel level, Throwable e, String text, Object... args) {
        log(level, text, args);
        if (setting("print_detailed", boolean.class, true)) {
            for (String line : getTrace(e).split("\n"))
                log(level, line);
        }
    }

    public NamespacedKey key(String key) { return keys.computeIfAbsent(key, __ -> new NamespacedKey(this, key)); }

    public <T> T setting(String path, Type type, T defaultValue) { return settings.get(path, type, defaultValue); }
    public <T> T setting(String path, Class<T> type, T defaultValue) { return settings.get(path, type, defaultValue); }
    public <T> Optional<T> setting(String path, Type type) { return settings.get(path, type); }
    public <T> Optional<T> setting(String path, Class<T> type) { return settings.get(path, type); }

    public String locale(CommandSender sender) { return sender instanceof Player ? ((Player) sender).getLocale() : localeManager.getDefaultLocale(); }

    public String gen(String locale, String key, Object... args) { return localeManager.gen(locale, key, args); }
    public String gen(String key, Object... args) { return gen(localeManager.getDefaultLocale(), key, args); }
    public String gen(CommandSender sender, String key, Object... args) { return gen(locale(sender), key, args); }

    public Optional<String> rgen(String locale, String key, Object... args) { return localeManager.rgen(locale, key, args); }
    public Optional<String> rgen(String key, Object... args) { return rgen(localeManager.getDefaultLocale(), key, args); }
    public Optional<String> rgen(CommandSender sender, String key, Object... args) { return rgen(locale(sender), key, args); }

    public <E extends CalibreIdentifiable> E fromRegistry(String id, Class<E> type) { return registry.get(id, type); }
    public CalibreComponent fromItem(ItemStack item) { return item == null ? null : itemManager.getItem(item, CalibreComponent.class); }

    public Gson prettyPrinter() { return gson.newBuilder().setPrettyPrinting().create(); }

    public void sendPacket(Player player, PacketContainer packer) {
        try {
            protocolManager.sendServerPacket(player, packer);
        } catch (InvocationTargetException ignore) {}
    }

    //endregion
}
