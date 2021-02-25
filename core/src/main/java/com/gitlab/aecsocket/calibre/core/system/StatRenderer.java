package com.gitlab.aecsocket.calibre.core.system;

import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface StatRenderer {
    List<Component> createInfo(Locale locale, Map<Integer, StatMap> stats, Component prefix);
}
