package me.aecsocket.calibre;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.handle.CalibreCommand;
import me.aecsocket.calibre.hook.CalibreCoreHook;
import me.aecsocket.calibre.hook.CalibreHook;
import me.aecsocket.calibre.item.CalibreIdentifiable;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.blueprint.Blueprint;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.util.CalibrePlayer;
import me.aecsocket.calibre.util.RegistryCommandContext;
import me.aecsocket.unifiedframework.item.ItemManager;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.locale.Translation;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.Ref;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.registry.ResolutionException;
import me.aecsocket.unifiedframework.resource.DataResult;
import me.aecsocket.unifiedframework.resource.LoadResult;
import me.aecsocket.unifiedframework.resource.ResourceLoadException;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.log.LabelledLogger;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * The main plugin class. Other plugins can hook into this by:
 * <ul>
 *     <li>Adding this plugin as a <code>loadbefore</code> in the <code>plugin.yml</code></li>
 *     <li>Extending {@link CalibreHook}</li>
 * </ul>
 */
public class CalibrePlugin extends JavaPlugin implements Tickable {
    private final LabelledLogger logger = new LabelledLogger(getLogger(), LogLevel.VERBOSE);
    private final Settings settings = new Settings();
    private final LocaleManager localeManager = new LocaleManager();
    private final Registry registry = new Registry();
    private final Set<CalibreHook> hooks = new HashSet<>();
    private final CalibreCoreHook coreHook = new CalibreCoreHook();
    private final Map<Player, CalibrePlayer> players = new HashMap<>();
    private final ItemManager itemManager = new ItemManager(this);
    private final SchedulerLoop schedulerLoop = new SchedulerLoop(this);
    private PaperCommandManager commandManager;
    private Gson gson;

    @Override
    public void onEnable() {
        commandManager = new PaperCommandManager(this);

        String pluginName = getDescription().getName();
        hooks.add(coreHook); // TODO an option to disable the core hook
        Stream.of(Bukkit.getPluginManager().getPlugins())
                .filter(plugin -> plugin instanceof CalibreHook && plugin.getDescription().getLoadBefore().contains(pluginName))
                .map(plugin -> (CalibreHook) plugin)
                .forEach(hooks::add);

        GsonBuilder gsonBuilder = new GsonBuilder();
        hooks.forEach(hook -> hook.initializeGson(this, gsonBuilder));
        gson = gsonBuilder.create();

        load().forEach(entry -> log(entry.isSuccessful() ? LogLevel.VERBOSE : LogLevel.WARN, (String) entry.getResult()));

        commandManager.getCommandContexts().registerContext(CalibreIdentifiable.class, new RegistryCommandContext<>(CalibreIdentifiable.class, registry));
        commandManager.getCommandContexts().registerContext(CalibreItem.class, new RegistryCommandContext<>(CalibreItem.class, registry));
        commandManager.getCommandContexts().registerContext(Player.class, context -> {
            String id = context.popFirstArg();
            Player result = Utils.getCommandTarget(id, context.getSender());
            if (result == null) throw new InvalidCommandArgument("No player with identifier " + id + " found");
            return result;
        });
        commandManager.getCommandCompletions().registerCompletion("players", context -> Utils.getCommandPlayerEntries(context.getSender()));
        commandManager.getCommandCompletions().registerCompletion("registry", context -> {
            String extend = context.getConfig("extends");
            Class<?> type = null;
            if (extend != null) {
                try { type = Class.forName(extend); }
                catch (ClassNotFoundException ignore) {}
            }
            List<String> result = new ArrayList<>();
            final Class<?> fType = type;
            registry.getRegistry().forEach((id, ref) -> {
                Identifiable raw = ref.get();
                if (fType == null || fType.isAssignableFrom(raw.getClass())) result.add(id);
            });
            return result;
        });
        commandManager.registerCommand(new CalibreCommand(this));

        schedulerLoop.registerTickable(this);
    }

