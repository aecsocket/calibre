package com.gitlab.aecsocket.calibre.paper;

import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;

public class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    /** The ID for this plugin on https://bstats.org. */
    public static final int BSTATS_ID = 10479;

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
