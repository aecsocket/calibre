package com.gitlab.aecsocket.calibre.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.arguments.selector.MultiplePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.captions.FactoryDelegatingCaptionRegistry;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.types.tuples.Triplet;
import com.gitlab.aecsocket.calibre.core.util.*;
import com.gitlab.aecsocket.calibre.paper.util.CalibrePlayerData;
import com.gitlab.aecsocket.calibre.paper.util.CalibreProtocol;
import com.gitlab.aecsocket.calibre.paper.util.IdentifiableArgument;
import com.gitlab.aecsocket.calibre.paper.util.TreeArgument;
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
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.command.CommandSender;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class CalibreCommand {
    public static final String WILDCARD = "*";

    private final CalibrePlugin plugin;
    private MinecraftHelp<CommandSender> help;

    public CalibreCommand(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin plugin() { return plugin; }

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

    protected void register(PaperCommandManager<CommandSender> manager) {
        manager.registerBrigadier();
        manager.registerAsynchronousCompletions();
        FactoryDelegatingCaptionRegistry<CommandSender> captions = (FactoryDelegatingCaptionRegistry<CommandSender>) manager.getCaptionRegistry();
        captions.registerMessageFactory(IdentifiableArgument.ARGUMENT_PARSE_FAILURE_IDENTIFIABLE, (cap, sender) -> "'{input}' is not a valid identifiable of type {type}");
        captions.registerMessageFactory(TreeArgument.ARGUMENT_PARSE_FAILURE_TREE, (cap, sender) -> "'{input}' could not be parsed as a component tree: {error}");

        BukkitAudiences audiences = BukkitAudiences.create(plugin);
        help = new MinecraftHelp<>("/calibre help", audiences::sender, manager);

        Command.Builder<CommandSender> cmdRoot = manager.commandBuilder("calibre", ArgumentDescription.of("Calibre's main command."), "cal");

        manager.command(cmdRoot
                .literal("help", ArgumentDescription.of("Lists help information."))
                .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
                .handler(ctx -> help.queryCommands(ctx.getOrDefault("query", ""), ctx.getSender()))
        );
        manager.command(cmdRoot
                .literal("version", ArgumentDescription.of("Gets version information."))
                .handler(this::version)
        );
        manager.command(cmdRoot
                .literal("reload", ArgumentDescription.of("Reloads all plugin data."))
                .permission("calibre.command.reload")
                .handler(this::reload)
        );
        manager.command(cmdRoot
                .literal("list", ArgumentDescription.of("Lists all registered objects."))
                .argument(StringArgument.<CommandSender>newBuilder("id-filter").withSuggestionsProvider((ctx, arg) -> Collections.singletonList(WILDCARD)), ArgumentDescription.of("The filter for the ID."))
                .argument(StringArgument.<CommandSender>newBuilder("class-filter").withSuggestionsProvider((ctx, arg) -> Collections.singletonList(WILDCARD)), ArgumentDescription.of("The filter for the object class."))
                .argument(StringArgument.<CommandSender>newBuilder("name-filter").withSuggestionsProvider((ctx, arg) -> Collections.singletonList(WILDCARD)), ArgumentDescription.of("The filter for the object name."))
                .permission("calibre.command.list")
                .handler(this::list)
        );
        manager.command(cmdRoot
                .literal("info", ArgumentDescription.of("Displays detailed info for an object."))
                .argument(IdentifiableArgument.of("object", plugin), ArgumentDescription.of("The object to get info for."))
                .permission("calibre.command.info")
                .handler(this::info)
        );
        manager.command(cmdRoot
                .literal("give", ArgumentDescription.of("Gives a registered item to player(s)."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The player(s) to give to."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).build(), ArgumentDescription.of("The amount to give of."))
                .argument(IdentifiableArgument.<CommandSender>newBuilder("object", plugin).withTargetType(ItemSupplier.class).build(), ArgumentDescription.of("The object to give."))
                .permission("calibre.command.give")
                .handler(this::give)
        );
        manager.command(cmdRoot
                .literal("create", ArgumentDescription.of("Creates a component tree and gives it to player(s)."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The player(s) to give to."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).build(), ArgumentDescription.of("The amount to give of."))
                .argument(TreeArgument.of("object", plugin), ArgumentDescription.of("The tree to give."))
                .permission("calibre.command.create")
                .handler(this::create)
        );
        manager.command(cmdRoot
                .literal("tree", ArgumentDescription.of("Prints the formatted tree of the currently held item."))
                .senderType(Player.class)
                .permission("calibre.command.tree")
                .handler(this::tree)
        );
        manager.command(cmdRoot
                .literal("dump-tree", ArgumentDescription.of("Displays the raw Protocol Buffer bytes of the currently held item (does not have to be a valid component)."))
                .senderType(Player.class)
                .permission("calibre.command.dump-tree")
                .handler(this::dumpTree)
        );
        manager.command(cmdRoot
                .literal("zoom", ArgumentDescription.of("Sets your FOV zoom using Minecraft's algorithm."))
                .senderType(Player.class)
                .argument(DoubleArgument.of("zoom"), ArgumentDescription.of("The zoom amount."))
                .permission("calibre.command.zoom")
                .handler(this::zoom)
        );
        manager.command(cmdRoot
                .literal("offset", ArgumentDescription.of("Shows an offset from your camera, helpful for finding barrel offsets. Specify <0, 0, 0> to remove."))
                .senderType(Player.class)
                .argumentTriplet("offset", TypeToken.get(Vector3D.class),
                        Triplet.of("x", "y", "z"),
                        Triplet.of(double.class, double.class, double.class),
                        (s, d) -> new Vector3D(d.getFirst(), d.getSecond(), d.getThird()),
                        ArgumentDescription.of("The offset."))
                .permission("calibre.command.offset")
                .handler(this::offset)
        );
        manager.command(cmdRoot
                .literal("inaccuracy", ArgumentDescription.of("Toggles displaying current inaccuracy."))
                .senderType(Player.class)
                .permission("calibre.command.inaccuracy")
                .handler(this::inaccuracy)
        );
    }

    private void version(CommandContext<CommandSender> ctx) {
        CommandSender sender = ctx.getSender();
        PluginDescriptionFile desc = plugin.getDescription();
        send(sender, "command.version",
                "name", desc.getName(),
                "version", desc.getVersion(),
                "authors", String.join(", ", desc.getAuthors()));
    }

    private void reload(CommandContext<CommandSender> ctx) {
        CommandSender sender = ctx.getSender();
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
        return filter.equals(WILDCARD) ? null : filter.toLowerCase(Locale.ROOT);
    }

    private void list(CommandContext<CommandSender> ctx) {
        CommandSender sender = ctx.getSender();
        String idFilter = createFilter(ctx.getOrDefault("id-filter", WILDCARD));
        String classFilter = createFilter(ctx.getOrDefault("class-filter", WILDCARD));
        String nameFilter = createFilter(ctx.getOrDefault("name-filter", WILDCARD));

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

    public void info(CommandContext<CommandSender> ctx) {
        CommandSender sender = ctx.getSender();
        CalibreIdentifiable object = ctx.get("object");
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

    private void internalGive(CommandSender sender, MultiplePlayerSelector selector, ItemSupplier<BukkitItem> item, Integer amount) {
        if (amount == null) amount = 1;
        if (amount < 1) {
            send(sender, "command.error.less_than",
                    "minimum", "1");
            return;
        }

        for (Player target : selector.getPlayers()) {
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

    public void give(CommandContext<CommandSender> ctx) {
        internalGive(ctx.getSender(), ctx.get("targets"), ctx.get("object"), ctx.get("amount"));
    }

    public void create(CommandContext<CommandSender> ctx) {
        internalGive(ctx.getSender(), ctx.get("targets"), ((ComponentTree) ctx.get("object")).<PaperComponent>root(), ctx.get("amount"));
    }

    public void tree(CommandContext<CommandSender> ctx) {
        Player player = (Player) ctx.getSender();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (BukkitUtils.empty(hand)) {
            send(player, "command.error.no_held_item");
            return;
        }

        Locale locale = locale(player);
        try {
            PaperComponent component = plugin.itemManager().get(player.getInventory().getItemInMainHand());
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
            send(player, "command.object",
                    "type", component.getClass().getSimpleName(),
                    "id", component.id(),
                    "name", component.name(locale));
            for (String line : lines)
                send(player, "command.tree.line",
                        "line", line);
        } catch (ComponentCreationException | ConfigurateException e) {
            sendError(player, e, "command.error.get_item",
                    "error", TextUtils.combineMessages(e));
        }
    }

    public void dumpTree(CommandContext<CommandSender> ctx) {
        Player player = (Player) ctx.getSender();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (BukkitUtils.empty(hand)) {
            send(player, "command.error.no_held_item");
            return;
        }

        Component spacing = gen(player, "command.dump_tree.spacing");

        long start = System.nanoTime();
        byte[] bytes = plugin.itemManager().tree(player.getInventory().getItemInMainHand());
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
            send(player, "command.dump_tree.line",
                    "index", String.format("%08x", lineIndex),
                    "bytes", line.build());
            lineIndex += lineLength;
        }
        send(player, "command.dump_tree.footer",
                "size_int", String.format("%,d", bytes.length),
                "size_hex", String.format("%x", bytes.length),
                "time", String.format("%,d", time));
    }

    public void zoom(CommandContext<CommandSender> ctx) {
        CalibreProtocol.fov((Player) ctx.getSender(), ctx.get("zoom"));
    }

    public void offset(CommandContext<CommandSender> ctx) {
        Vector3D offset = ctx.get("offset");
        plugin.playerData((Player) ctx.getSender()).offset(
                offset.x() == 0 && offset.y() == 0 && offset.z() == 0
                        ? null
                        : offset
        );
    }

    public void inaccuracy(CommandContext<CommandSender> ctx) {
        CalibrePlayerData data = plugin.playerData((Player) ctx.getSender());
        data.showInaccuracy(!data.showInaccuracy());
    }
}
