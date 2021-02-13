package me.aecsocket.calibre.system;

import me.aecsocket.unifiedframework.stat.StatMap;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

public interface StatRenderer {
    List<Component> createInfo(String locale, Map<Integer, StatMap> stats, Component prefix);
}
