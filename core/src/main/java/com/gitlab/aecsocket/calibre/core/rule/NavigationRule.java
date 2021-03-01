package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Arrays;
import java.util.Objects;

public final class NavigationRule {
    private NavigationRule() {}

    @ConfigSerializable
    public static class As implements Rule {
        public static final String TYPE = "as";
        @Override public String type() { return TYPE; }

        @Required private final String[] target;
        @Required private final Rule operand;

        public As(String[] target, Rule operand) {
            this.target = target;
            this.operand = operand;
        }

        private As() { target = null; operand = null; }

        public String[] target() { return target; }
        public Rule operand() { return operand; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            component = component.component(target);
            if (component == null)
                return false;
            return operand.applies(component);
        }

        @Override public String toString() { return TYPE + Arrays.toString(target) + "{" + operand + "}"; }

        @Override
        public int hashCode() {
            int result = Objects.hash(operand);
            result = 31 * result + Arrays.hashCode(target);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            As as = (As) o;
            return Arrays.equals(target, as.target) && operand.equals(as.operand);
        }
    }

    @ConfigSerializable
    public static class AsRoot implements Rule {
        public static final String TYPE = "as_root";
        @Override public String type() { return TYPE; }

        private final String[] target;
        @Required private final Rule operand;

        public AsRoot(String[] target, Rule operand) {
            this.target = target;
            this.operand = operand;
        }

        private AsRoot() { target = null; operand = null; }

        public String[] target() { return target; }
        public Rule operand() { return operand; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            component = component.root().component(target);
            if (component == null)
                return false;
            return operand.applies(component);
        }

        @Override public String toString() { return TYPE + Arrays.toString(target) + "{" + operand + "}"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AsRoot asRoot = (AsRoot) o;
            return Arrays.equals(target, asRoot.target) && operand.equals(asRoot.operand);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(operand);
            result = 31 * result + Arrays.hashCode(target);
            return result;
        }
    }
}
