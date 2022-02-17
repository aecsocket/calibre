package com.github.aecsocket.calibre.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.bukkit.parsers.location.LocationArgument;
import cloud.commandframework.context.CommandContext;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static net.kyori.adventure.text.Component.*;
import static com.github.aecsocket.calibre.paper.CalibrePlugin.format2;

/* package */ final class CalibreCommand extends BaseCommand<CalibrePlugin> {
    record ExplosionInfo(Explosions.Instance explosion, Location location) {}

    public static final String
        LOCATION = "location",
        COMMAND_EXPL_SPAWN_GENERIC = "command.expl.spawn.generic",
        COMMAND_EXPL_SPAWN_PLAYER = "command.expl.spawn.player";

    public CalibreCommand(CalibrePlugin plugin) throws Exception {
        super(plugin, "calibre",
            (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command."), "cal"));

        var expl = root
            .literal("expl", ArgumentDescription.of("Perform explosion debug accounts."));

        manager.command(expl
            .literal("spawn", ArgumentDescription.of("Spawns an explosion."))
            .argument(LocationArgument.of("location"), ArgumentDescription.of("The location to spawn at."))
            .argument(DoubleArgument.of("power"), ArgumentDescription.of("The explosion power."))
            .permission(permission("explosion.spawn"))
            .handler(c -> handle(c, this::explosionSpawn)));
        manager.command(expl
            .literal("info", ArgumentDescription.of("Shows info on explosion statistics."))
            .argument(LocationArgument.of("location"), ArgumentDescription.of("The location of the explosion."))
            .argument(DoubleArgument.of("power"), ArgumentDescription.of("The explosion power."))
            .permission(permission("explosion.info"))
            .senderType(Player.class)
            .handler(c -> handle(c, this::explosionInfo)));
        manager.command(expl
            .literal("info", ArgumentDescription.of("Shows info on explosion statistics."))
            .literal("off", ArgumentDescription.of("Disables the info."))
            .senderType(Player.class)
            .handler(c -> handle(c, this::explosionInfoOff)));
    }

    private Component renderLoc(Locale locale, Location location) {
        return i18n.line(locale, LOCATION,
            c -> c.of("x", () -> text(format2(locale, location.getX()))),
            c -> c.of("y", () -> text(format2(locale, location.getY()))),
            c -> c.of("z", () -> text(format2(locale, location.getZ()))));
    }


    private void explosionSpawn(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        Location location = ctx.get("location");
        double power = ctx.get("power");

        Explosions.Instance explosion = plugin.explosions().instance(power, 0);
        explosion.spawn(location, pSender);

        if (pSender == null) {
            plugin.send(sender, i18n.lines(locale, COMMAND_EXPL_SPAWN_GENERIC,
                c -> c.of("location", () -> renderLoc(locale, location)),
                c -> c.of("power", () -> text(format2(locale, power))),
                c -> c.of("max_distance", () -> text(String.format(locale, "%.2f", explosion.maxDistance())))));
        } else {
            Explosions.Instance.DamageComponent damage = explosion.computeDamage(location, pSender);
            plugin.send(sender, i18n.lines(locale, COMMAND_EXPL_SPAWN_PLAYER,
                c -> c.of("location", () -> renderLoc(locale, location)),
                c -> c.of("power", () -> text(format2(locale, power))),
                c -> c.of("max_distance", () -> text(format2(locale, explosion.maxDistance()))),
                c -> c.of("distance", () -> text(format2(locale, damage.distance()))),
                c -> c.of("damage", () -> text(format2(locale, damage.damage()))),
                c -> c.of("hardness", () -> text(format2(locale, damage.hardness()))),
                c -> c.of("penetration", () -> text(format2(locale, explosion.penetration())))));
        }
    }

    private void explosionInfo(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        Location location = ctx.get("location");
        double power = ctx.get("power");

        Explosions.Instance explosion = plugin.explosions().instance(power, 0);
        plugin.playerData(pSender).explosionInfo = new ExplosionInfo(explosion, location);
    }

    private void explosionInfoOff(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        plugin.playerData(pSender).explosionInfo = null;
    }
}
