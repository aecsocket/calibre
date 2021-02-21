package com.gitlab.aecsocket.calibre.paper.util;

import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.util.CalibreIdentifiable;
import com.gitlab.aecsocket.unifiedframework.core.registry.Ref;
import com.gitlab.aecsocket.unifiedframework.core.registry.Registry;
import com.gitlab.aecsocket.unifiedframework.core.registry.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CalibreRegistry extends Registry<CalibreIdentifiable> {
    private final List<Consumer<CalibreRegistry>> onLoad = new ArrayList<>();

    public List<Consumer<CalibreRegistry>> onLoad() { return onLoad; }
    public void onLoad(Consumer<CalibreRegistry> supplier) { onLoad.add(supplier); }

    @Override
    public void unregisterAll() {
        super.unregisterAll();
        onLoad.forEach(consumer -> consumer.accept(this));
    }

    @Override
    public Ref<CalibreIdentifiable> register(CalibreIdentifiable object) throws ValidationException {
        if (object instanceof CalibreSystem && !(object instanceof PaperSystem))
            throw new ValidationException("Systems registered must be a PaperSystem");
        return super.register(object);
    }
}
