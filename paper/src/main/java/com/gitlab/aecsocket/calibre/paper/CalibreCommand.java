package com.gitlab.aecsocket.calibre.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.FloatArgument;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import org.bukkit.entity.Player;

/* package */ class CalibreCommand extends BaseCommand<CalibrePlugin> {
    public CalibreCommand(CalibrePlugin plugin) throws Exception {
        super(plugin, "calibre",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command."), "cal"));

        manager.command(root
                .literal("zoom")
                .argument(FloatArgument.of("zoom"))
                .handler(ctx -> {
                    plugin.zoom((Player) ctx.getSender(), ctx.get("zoom"));
                })
        );
    }
}
