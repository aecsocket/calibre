package me.aecsocket.calibre.util;

import me.aecsocket.unifiedframework.registry.Registry;

import java.util.Collection;
import java.util.HashSet;

/**
 * Implementation of {@link Registry} for Calibre.
 */
public class CalibreRegistry extends Registry<CalibreIdentifiable> {
    /**
     * A collection of objects to register after all others are unregistered.
     */
    private final Collection<CalibreIdentifiable> preRegister = new HashSet<>();

    public Collection<CalibreIdentifiable> getPreRegister() { return preRegister; }
    public void addPreRegister(CalibreIdentifiable object) { preRegister.add(object); }

    @Override
    public void unregisterAll() {
        super.unregisterAll();
        preRegister();
    }

    public void preRegister() {
        preRegister.forEach(this::register);
    }
}
