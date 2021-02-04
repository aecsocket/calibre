package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;

import java.util.Arrays;

public abstract class ChamberSystem extends AbstractSystem {
    public static final String ID = "chamber";

    @FromMaster
    protected String[] loadPath;

    /**
     * Used for registration + deserialization.
     */
    public ChamberSystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public ChamberSystem(ChamberSystem o) {
        super(o);
        loadPath = o.loadPath.clone();
    }

    @Override public String id() { return ID; }

    public String[] loadPath() { return loadPath; }
    public void loadPath(String[] loadPath) { this.loadPath = loadPath; }

    public CalibreSlot getLoadSlot() {
        return parent.slot(loadPath);
    }

    public abstract ChamberSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChamberSystem that = (ChamberSystem) o;
        return Arrays.equals(loadPath, that.loadPath);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(loadPath);
    }
}
