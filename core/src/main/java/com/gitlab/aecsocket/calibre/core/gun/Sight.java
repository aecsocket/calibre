package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.sokol.core.stat.StatLists;

public interface Sight {
    String id();
    double zoom();
    StatLists stats();
}
