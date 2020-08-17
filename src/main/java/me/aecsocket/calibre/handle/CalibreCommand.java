package me.aecsocket.calibre.handle;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * The plugin's command handler. Uses the ACF framework.
 */
@CommandAlias("calibre|cal")
public class CalibreCommand extends BaseCommand {
    private final CalibrePlugin plugin;

    public CalibreCommand(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    private void send(CommandSender sender, String key, Object... args) { sender.sendMessage(plugin.gen(sender, key, args)); }

    @Subcommand("version|ver")
    public void version(CommandSender sender) {
        PluginDescriptionFile description = plugin.getDescription();
        send(sender, "chat.command.version", "name", description.getName(), "version", description.getVersion(), "authors", String.join(", ",description.getAuthors()));
    }

    @Subcommand("reload")
    @CommandPermission("calibre.command.reload")
    public void reload(CommandSender sender) {
        send(sender, "chat.command.reload.progress");
        int[] warnings = {0};
        plugin.load().forEach(entry -> {
            plugin.log(entry.isSuccessful() ? LogLevel.VERBOSE : LogLevel.WARN, (String) entry.getData());
            if (!entry.isSuccessful()) {
                send(sender, "chat.command.reload.warning", "msg", entry.getData());
                ++warnings[0];
            }
        });
        if (warnings[0] == 0)
            send(sender, "chat.command.reload.complete");
        else
            send(sender, "chat.command.reload.complete.warnings", "warnings", warnings[0]);
    }
}
