package me.aecsocket.calibre.handle;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreIdentifiable;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * The plugin's command handler. Uses the ACF framework.
 */
@CommandAlias("calibre|cal")
public class CalibreCommand extends BaseCommand {
    public static final String WILDCARD = "*";

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
            plugin.log(entry.isSuccessful() ? LogLevel.VERBOSE : LogLevel.WARN, (String) entry.getResult());
            if (!entry.isSuccessful()) {
                send(sender, "chat.command.reload.warning", "msg", entry.getResult());
                ++warnings[0];
            }
        });
        if (warnings[0] == 0)
            send(sender, "chat.command.reload.complete");
        else
            send(sender, "chat.command.reload.complete.warnings", "warnings", warnings[0]);
    }

    @Subcommand("list")
    @CommandPermission("calibre.command.list")
    @CommandCompletion("@registry:extends=me.aecsocket.calibre.item.CalibreIdentifiable")
    public void list(CommandSender sender, @Optional String nameFilter, @Optional String typeFilter) {
        if (WILDCARD.equals(nameFilter)) nameFilter = null;
        else if (nameFilter != null) nameFilter = nameFilter.toLowerCase();
        if (WILDCARD.equals(typeFilter)) typeFilter = null;
        else if (typeFilter != null) typeFilter = typeFilter.toLowerCase();
        final String fNameFilter = nameFilter;
        final String fTypeFilter = typeFilter;
        int[] results = {0};
        plugin.getRegistry().getRegistry().forEach((id, ref) -> {
            if (!(ref.get() instanceof CalibreIdentifiable)) return;
            CalibreIdentifiable object = (CalibreIdentifiable) ref.get();
            String type = object.getClass().getSimpleName();
            String localizedName = object instanceof CalibreItem ? ((CalibreItem) object).getLocalizedName(sender) : null;
            if (fTypeFilter != null && !type.toLowerCase().contains(fTypeFilter)) return;
            if (fNameFilter != null) {
                boolean pass = false;
                if (id.contains(fNameFilter)) pass = true;
                else if (object instanceof CalibreItem && ChatColor.stripColor(localizedName).toLowerCase().contains(fNameFilter))
                    pass = true;
                if (!pass) return;
            }
            String info = object.getShortInfo(sender);
            if (info == null)
                send(sender, "chat.command.list.no_info", "class", type, "id", id);
            else
                send(sender, "chat.command.list.info", "class", type, "id", id, "info", info);
            ++results[0];
        });
        if (results[0] == 0)
            send(sender, "chat.command.list.no_results");
    }

    @Subcommand("info")
    @CommandPermission("calibre.command.info")
    @CommandCompletion("@registry")
    public void info(CommandSender sender, CalibreIdentifiable object) {
        String info = object.getLongInfo(sender);
        if (info == null) {
            send(sender, "chat.command.info.no_info");
            return;
        }
        String[] lines = info.split("\n");
        for (String line : lines)
            send(sender, "chat.command.info", "line", line);
    }

    @Subcommand("give")
    @CommandPermission("calibre.command.give")
    @CommandCompletion("@players @registry:extends=me.aecsocket.calibre.item.CalibreItem")
    public void give(CommandSender sender, Player target, CalibreItem item, @Optional Integer amount) {
        if (amount == null) amount = 1;
        ItemStack stack;
        try {
            stack = item.createItem(target);
        } catch (ItemCreationException e) {
            send(sender, "chat.command.give.error", "id", item.getId(), "msg", e.getMessage());
            return;
        }
        for (int i = 0; i < amount; i++)
            Utils.giveItem(target, stack);
        send(sender, "chat.command.give", "amount", amount, "item", item.getLocalizedName(sender), "target", target.getDisplayName());
    }
}
