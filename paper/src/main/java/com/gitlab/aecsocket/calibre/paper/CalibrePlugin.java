package com.gitlab.aecsocket.calibre.paper;

import com.gitlab.aecsocket.calibre.core.gun.SightSystem;
import com.gitlab.aecsocket.calibre.paper.gun.PaperSightSystem;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;

public class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    /** The ID for this plugin on https://bstats.org. */
    public static final int BSTATS_ID = 10479;

    @Override
    public void onEnable() {
        super.onEnable();
        SokolPlugin sokol = SokolPlugin.instance();
        sokol
                .registerSystemType(SightSystem.ID, PaperSightSystem.type(sokol));
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
