package me.aecsocket.calibre;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class TestSystem extends AbstractSystem {
    private String lastData;

    public TestSystem() {}

    public TestSystem(String lastData) {
        this.lastData = lastData;
    }

    public TestSystem(TestSystem o) {
        super(o);
        lastData = o.lastData;
    }

    @Override public String id() { return "test"; }
    @Override public void id(String s) {}

    public String lastData() { return lastData; }
    public void lastData(String lastData) { this.lastData = lastData; }

    @Override public net.kyori.adventure.text.Component gen(String locale, String key, Object... args) { return net.kyori.adventure.text.Component.text(""); }
    @Override public ConfigurationNode setting(Object... path) { return BasicConfigurationNode.root(); }

    @Override
    public void setup(CalibreComponent<?> parent) {}

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        tree.events().registerListener(Events.DebugEvent.class, this::onEvent, 0);
    }

    protected void onEvent(Events.DebugEvent event) {
        lastData = event.data;
    }

    @Override public TestSystem copy() { return new TestSystem(this); }

    public static final class Events {
        private Events() {}

        public static class DebugEvent {
            private final String data;

            public DebugEvent(String data) {
                this.data = data;
            }

            public String data() { return data; }
        }
    }
}
