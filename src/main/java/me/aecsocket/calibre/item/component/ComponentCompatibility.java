package me.aecsocket.calibre.item.component;

import me.aecsocket.calibre.item.system.CalibreSystem;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

public class ComponentCompatibility implements Predicate<CalibreComponent> {
    private final Collection<String> categories;
    private final Collection<String> ids;
    private final Collection<Class<? extends CalibreSystem>> services;

    public ComponentCompatibility(Collection<String> categories, Collection<String> ids, Collection<Class<? extends CalibreSystem>> services) {
        this.categories = categories;
        this.ids = ids;
        this.services = services;
    }

    public Collection<String> getCategories() { return categories; }
    public Collection<String> getIds() { return ids; }
    public Collection<Class<? extends CalibreSystem>> getServices() { return services; }

    @Override
    public boolean test(CalibreComponent component) {
        if (
                categories != null && categories.size() > 0
                && component.getCategories() != null && Collections.disjoint(categories, component.getCategories())
        ) return false;
        if (
                ids != null && ids.size() > 0
                && !ids.contains(component.getId())
        ) return false;
        if (
                services != null && services.size() > 0
                && Collections.disjoint(component.getSystemServices().keySet(), services)
        ) return false;
        return true;
    }
}
