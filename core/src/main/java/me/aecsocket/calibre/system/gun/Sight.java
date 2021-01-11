package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.util.StatCollection;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Objects;

@ConfigSerializable
public class Sight {
    protected String id;
    protected StatCollection activeStats;
    protected StatCollection notActiveStats;
    protected StatCollection aimingStats;
    protected StatCollection notAimingStats;

    public Sight(String id, StatCollection activeStats, StatCollection notActiveStats, StatCollection aimingStats, StatCollection notAimingStats) {
        this.id = id;
        this.activeStats = activeStats;
        this.notActiveStats = notActiveStats;
        this.aimingStats = aimingStats;
        this.notAimingStats = notAimingStats;
    }

    public Sight() {}

    public String id() { return id; }
    public Sight id(String id) { this.id = id; return this; }

    public StatCollection activeStats() { return activeStats; }
    public Sight activeStats(StatCollection activeStats) { this.activeStats = activeStats; return this; }

    public StatCollection notActiveStats() { return notActiveStats; }
    public Sight notActiveStats(StatCollection notActiveStats) { this.notActiveStats = notActiveStats; return this; }

    public StatCollection aimingStats() { return aimingStats; }
    public Sight aimingStats(StatCollection aimingStats) { this.aimingStats = aimingStats; return this; }

    public StatCollection notAimingStats() { return notAimingStats; }
    public Sight notAimingStats(StatCollection notAimingStats) { this.notAimingStats = notAimingStats; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sight sight = (Sight) o;
        return id.equals(sight.id) && Objects.equals(activeStats, sight.activeStats) && Objects.equals(notActiveStats, sight.notActiveStats) && Objects.equals(aimingStats, sight.aimingStats) && Objects.equals(notAimingStats, sight.notAimingStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activeStats, notActiveStats, aimingStats, notAimingStats);
    }

    @Override public String toString() { return "Sight{" + id + "}"; }
}
