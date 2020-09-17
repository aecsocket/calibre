package me.aecsocket.calibre.util.componentlist;

import me.aecsocket.calibre.item.component.CalibreComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;

public class CalibreComponentList extends LinkedList<CalibreComponent> {
    public CalibreComponentList() {}
    public CalibreComponentList(@NotNull Collection<? extends CalibreComponent> c) { super(c); }
}
