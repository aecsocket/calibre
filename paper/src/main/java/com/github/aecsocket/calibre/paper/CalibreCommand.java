package com.github.aecsocket.calibre.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.bukkit.parsers.location.LocationArgument;
import cloud.commandframework.context.CommandContext;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/* package */ final class CalibreCommand extends BaseCommand<CalibrePlugin> {
    public CalibreCommand(CalibrePlugin plugin) throws Exception {
        super(plugin, "calibre",
            (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command."), "cal"));

        manager.command(root
            .literal("explode", ArgumentDescription.of("Spawns an explosion with defined options."))
            .argument(LocationArgument.of("location"), ArgumentDescription.of("The location to spawn at."))
            .argument(DoubleArgument.of("power"), ArgumentDescription.of("The explosion power."))
            .permission(permission("explode"))
            .handler(c -> handle(c, this::explode)));
    }

    private void explode(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        Location location = ctx.get("location");
        double power = ctx.get("power");

        Explosion explosion = Explosion.explosion(plugin, plugin.explosionOptions(), power, 0);
        explosion.spawn(location, pSender);

        sender.sendMessage(explosion.damage(Explosion.distance(pSender, location))+"");
    }
}
