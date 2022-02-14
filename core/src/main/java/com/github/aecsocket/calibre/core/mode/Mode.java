package com.github.aecsocket.calibre.core.mode;

import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;

public interface Mode {
    String id();
    StatLists stats();
}
