package com.github.aecsocket.calibre.core.sight;

import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;

public interface Sight {
    String id();
    double zoom();
    StatLists stats();
}
