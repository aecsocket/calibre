package com.gitlab.aecsocket.calibre.core.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
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
