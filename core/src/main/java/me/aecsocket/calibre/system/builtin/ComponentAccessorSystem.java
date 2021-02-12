package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.world.user.ItemUser;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Predicate;

public interface ComponentAccessorSystem extends CalibreSystem {
    interface Result {
        CalibreComponent<?> component();
        void removeItem();
    }

    Result collectComponent(ItemUser user, Predicate<CalibreComponent<?>> predicate, @Nullable Comparator<CalibreComponent<?>> sorter);

    void addComponent(ItemUser user, CalibreComponent<?> component);
}
