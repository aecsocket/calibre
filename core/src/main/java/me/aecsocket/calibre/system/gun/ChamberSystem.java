package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.builtin.ProjectileSystem;
import me.aecsocket.calibre.system.SystemSetupException;

import java.util.Arrays;

public abstract class ChamberSystem extends AbstractSystem {
    public static final String ID = "chamber";

    @FromParent
    protected String[] projectilePath;

    public ChamberSystem() {}

    public ChamberSystem(ChamberSystem o) {
        super(o);
        projectilePath = o.projectilePath;
    }

    @Override public String id() { return ID; }

    public String[] projectilePath() { return projectilePath; }
    public void projectilePath(String[] projectilePath) { this.projectilePath = projectilePath; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    public ProjectileSystem getProjectile() {
        CalibreComponent<?> atPath = parent.component(projectilePath);
        if (atPath == null)
            return null;
        return atPath.system(ProjectileSystem.class);
    }

    public abstract ChamberSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChamberSystem that = (ChamberSystem) o;
        return Arrays.equals(projectilePath, that.projectilePath);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(projectilePath);
    }
}
