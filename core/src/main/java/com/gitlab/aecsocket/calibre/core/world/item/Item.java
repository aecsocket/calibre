package com.gitlab.aecsocket.calibre.core.world.item;

import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collection;

public interface Item {
    void saveTree(ComponentTree tree);

    Component name();
    void name(Component component);

    void addInfo(Collection<Component> components);
    default void addInfo(Component... components) { addInfo(Arrays.asList(components)); }

    int amount();

    void subtract(int amount);
    default void subtract() { subtract(1); }
    boolean add();
}
