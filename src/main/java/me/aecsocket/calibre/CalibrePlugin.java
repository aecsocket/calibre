package me.aecsocket.calibre;

import co.aikar.commands.PaperCommandManager;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.aecsocket.calibre.handle.CalibreCommand;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.locale.Translation;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.ResourceLoadException;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.util.log.LabelledLogger;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main plugin class. Other plugins can hook into this by:
 * <ul>
 *     <li>Adding this plugin as a <code>loadbefore</code> in the <code>plugin.yml</code></li>
 *     <li>Extending {@link CalibreHook}</li>
 * </ul>
 */
public class CalibrePlugin extends JavaPlugin {
    private final LabelledLogger logger = new LabelledLogger(getLogger(), LogLevel.VERBOSE);
    private final Settings settings = new Settings();
    private final LocaleManager localeManager = new LocaleManager();
    private final Registry registry = new Registry();
    private PaperCommandManager commandManager;
    private Gson gson;

    @Override
    public void onEnable() {
        commandManager = new PaperCommandManager(this);

        String pluginName = getDescription().getName();
        List<CalibreHook> hooks = Stream.of(Bukkit.getPluginManager().getPlugins())
                .filter(plugin -> plugin instanceof CalibreHook && plugin.getDescription().getLoadBefore().contains(pluginName))
                .map(plugin -> (CalibreHook) plugin)
                .collect(Collectors.toList());

        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        hooks.forEach(hook -> hook.initializeGson(gsonBuilder));
        gson = gsonBuilder.create();

        load();

        commandManager.registerCommand(new CalibreCommand(this));
    }

    public LabelledLogger getPluginLogger() { return logger; }
    public Settings getSettings() { return settings; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public Registry getRegistry() { return registry; }
    public PaperCommandManager getCommandManager() { return commandManager; }
    public Gson getGson() { return gson; }

    /**
     * Reloads settings from the settings file and applies some initial settings.
     */
    public void loadSettings() {
        try {
            settings.loadFrom(this);
        } catch (ResourceLoadException e) {
            log(LogLevel.WARN, "Failed to load settings file: {msg}", "msg", e.getCause().getMessage());
            return;
        }
        log(LogLevel.VERBOSE, "Loaded settings file");
        LogLevel level = LogLevel.valueOfDefault(setting("log_level", String.class, "verbose").toUpperCase());
        if (level != null) logger.setLevel(level);
        String defaultLocale = setting("locale", String.class, localeManager.getDefaultLocale());
        if (defaultLocale != null) localeManager.setDefaultLocale(defaultLocale);
    }

    /**
     * Reloads languages and locales using {@link LocaleManager#loadFrom(Gson, Plugin)}.
     */
    public void loadLocales() {
        localeManager.unregisterAll();
        try {
            localeManager.loadFrom(gson, this).forEach(entry -> {
                Path path = entry.getPath();
                if (entry.isSuccessful())
                    log(LogLevel.VERBOSE, "Loaded translation {path} ({locale})", "path", path, "locale", ((Translation) entry.getData()).getLocale());
                else
                    log(LogLevel.WARN, "Failed to load translation {path}: {msg}", "path", path, "msg", ((Exception) entry.getData()).getMessage());
            });
        } catch (ResourceLoadException e) {
            log(LogLevel.WARN, "Failed to load locale files: {msg}", "msg", e.getMessage());
        }
    }

    /**
     * Reloads all the items in the plugin's {@link Registry} using {@link Registry#loadFrom(Gson, Plugin)}.
     * <p>
     * There are no configured subdirectories currently.
     */
    public void loadRegistry() {
        registry.unregisterAll();
        try {
            registry.loadFrom(gson, this)
                    //.with("component", CalibreComponent.class)
                    //.with("blueprint", Blueprint.class)
                    .load()
                    .forEach(entry -> {
                        Path path = entry.getPath();
                        if (entry.isSuccessful()) {
                            Identifiable id = (Identifiable) entry.getData();
                            log(LogLevel.VERBOSE, "Loaded {class} {id}", "class", id.getClass().getSimpleName(), "id", id.getId());
                        } else
                            log(LogLevel.WARN, "Failed to load {path}: {msg}", "path", path, "msg", ((Exception) entry.getData()).getMessage());
                    });
        } catch (ResourceLoadException e) {
            log(LogLevel.WARN, "Failed to load object files: {msg}", "msg", e.getMessage());
        }
    }

    /**
     * Checks if the {@link Plugin#getDataFolder()} exists, and runs:
     * <ol>
     *     <li>{@link CalibrePlugin#loadSettings()}</li>
     *     <li>{@link CalibrePlugin#loadLocales()}</li>
     *     <li>{@link CalibrePlugin#loadRegistry()}</li>
     * </ol>
     */
    public void load() {
        File root = getDataFolder();
        if (!root.exists()) {
            log(LogLevel.ERROR, "Failed to load data directory");
            return;
        }

        loadSettings();
        loadLocales();
        loadRegistry();
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
}
