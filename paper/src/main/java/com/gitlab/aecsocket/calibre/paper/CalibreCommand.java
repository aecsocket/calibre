package com.gitlab.aecsocket.calibre.paper;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.gitlab.aecsocket.calibre.core.util.*;
import com.gitlab.aecsocket.calibre.paper.util.CalibrePlayerData;
import com.gitlab.aecsocket.calibre.paper.util.CalibreProtocol;
import com.gitlab.aecsocket.calibre.paper.util.item.ComponentCreationException;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.core.registry.Ref;
import com.gitlab.aecsocket.unifiedframework.core.util.result.LoggingEntry;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import com.gitlab.aecsocket.unifiedframework.core.util.TextUtils;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LogLevel;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("calibre|cal")
public class CalibreCommand extends BaseCommand {
    public static final String WILDCARD = "*";

    private final CalibrePlugin plugin;

    public CalibreCommand(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    private Locale locale(CommandSender sender) { return plugin.locale(sender); }

    private Component gen(CommandSender sender, String key, Object... args) {
        return plugin.gen(locale(sender), key, args);
    }

    private void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(gen(sender, key, args));
    }

    private void sendError(CommandSender sender, Throwable e, String key, Object... args) {
        send(sender, key, args);
        plugin.log(LogLevel.WARN, e, e.getMessage());
    }

    @CatchUnknown
    public void unknown(CommandSender sender) {
        send(sender, "command.unknown");
    }

    @HelpCommand
    @Description("Displays help for command usage.")
    @Syntax("")
    public void help(CommandSender sender, CommandHelp help) {
        send(sender, "command.help.header");
        help.getHelpEntries().forEach(entry -> send(sender, "command.help.entry",
                "command", entry.getCommand(),
                "syntax", entry.getParameterSyntax(),
                "description", entry.getDescription()));
    }

    @Subcommand("version|ver")
    @Description("Displays version info for Calibre.")
    public void version(CommandSender sender) {
        PluginDescriptionFile description = plugin.getDescription();
        send(sender, "command.version",
                "name", description.getName(),
                "version", description.getVersion(),
                "authors", String.join(", ", description.getAuthors()));
    }

    @Subcommand("reload")
    @Description("Reloads all plugin data.")
    @CommandPermission("calibre.command.reload")
    public void reload(CommandSender sender) {
        send(sender, "command.reload.start");
        AtomicInteger warnings = new AtomicInteger(0);
        List<LoggingEntry> result = new ArrayList<>();
        plugin.load(result);
        plugin.log(result).forEach(entry -> {
            if (entry.level().level() >= LogLevel.WARN.level()) {
                warnings.incrementAndGet();
                for (String line : TextUtils.lines(entry.infoBasic())) {
                    send(sender, "command.reload.warning",
                            "message", line);
                }
            }
        });
        if (warnings.get() == 0)
            send(sender, "command.reload.no_warnings");
        else
            send(sender, "command.reload.warnings",
                    "warnings", Integer.toString(warnings.get()));
    }

