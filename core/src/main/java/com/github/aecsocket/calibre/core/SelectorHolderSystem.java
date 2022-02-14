package com.github.aecsocket.calibre.core;

import com.gitlab.aecsocket.sokol.core.system.System;

import java.util.List;

public interface SelectorHolderSystem<T> extends System.Instance {
    List<T> selections();
}
