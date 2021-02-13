package me.aecsocket.calibre.component;

import me.aecsocket.calibre.CalibreComponentImpl;
import me.aecsocket.calibre.TestSystem;
import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.world.Item;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CalibreComponentTest {
    private CalibreComponent<Item> createRoot() {
        return new CalibreComponentImpl("comp_a")
                .slot("a", new CalibreSlot().set(new CalibreComponentImpl("comp_b")
                        .slot("b", new CalibreSlot().set(new CalibreComponentImpl("comp_c")))))
                .slot("c", new CalibreSlot().set(new CalibreComponentImpl("comp_d").system(new TestSystem())))
                .slot("d", new CalibreSlot());
    }

    @Test
    void testNavigation() {
        CalibreComponent<?> root = createRoot();

        assertEquals((Object) root.slot("a"), root.component("a").parent());
        assertEquals(root, root.component("a").root());
        assertEquals("[a, b]", Arrays.toString(root.component("a", "b").path()));
    }

    @Test
    void testTree() {
        CalibreComponent<?> root = createRoot();
        ComponentTree tree = root.buildTree().tree;

        assertEquals(tree, root.tree());
        root.walk(data -> {
            if (data.slot() != null && data.component() != null)
                assertEquals(tree, data.<CalibreComponent<?>>component().tree);
        });
    }

    @Test
    void testEvents() {
        CalibreSystem system = new TestSystem();
        CalibreComponent<Item> root = createRoot().system(system);
        ComponentTree tree = root.buildTree().tree;

        String data = "abc";
        tree.call(new TestSystem.Events.TestEvent(data));
        assertEquals(data, root.<TestSystem>system(system.id()).lastData());
    }

    @Test
    void testCopy() throws SerializationException {
        CalibreComponent<?> one = createRoot();
        CalibreComponent<?> two = one.copy().buildTree();
        two.<CalibreSlot>slot("a", "b").set(new CalibreComponentImpl("alt_comp_c"));

        assertNotEquals(one.<CalibreComponent<?>>component("a", "b"), two.<CalibreComponent<?>>component("a", "b"));

        one.<CalibreComponent<Item>>component("c").<TestSystem>system("test").lastData("one");
        two.<CalibreComponent<Item>>component("c").<TestSystem>system("test").lastData("two");
        CalibreComponent<?> three = one.copy();
        assertEquals("one", one.<CalibreComponent<Item>>component("c").<TestSystem>system("test").lastData());
        assertEquals("two", two.<CalibreComponent<Item>>component("c").<TestSystem>system("test").lastData());
        assertEquals("one", three.<CalibreComponent<Item>>component("c").<TestSystem>system("test").lastData());
    }
}