    private String createFilter(String filter) {
        if (filter != null) {
            if (filter.equals(WILDCARD)) return null;
            else return filter.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    @Subcommand("list")
    @Description("Lists all registered objects.")
    @CommandPermission("calibre.command.list")
    @CommandCompletion("[id-filter]|* [class-filter]|* [name-filter]|*")
    @Syntax("[id-filter]|* [class-filter]|* [name-filter]|*")
    public void list(CommandSender sender, @Optional String idFilter, @Optional String classFilter, @Optional String nameFilter) {
        idFilter = createFilter(idFilter);
        classFilter = createFilter(classFilter);
        nameFilter = createFilter(nameFilter);

        Locale locale = locale(sender);
        int results = 0;
        for (Ref<CalibreIdentifiable> ref : plugin.registry().getRegistry().values()) {
            CalibreIdentifiable object = ref.get();
            Class<? extends CalibreIdentifiable> type = object.getClass();
            if (idFilter != null && !object.id().toLowerCase(Locale.ROOT).contains(idFilter)) continue;
            if (classFilter != null && !type.getName().toLowerCase(Locale.ROOT).contains(classFilter)) continue;
            if (
                    nameFilter != null
                    && !PlainComponentSerializer.plain().serialize(object.name(locale)).toLowerCase(Locale.ROOT).contains(nameFilter)
            ) continue;

            send(sender, "command.object",
                    "type", type.getSimpleName(),
                    "id", object.id(),
                    "name", object.name(locale));
            ++results;
        }

        if (results > 0)
            send(sender, "command.list.results",
                    "results", Integer.toString(results));
        else
            send(sender, "command.list.no_results");
    }

    @Subcommand("info")
    @Description("Displays detailed info for an object.")
    @CommandPermission("calibre.command.info")
    @CommandCompletion("@registry")
    @Syntax("<id>")
    public void info(CommandSender sender, CalibreIdentifiable object) {
        Locale locale = locale(sender);
        send(sender, "command.object",
                "type", object.getClass().getSimpleName(),
                "id", object.id(),
                "name", object.name(locale));
        Component[] info = object.info(locale);
        if (info == null)
            send(sender, "command.info.no_info");
        else {
            for (Component line : info) {
                send(sender, "command.info.info",
                        "info", line);
            }
        }
    }

    private void internalGive(CommandSender sender, String selector, ItemSupplier<BukkitItem> item, Integer amount) {
        if (amount == null) amount = 1;
        if (amount < 1) {
            send(sender, "command.error.less_than",
                    "minimum", "1");
            return;
        }

        List<Entity> targets;
        try {
            targets = Bukkit.selectEntities(sender, selector);
        } catch (IllegalArgumentException e) {
            sendError(sender, e, "command.error.selector",
                    "error", TextUtils.combineMessages(e));
            return;
        }
        if (targets.size() == 0) {
            send(sender, "command.error.no_targets");
            return;
        }

        for (Entity entity : targets) {
            if (!(entity instanceof Player)) {
                send(sender, "command.error.not_player",
                        "name", entity.getName());
                continue;
            }

            Player target = (Player) entity;
            Locale locale = target.locale();

            BukkitItem wrapCreated;
            try {
                wrapCreated = item.create(locale);
            } catch (ItemCreationException e) {
                sendError(sender, e, "command.error.create_item",
                        "error", TextUtils.combineMessages(e));
                continue;
            }
            ItemStack created = wrapCreated.item();

            for (int i = 0; i < amount; i++)
                target.getInventory().addItem(created);
            send(sender, "command.give.success",
                    "target", target.getName(),
                    "amount", Integer.toString(amount),
                    "name", item.name(locale));
        }
    }

    @Subcommand("give")
    @Description("Gives a registered item to player targets.")
    @CommandPermission("calibre.command.give")
    @CommandCompletion("<selector> @registry:type=me.aecsocket.calibre.util.ItemSupplier [amount]")
    @Syntax("<selector> <item> [amount]")
    public void create(CommandSender sender, String selector, CalibreIdentifiable item, @Optional Integer amount) {
        @SuppressWarnings("unchecked")
        ItemSupplier<BukkitItem> supplier = (ItemSupplier<BukkitItem>) item;
        internalGive(sender, selector, supplier, amount);
    }

    @Subcommand("create")
    @Description("Creates a component tree and gives it to a player.")
    @CommandPermission("calibre.command.create")
    @CommandCompletion("<selector> @registry:type=me.aecsocket.calibre.component.Component [amount]")
    @Syntax("<selector> <tree> [amount]")
    public void create(CommandSender sender, String selector, ComponentTree tree, @Optional Integer amount) {
        internalGive(sender, selector, tree.<PaperComponent>root(), amount);
    }

    @Subcommand("tree")
    @Description("Displays the component tree of the currently held item.")
    @CommandPermission("calibre.command.tree")
    public void tree(CommandSender sender) {
        if (!(sender instanceof Player)) {
            send(sender, "command.error.sender_not_player");
            return;
        }

        ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
        if (BukkitUtils.empty(hand)) {
            send(sender, "command.error.no_held_item");
            return;
        }

        Locale locale = locale(sender);
        try {
            PaperComponent component = plugin.itemManager().get(((Player) sender).getInventory().getItemInMainHand());
            ComponentTree tree = component.tree();

            // Save into pretty printed HOCON
            StringWriter writer = new StringWriter();
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .sink(() -> new BufferedWriter(writer))
                    .prettyPrinting(true)
                    .build();
            ConfigurationNode node = BasicConfigurationNode.root(plugin.configOptions()).set(tree);
            if (!node.isMap())
                node.node("id").set(component.id());
            loader.save(node);

            // Print
            String[] lines = writer.toString().split("\n");
            send(sender, "command.object",
                    "type", component.getClass().getSimpleName(),
                    "id", component.id(),
                    "name", component.name(locale));
            for (String line : lines)
                send(sender, "command.tree.line",
                        "line", line);
        } catch (ComponentCreationException | ConfigurateException e) {
            sendError(sender, e, "command.error.get_item",
                    "error", TextUtils.combineMessages(e));
        }
    }

    @Subcommand("dump-tree")
    @Description("Displays the raw Protocol Buffer bytes of the currently held item (does not have to be a valid component)")
    @CommandPermission("calibre.command.dump-tree")
    public void dumpTree(CommandSender sender) {
        if (!(sender instanceof Player)) {
            send(sender, "command.error.sender_not_player");
            return;
        }

        ItemStack hand = ((Player) sender).getInventory().getItemInMainHand();
        if (BukkitUtils.empty(hand)) {
            send(sender, "command.error.no_held_item");
            return;
        }

        Component spacing = gen(sender, "command.dump_tree.spacing");

        long start = System.nanoTime();
        byte[] bytes = plugin.itemManager().tree(((Player) sender).getInventory().getItemInMainHand());
        long time = System.nanoTime() - start;

        int lineIndex = 0;
        int lineLength = 16;
        while (lineIndex < bytes.length) {
            TextComponent.Builder line = Component.text();
            for (int i = 0; i < lineLength; i++) {
                int idx = lineIndex + i;
                Component toWrite;
                if (idx >= bytes.length)
                    break;
                toWrite = Component.text(String.format("%02x", bytes[idx]));
                line.append(Component.text()
                        .append(toWrite)
                        .append(spacing)
                );
            }
            send(sender, "command.dump_tree.line",
                    "index", String.format("%08x", lineIndex),
                    "bytes", line.build());
            lineIndex += lineLength;
        }
        send(sender, "command.dump_tree.footer",
                "size_int", String.format("%,d", bytes.length),
                "size_hex", String.format("%x", bytes.length),
                "time", String.format("%,d", time));
    }

    @Subcommand("zoom")
    @Description("Sets your FOV zoom using Minecraft's formula")
    @CommandPermission("calibre.command.zoom")
    @CommandCompletion("<zoom>")
    @Syntax("<zoom>")
    public void zoom(CommandSender sender, double zoom) {
        if (!(sender instanceof Player)) {
            send(sender, "command.error.sender_not_player");
            return;
        }

        CalibreProtocol.fov((Player) sender, zoom);
    }

    @Subcommand("offset")
    @Description("Shows an offset from your camera, helpful for finding barrel offsets")
    @CommandPermission("calibre.command.offset")
    @CommandCompletion("<x> <y> <z>")
    @Syntax("<x> <y> <z>")
    public void offset(CommandSender sender, @Optional Double x, @Optional Double y, @Optional Double z) {
        if (!(sender instanceof Player)) {
            send(sender, "command.error.sender_not_player");
            return;
        }

        Player player = (Player) sender;
        if (x == null || y == null || z == null)
            plugin.playerData(player).offset(null);
        else
            plugin.playerData(player).offset(new Vector3D(x, y, z));
    }

    @Subcommand("inaccuracy")
    @Description("Toggles showing your inaccuracy in the subtitle")
    @CommandPermission("calibre.command.inaccuracy")
    public void inaccuracy(CommandSender sender) {
        if (!(sender instanceof Player)) {
            send(sender, "command.error.sender_not_player");
            return;
        }

        CalibrePlayerData data = plugin.playerData((Player) sender);
        data.showInaccuracy(!data.showInaccuracy());
    }
}
