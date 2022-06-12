package com.github.aecsocket.calibre.paper

import com.github.aecsocket.alexandria.paper.plugin.CloudCommand
import com.github.aecsocket.alexandria.paper.plugin.desc

internal class CalibreCommand(plugin: CalibrePlugin) : CloudCommand<CalibrePlugin>(
    plugin, "calibre",
    { manager, rootName -> manager.commandBuilder(rootName, desc("Core plugin command."), "cal") }
) {
    init {
        manager.command(root
            .literal("inspect")
            .handler {  }
        )
    }
}
