package me.aecsocket.calibre.defaults.system;

import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.component.IncompatibleComponentException;
import me.aecsocket.unifiedframework.util.Quantifier;

import java.util.LinkedList;

public interface ComponentProviderSystem extends CalibreSystem {
    @Override default void initialize(CalibreComponent parent, ComponentTree tree) {
        parent.registerSystemService(ComponentProviderSystem.class, this);
    }

    LinkedList<Quantifier<CalibreComponent>> getComponents();

    default Quantifier<CalibreComponent> peekRaw() { return getComponents().peekLast(); }
    default CalibreComponent peek() { return peekRaw() == null ? null : peekRaw().get(); }

    default Quantifier<CalibreComponent> nextRaw() {
        LinkedList<Quantifier<CalibreComponent>> ammo = getComponents();
        Quantifier<CalibreComponent> result = ammo.pollLast();
        if (result != null) {
            result.add(-1);
            if (result.getAmount() <= 0)
                ammo.removeLast();
            return result;
        }
        return null;
    }
    default CalibreComponent next() {
        Quantifier<CalibreComponent> result = nextRaw();
        return result == null ? null : result.get();
    }

    boolean isCompatible(CalibreComponent component);

    default Quantifier<CalibreComponent> add(CalibreComponent object, int amount) throws IncompatibleComponentException {
        return add(new Quantifier<>(object, amount));
    }
    default Quantifier<CalibreComponent> add(Quantifier<CalibreComponent> quantifier) throws IncompatibleComponentException {
        if (!isCompatible(quantifier.get())) throw new IncompatibleComponentException();
        LinkedList<Quantifier<CalibreComponent>> ammo = getComponents();
        Quantifier<CalibreComponent> last = ammo.peekLast();
        if (last != null && last.get().equals(quantifier.get())) {
            last.add(quantifier.getAmount());
            return last;
        }
        ammo.add(quantifier);
        return quantifier;
    }
}
