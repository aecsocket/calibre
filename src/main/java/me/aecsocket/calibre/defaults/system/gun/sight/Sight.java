package me.aecsocket.calibre.defaults.system.gun.sight;

import me.aecsocket.calibre.util.OrderedStatMap;

public class Sight {
    private String name;
    private OrderedStatMap inactiveStats;
    private OrderedStatMap activeStats;

    public Sight(String name, OrderedStatMap inactiveStats, OrderedStatMap activeStats) {
        this.name = name;
        this.inactiveStats = inactiveStats;
        this.activeStats = activeStats;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OrderedStatMap getInactiveStats() { return inactiveStats; }
    public void setInactiveStats(OrderedStatMap inactiveStats) { this.inactiveStats = inactiveStats; }

    public OrderedStatMap getActiveStats() { return activeStats; }
    public void setActiveStats(OrderedStatMap activeStats) { this.activeStats = activeStats; }

    @Override public String toString() { return name + ":" + inactiveStats + " " + activeStats; }
}
