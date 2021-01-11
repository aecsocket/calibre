package me.aecsocket.calibre.system.builtin;

import me.aecsocket.unifiedframework.stat.StatInstance;

public interface StatRenderer {
    Object create(StatInstance<?> inst, String locale, String key);
}
