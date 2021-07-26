package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.sokol.core.system.System;

import java.util.List;

public interface SelectorHolderSystem<T> extends System.Instance {
    List<T> selections();
}
