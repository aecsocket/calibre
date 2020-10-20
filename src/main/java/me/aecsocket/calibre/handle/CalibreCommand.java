package me.aecsocket.calibre.handle;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.util.CalibreIdentifiable;
import me.aecsocket.calibre.util.LogMessageResult;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.item.ItemStackFactory;
import me.aecsocket.unifiedframework.registry.Ref;
import me.aecsocket.unifiedframework.util.GraphUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * The plugin's main command class.
 */
@CommandAlias("calibre|cal")
public class CalibreCommand extends BaseCommand {
    public static final String WILDCARD = "*";

    private final CalibrePlugin plugin;

    public CalibreCommand(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    private void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(plugin.gen(sender, key, args));
    }

    @CatchUnknown
    public void unknown(CommandSender sender) {
        send(sender, "chat.command.unknown");
    }

    @HelpCommand
    @Syntax("")
    @Description("Displays help for the command usage.")
    public void help(CommandSender sender, CommandHelp help) {
        send(sender, "chat.command.help");
        help.getHelpEntries().forEach(entry -> send(sender, "chat.command.help.entry",
                "cmd", entry.getCommand(), "params", entry.getParameterSyntax(), "desc", entry.getDescription()));
    }

    @Subcommand("version|ver")
    @Description("Displays info about the Calibre plugin.")
    public void version(CommandSender sender) {
        PluginDescriptionFile desc = plugin.getDescription();
        send(sender, "chat.command.version",
                "name", desc.getName(), "version", desc.getVersion(), "authors", String.join(", ", desc.getAuthors()));
    }

    @Subcommand("reload")
    @Description("Reloads all data for the plugin.")
    @CommandPermission("calibre.command.reload")
    public void reload(CommandSender sender) {
        send(sender, "chat.command.reload.progress");
        int[] warnings = {0};
        plugin.logReload().forEach(entry -> {
            if (!entry.isSuccessful()) {
                send(sender, "chat.command.reload.warning",
                        "msg", ((LogMessageResult.Message) entry.getResult()).getMessage());
                ++warnings[0];
            }
        });
        if (warnings[0] == 0)
            send(sender, "chat.command.reload.complete");
        else
            send(sender, "chat.command.reload.complete.warnings",
                    "warnings", warnings[0]);
    }

    @Subcommand("list")
    @Description("Lists all registered objects.")
    @CommandPermission("calibre.command.list")
    @CommandCompletion("[id-filter]|* [class-filter]|* [info-filter]|*")
    public void list(CommandSender sender, @Optional String idFilter, @Optional String classFilter, @Optional String infoFilter) {
        String locale = plugin.locale(sender);
        if (idFilter != null && idFilter.equals(WILDCARD)) idFilter = null;
        if (classFilter != null && classFilter.equals(WILDCARD)) classFilter = null;
        if (infoFilter != null && infoFilter.equals(WILDCARD)) infoFilter = null;

        int results = 0;
        for (Ref<CalibreIdentifiable> ref : plugin.getRegistry().getRegistry().values()) {
            CalibreIdentifiable object = ref.get();
            Class<? extends CalibreIdentifiable> clazz = object.getClass();
            if (idFilter != null && !object.getId().contains(idFilter)) continue;
            if (classFilter != null && !object.getClass().getName().contains(classFilter)) continue;
            if (infoFilter != null && !object.getShortInfo(locale).contains(infoFilter)) continue;

            send(sender, "chat.command.list.line",
                    "class", clazz.getSimpleName(),
                    "id", object.getId(),
                    "info", object.getShortInfo(locale));
            ++results;
        }

        if (results > 0)
            send(sender, "chat.command.list.results",
                    "results", results);
        else
            send(sender, "chat.command.list.no_results");
    }

    @Subcommand("info")
    @Description("Shows extensive info for a registered object.")
    @CommandPermission("calibre.command.info")
    @CommandCompletion("@registry")
    public void info(CommandSender sender, Ref<CalibreIdentifiable> item) {
        send(sender, "chat.command.info",
                "id", item.getId());
        for (String line : item.get().getLongInfo(plugin.locale(sender)).split("\n")) {
            send(sender, "chat.command.info.line",
                    "line", line);
        }
    }

    @Subcommand("resolution-order")
    @Description("Displays the full topologically sorted dependency graph of the registry.")
    @CommandPermission("calibre.command.resolution-order")
    @SuppressWarnings("UnstableApiUsage")
    public void resolutionOrder(CommandSender sender) {
        for (Ref<CalibreIdentifiable> node : GraphUtils.topologicallySortedNodes(plugin.getRegistry().getDependencyGraph())) {
            CalibreIdentifiable object = node.get();
            send(sender, "chat.command.list.info",
                    "class", object.getClass().getSimpleName(),
                    "id", object.getId(),
                    "info", object.getShortInfo(plugin.locale(sender)));
        }
    }

    @Subcommand("tree")
    @Description("Gets the JSON representation of the component tree of the currently held item.")
    @CommandPermission("calibre.command.tree")
    public void tree(CommandSender sender) {
        if (!(sender instanceof Player)) {
            send(sender, "chat.command.not_player");
            return;
        }

        ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
        CalibreComponent component = plugin.fromItem(item);
        if (component == null) {
            send(sender, "chat.command.tree.not_item");
            return;
        }
        send(sender, "chat.command.tree",
                "id", component.getId(),
                "info", component.getShortInfo(plugin.locale(sender)));
        Gson gson = plugin.prettyPrinter();
        for (String line : gson.toJson(component.getTree()).split("\n"))
            send(sender, "chat.command.tree.line",
                    "line", line);
    }

    private <T extends CalibreIdentifiable & ItemStackFactory> void give(CommandSender sender, Player target, T item, Integer amount) {
        int rAmount = amount == null ? 1 : amount;
        ItemStack stack;
        try {
            stack = item.createItem(target);
        } catch (ItemCreationException e) {
            send(sender, "chat.command.give.error",
                    "id", item.getId(),
                    "msg", e.getMessage());
            return;
        }
        for (int i = 0; i < rAmount; i++)
            target.getInventory().addItem(stack);
        send( sender, "chat.command.give",
                "amount", rAmount,
                "item", item.getLocalizedName(sender),
                "target", target.getDisplayName());
    }

    @Subcommand("give")
    @Description("Gives an item to a player.")
    @CommandPermission("calibre.command.give")
    @CommandCompletion("@players @registry:type=me.aecsocket.calibre.unifiedframework.item.ItemStackFactory [amount]")
    public <T extends CalibreIdentifiable & ItemStackFactory> void give(CommandSender sender, Player target, @Flags("type=me.aecsocket.calibre.unifiedframework.item.ItemStackFactory") Ref<T> item, @Optional Integer amount) {
        give(sender, target, item.get(), amount);
    }

    @Subcommand("create")
    @Description("Creates a component tree and gives that to a player.")
    @CommandPermission("calibre.command.create")
    @CommandCompletion("@players <json> [amount]")
    public void create(CommandSender sender, Player target, String json, @Optional Integer amount) {
        CalibreComponent component;
        try {
            component = plugin.getGson().fromJson(json, ComponentTree.class).getRoot();
        } catch (JsonParseException e) {
            send(sender, "chat.command.create.error.json",
                    "msg", e.getMessage());
            return;
        }

        give(sender, target, component, amount);
    }
}
