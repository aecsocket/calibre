package com.github.aecsocket.calibre.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.FloatArgument;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/* package */ class CalibreCommand extends BaseCommand<CalibrePlugin> {
    public CalibreCommand(CalibrePlugin plugin) throws Exception {
        super(plugin, "calibre",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command."), "cal"));

        manager.command(root
                .literal("zoom", ArgumentDescription.of("Displays a zoom level, that can then be used in the configs."))
                .argument(FloatArgument.optional("zoom", 0.1f), ArgumentDescription.of("The amount of zoom. Default = 0.1, " +
                        "higher number = higher zoom. If zoom < 0, the closer to -0.125, the more zoom."))
                .senderType(Player.class)
                .permission("%s.command.zoom".formatted(rootName))
                .handler(c -> handle(c, this::zoom)));

        var rootOffset = root
                .literal("offset", ArgumentDescription.of("Toggles showing a camera offset, that can be used in the configs."));
        manager.command(rootOffset
                .literal("set", ArgumentDescription.of("Displays a camera offset, that can be used in the configs."))
                .argument(DoubleArgument.of("x"), ArgumentDescription.of("The X coordinate of the offset."))
                .argument(DoubleArgument.of("y"), ArgumentDescription.of("The X coordinate of the offset."))
                .argument(DoubleArgument.of("z"), ArgumentDescription.of("The X coordinate of the offset."))
                .senderType(Player.class)
                .permission("%s.command.offset.set".formatted(rootName))
                .handler(c -> handle(c, this::offsetSet)));
        manager.command(rootOffset
                .literal("unset", ArgumentDescription.of("Removes the camera offset display."))
                .senderType(Player.class)
                .permission("%s.command.offset.unset".formatted(rootName))
                .handler(c -> handle(c, this::offsetUnset)));
    }

    private void zoom(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        plugin.zoom(pSender, ctx.get("zoom"));
    }

    private void offsetSet(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        plugin.playerData(pSender).displayOffset(Vector3.vec3(ctx.get("x"), ctx.get("y"), ctx.get("z")));
    }

    private void offsetUnset(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        plugin.playerData(pSender).displayOffset(null);
    }
}
