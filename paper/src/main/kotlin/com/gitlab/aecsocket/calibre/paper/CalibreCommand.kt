package com.gitlab.aecsocket.calibre.paper

import com.gitlab.aecsocket.alexandria.paper.plugin.CloudCommand
import com.gitlab.aecsocket.alexandria.paper.plugin.desc

internal class CalibreCommand(plugin: Calibre) : CloudCommand<Calibre>(
    plugin, "calibre",
    { manager, rootName -> manager.commandBuilder(rootName, desc("Core plugin command.")) }
) {
    init {
    }
}
