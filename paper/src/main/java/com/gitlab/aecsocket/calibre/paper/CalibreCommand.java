package com.gitlab.aecsocket.calibre.paper;

import cloud.commandframework.ArgumentDescription;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;

/* package */ class CalibreCommand extends BaseCommand<CalibrePlugin> {
    public CalibreCommand(CalibrePlugin plugin) throws Exception {
        super(plugin, "calibre",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command."), "cal"));
    }
}
