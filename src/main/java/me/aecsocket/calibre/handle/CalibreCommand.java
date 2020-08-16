package me.aecsocket.calibre.handle;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import me.aecsocket.calibre.CalibrePlugin;
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
}
