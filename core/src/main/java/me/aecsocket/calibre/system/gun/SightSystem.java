package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.SystemSetupException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class SightSystem extends AbstractSystem {
    public static final String ID = "sight";

    protected final List<Sight> sights;

    public SightSystem() {
        sights = new ArrayList<>();
    }

    public SightSystem(SightSystem o) {
        super(o);
        sights = o.sights == null ? null : new ArrayList<>(o.sights);
    }

    public List<Sight> sights() { return sights; }

    @Override public String id() { return ID; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    public abstract SightSystem copy();

    @Override
    public void inherit(CalibreSystem child) {
        if (!(child instanceof SightSystem)) return;
        SightSystem other = (SightSystem) child;
        other.sights.addAll(sights);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SightSystem that = (SightSystem) o;
        return sights.equals(that.sights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sights);
    }
}
