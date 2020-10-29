package me.aecsocket.calibre.defaults.system.gun.sight;

import me.aecsocket.calibre.util.OrderedStatMap;

public class Sight {
    private String name;
    private OrderedStatMap stats;
    private OrderedStatMap activeStats;

    public Sight(String name, OrderedStatMap stats, OrderedStatMap activeStats) {
        this.name = name;
        this.stats = stats;
        this.activeStats = activeStats;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OrderedStatMap getStats() { return stats; }
    public void setStats(OrderedStatMap stats) { this.stats = stats; }

    public OrderedStatMap getActiveStats() { return activeStats; }
    public void setActiveStats(OrderedStatMap activeStats) { this.activeStats = activeStats; }

    @Override public String toString() { return name + ":" + stats; }
}