    public LabelledLogger getPluginLogger() { return logger; }
    public Settings getSettings() { return settings; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public Registry getRegistry() { return registry; }
    public Set<CalibreHook> getHooks() { return hooks; }
    public CalibreCoreHook getCoreHook() { return coreHook; }
    public Map<Player, CalibrePlayer> getPlayers() { return players; }
    public ItemManager getItemManager() { return itemManager; }
    public SchedulerLoop getSchedulerLoop() { return schedulerLoop; }
    public PaperCommandManager getCommandManager() { return commandManager; }
    public Gson getGson() { return gson; }

    public CalibrePlayer getPlayerData(Player player) { return players.computeIfAbsent(player, p -> new CalibrePlayer(this, p)); }
    public CalibrePlayer removePlayerData(Player player) { return players.remove(player); }

    /**
     * Reloads settings from the settings file and applies some initial settings.
     * @return A {@link LoadResult} of messages during loading.
     */
    public DataResult<String, String> loadSettings() {
        try {
            settings.loadFrom(this);
        } catch (ResourceLoadException e) {
            DataResult<String, String> result = new DataResult<>();
            return result.addFailureData(TextUtils.format("Failed to load settings file: {msg}", "msg", e.getCause().getMessage()));
        }
        log(LogLevel.VERBOSE, "Loaded settings file");
        LogLevel level = LogLevel.valueOfDefault(setting("log_level", String.class, "verbose").toUpperCase());
        if (level != null) logger.setLevel(level);
        String defaultLocale = setting("locale", String.class, localeManager.getDefaultLocale());
        if (defaultLocale != null) localeManager.setDefaultLocale(defaultLocale);
        return new DataResult<>();
    }

    /**
     * Reloads languages and locales using {@link LocaleManager#loadFrom(Gson, Plugin)}.
     * @return A {@link LoadResult} of messages during loading.
     */
    public DataResult<String, String> loadLocales() {
        DataResult<String, String> result = new DataResult<>();
        try {
            localeManager.loadFrom(gson, this).forEach(entry -> {
                Path path = entry.getKey();
                if (entry.isSuccessful())
                    result.addSuccessData(TextUtils.format("Loaded translation {path} ({locale})", "path", path, "locale", ((Translation) entry.getResult()).getLocale()));
                else
                    result.addFailureData(TextUtils.format("Failed to load translation {path}: {msg}", "path", path, "msg", ((Exception) entry.getResult()).getMessage()));
            });
        } catch (ResourceLoadException e) {
            result.addFailureData(TextUtils.format("Failed to load locale files: {msg}", "msg", e.getMessage()));
        }
        return result;
    }

    /**
     * Reloads all the items in the plugin's {@link Registry} using {@link Registry#loadFrom(Gson, Plugin)}.
     * <p>
     * Configured subdirectories:
     * <ul>
     *     <li><code>component</code>: {@link CalibreComponent}</li>
     *     <li><code>blueprint</code>: {@link Blueprint}</li>
     * </ul>
     * @return A {@link LoadResult} of messages during loading.
     */
    public DataResult<String, String> loadRegistry() {
        DataResult<String, String> result = new DataResult<>();
        try {
            Registry.LoadContext load = registry.loadFrom(gson, this)
                    .with("component", CalibreComponent.class)
                    .with("blueprint", Blueprint.class)
                    .load()
                    .resolve();
            load.loadResult().forEach(entry -> {
                Path path = entry.getKey();
                if (entry.isSuccessful()) {
                    Identifiable id = (Identifiable) entry.getResult();
                    result.addSuccessData(TextUtils.format("Loaded {class} {id}", "class", id.getClass().getSimpleName(), "id", id.getId()));
                } else {
                    result.addFailureData(TextUtils.format("Failed to load {path}: {msg}", "path", path, "msg", ((Exception) entry.getResult()).getMessage()));
                }
            });
            load.resolveResult().forEach(entry -> {
                Identifiable id = entry.getKey().get();
                String type = id.getClass().getSimpleName();
                if (entry.isSuccessful()) {
                    result.addSuccessData(TextUtils.format("Resolved {class} {id}", "class", type, "id", id.getId()));
                } else {
                    result.addFailureData(TextUtils.format("Failed to resolve {class} {id}: {msg}", "class", type, "id", id.getId(), "msg", ((Exception) entry.getResult()).getMessage()));
                }
            });
        } catch (ResourceLoadException e) {
            result.addFailureData(TextUtils.format("Failed to load object files: {msg}", "msg", e.getMessage()));
        }
        return result;
    }

    /**
     * Checks if the {@link Plugin#getDataFolder()} exists, and runs:
     * <ol>
     *     <li>{@link CalibrePlugin#loadSettings()}</li>
     *     <li>{@link CalibrePlugin#loadLocales()}</li>
     *     <li>{@link CalibrePlugin#loadRegistry()}</li>
     * </ol>
     * <p>
     * This version does not take into account hooks. Use {@link CalibrePlugin#load()} to incorporate hooks.
     * @return A {@link LoadResult} of messages during loading.
     */
    public DataResult<String, String> cleanLoad() {
        DataResult<String, String> result = new DataResult<>();
        File root = getDataFolder();
        if (!root.exists()) {
            result.addFailureData("Failed to load data directory");
            return result;
        }

        return result
                .combine(loadSettings())
                .combine(loadLocales())
                .combine(loadRegistry());
    }

    /**
     * Clears the registry and locale manager, initializes hooks, then calls {@link CalibrePlugin#cleanLoad()}.
     * @return A {@link LoadResult} of messages during loading.
     */
    public DataResult<String, String> load() {
        localeManager.unregisterAll();
        registry.unregisterAll();

        hooks.forEach(hook -> hook.preLoadRegister(this, registry, localeManager, settings));
        DataResult<String, String> result = cleanLoad();
        hooks.forEach(hook -> hook.postLoadRegister(this, registry, localeManager, settings));
        return result;
    }

    @Override
    public void tick(TickContext tickContext) {
        Bukkit.getOnlinePlayers().forEach(player -> tickContext.tick(getPlayerData(player)));
    }

    /**
     * Gets a setting from the plugin's {@link Settings} instance.
     * @param path The path to the setting.
     * @param type The type of result.
     * @param defaultValue The default value if none were found in the settings file.
     * @param <T> The type of result.
     * @return The result.
     */
    public <T> T setting(String path, Type type, T defaultValue) { return settings.get(path, type, defaultValue); }

    /**
     * Gets a setting from the plugin's {@link Settings} instance.
     * @param path The path to the setting.
     * @param type The type of result.
     * @param defaultValue The default value if none were found in the settings file.
     * @param <T> The type of result.
     * @return The result.
     */
    public <T> T setting(String path, Class<T> type, T defaultValue) { return setting(path, (Type) type, defaultValue); }

    /**
     * Logs something to the plugin's {@link LabelledLogger}.
     * @param level The {@link LogLevel}.
     * @param text The text to log.
     * @param args The arguments used in {@link me.aecsocket.unifiedframework.util.TextUtils#format(String, Object...)}.
     */
    public void log(LogLevel level, String text, Object... args) { logger.log(level, text, args); }

    /**
     * Generates some localized text.
     * @param locale The locale to generate for.
     * @param key The key of the string.
     * @param args The arguments used in {@link me.aecsocket.unifiedframework.util.TextUtils#format(String, Object...)}.
     * @return The localized text.
     */
    public String gen(String locale, String key, Object... args) { return localeManager.gen(locale, key, args); }

    /**
     * Generates some localized text using the default locale.
     * @param key The key of the string.
     * @param args The arguments used in {@link me.aecsocket.unifiedframework.util.TextUtils#format(String, Object...)}.
     * @return The localized text.
     */
    public String gen(String key, Object... args) { return gen(localeManager.getDefaultLocale(), key, args); }

    /**
     * Generates some localized text for a {@link Player}'s locale.
     * @param player The player to get the locale from.
     * @param key The key of the string.
     * @param args The arguments used in {@link me.aecsocket.unifiedframework.util.TextUtils#format(String, Object...)}.
     * @return The localized text.
     */
    public String gen(Player player, String key, Object... args) { return gen(player.getLocale(), key, args); }

    /**
     * Generates some localized text for a {@link CommandSender}.
     * <p>
     * If this is a {@link Player}, {@link CalibrePlugin#gen(Player, String, Object...)} will be run. Otherwise, {@link CalibrePlugin#gen(String, Object...)} will be run.
     * @param sender The sender to generate for.
     * @param key The key of the string.
     * @param args The arguments used in {@link me.aecsocket.unifiedframework.util.TextUtils#format(String, Object...)}.
     * @return The localized text.
     */
    public String gen(CommandSender sender, String key, Object... args) { return sender instanceof Player ? gen((Player) sender, key, args) : gen(key, args); }

    /**
     * Gets the {@link CalibreItem} representation of an {@link ItemStack}.
     * @param stack The ItemStack.
     * @return The CalibreItem.
     */
    public CalibreItem getItem(ItemStack stack) { return itemManager.getItem(stack, CalibreItem.class); }

    /**
     * Gets the {@link T} representation of an {@link ItemStack}.
     * @param stack The ItemStack.
     * @return The {@link T}.
     */
    public <T extends CalibreItem> T getItem(ItemStack stack, Class<T> type) { return itemManager.getItem(stack, type); }
}
