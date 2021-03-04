package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.rule.visitor.Visitor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Arrays;

public final class ComponentRule {
    private ComponentRule() {}

    @ConfigSerializable
    public static class Complete implements Rule {
        public static final String TYPE = "complete";
        @Override public String type() { return TYPE; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            return component.tree().complete();
        }

        @Override public void visit(Visitor visitor) { visitor.visit(this); }

        @Override public String toString() { return TYPE; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }
    }

    @ConfigSerializable
    public static class SlotFilled implements Rule {
        public static final String TYPE = "slot_filled";
        @Override public String type() { return TYPE; }

        @Required private final String[] path;

        public SlotFilled(String[] path) {
            this.path = path;
        }

        private SlotFilled() {
            path = null;
        }

        public String[] path() { return path; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            return component.component(path) != null;
        }

        @Override public void visit(Visitor visitor) { visitor.visit(this); }

        @Override public String toString() { return TYPE + "{" + Arrays.toString(path) + "}"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }
    }

    @ConfigSerializable
    public static class HasCategory implements Rule {
        public static final String TYPE = "has_category";
        @Override public String type() { return TYPE; }

        @Required private final String category;

        public HasCategory(String category) {
            this.category = category;
        }

        private HasCategory() { category = null; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            return component.categories().contains(category);
        }

        @Override public void visit(Visitor visitor) { visitor.visit(this); }

        @Override public String toString() { return TYPE + "{" + category + "}"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }
    }

    @ConfigSerializable
    public static class HasSystem implements Rule {
        public static final String TYPE = "has_system";
        @Override public String type() { return TYPE; }

        @Required private final String id;

        public HasSystem(String id) {
            this.id = id;
        }

        private HasSystem() { id = null; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            return component.systems().containsKey(id);
        }

        @Override public void visit(Visitor visitor) { visitor.visit(this); }

        @Override public String toString() { return TYPE + "{" + id + "}"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }
    }
}
