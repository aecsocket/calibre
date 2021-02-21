package com.gitlab.aecsocket.calibre.core;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class TestSystem extends AbstractSystem {
    private String lastData;

    public TestSystem() { super(0); }

    public TestSystem(String lastData) {
        super(0);
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

    @Override
    public void setup(CalibreComponent<?> parent) {}

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        tree.events().registerListener(Events.TestEvent.class, this::onEvent, 0);
    }

    protected void onEvent(Events.TestEvent event) {
        lastData = event.data;
    }

    @Override public TestSystem copy() { return new TestSystem(this); }

    public static final class Events {
        private Events() {}

        public static class TestEvent {
            private final String data;

            public TestEvent(String data) {
                this.data = data;
            }

            public String data() { return data; }
        }
    }
}
