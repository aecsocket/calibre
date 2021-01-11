package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.SystemSetupException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class FireModeSystem extends AbstractSystem {
    public static final String ID = "fire_mode";

    protected final List<FireMode> fireModes;

    public FireModeSystem() {
        fireModes = new ArrayList<>();
    }

    public FireModeSystem(FireModeSystem o) {
        super(o);
        fireModes = o.fireModes == null ? null : new ArrayList<>(o.fireModes);
    }

    public List<FireMode> fireModes() { return fireModes; }

    @Override public String id() { return ID; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    public abstract FireModeSystem copy();

    @Override
    public void inherit(CalibreSystem child) {
        if (!(child instanceof FireModeSystem)) return;
        FireModeSystem other = (FireModeSystem) child;
        other.fireModes.addAll(fireModes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireModeSystem that = (FireModeSystem) o;
        return fireModes.equals(that.fireModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fireModes);
    }
}
