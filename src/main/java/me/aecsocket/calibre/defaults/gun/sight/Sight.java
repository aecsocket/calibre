package me.aecsocket.calibre.defaults.gun.sight;

import me.aecsocket.unifiedframework.stat.StatMap;

public class Sight {
    private StatMap selected;
    private StatMap active;

    public StatMap getSelected() { return selected; }
    public void setSelected(StatMap selected) { this.selected = selected; }

    public StatMap getActive() { return active; }
    public void setActive(StatMap active) { this.active = active; }
}
