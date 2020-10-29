package me.aecsocket.calibre.defaults.system.gun.firemode;

import me.aecsocket.calibre.util.OrderedStatMap;

public class FireMode {
    private String name;
    private OrderedStatMap stats;

    public FireMode(String name, OrderedStatMap stats) {
        this.name = name;
        this.stats = stats;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OrderedStatMap getStats() { return stats; }
    public void setStats(OrderedStatMap stats) { this.stats = stats; }

    @Override public String toString() { return name + ":" + stats; }
}
