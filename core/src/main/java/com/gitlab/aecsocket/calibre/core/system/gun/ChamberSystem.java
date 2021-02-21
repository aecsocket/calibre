package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;

import java.util.Arrays;

public abstract class ChamberSystem extends AbstractSystem {
    public static final String ID = "chamber";

    @FromMaster protected String[] loadPath;

    /**
     * Used for registration + deserialization.
     */
    public ChamberSystem() { super(0); }

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

    @Override public abstract ChamberSystem copy();

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
