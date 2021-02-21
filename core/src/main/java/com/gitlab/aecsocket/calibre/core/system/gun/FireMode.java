package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Objects;

@ConfigSerializable
public class FireMode {
    protected String id;
    protected StatCollection activeStats;
    protected StatCollection notActiveStats;

    public FireMode() {}

    public String id() { return id; }
    public FireMode id(String id) { this.id = id; return this; }

    public StatCollection activeStats() { return activeStats; }
    public FireMode activeStats(StatCollection activeStats) { this.activeStats = activeStats; return this; }

    public StatCollection notActiveStats() { return notActiveStats; }
    public FireMode notActiveStats(StatCollection notActiveStats) { this.notActiveStats = notActiveStats; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireMode fireMode = (FireMode) o;
        return id.equals(fireMode.id) && Objects.equals(activeStats, fireMode.activeStats) && Objects.equals(notActiveStats, fireMode.notActiveStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activeStats, notActiveStats);
    }

    @Override
    public String toString() { return "FireMode{" + id + '}'; }
}
