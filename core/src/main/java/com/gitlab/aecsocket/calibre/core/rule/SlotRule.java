package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.rule.visitor.Visitor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public final class SlotRule {
    private SlotRule() {}

    public static abstract class AsSlotPart implements Rule {
        private transient CalibreComponent<?> part;
        private final Rule operand;

        public AsSlotPart(Rule operand) {
            this.operand = operand;
        }

        private AsSlotPart() { operand = null; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            if (part == null) {
                return false;
            }
            return operand.applies(part);
        }

        @Override public void visit(Visitor visitor) {
            visitor.visit(this);
            operand.visit(visitor);
        }

        public CalibreComponent<?> part() { return part; }
        public void part(CalibreComponent<?> part) { this.part = part; }

        public Rule operand() { return operand; }

        @Override public String toString() { return type() + "{" + operand + "}"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }
    }

    @ConfigSerializable
    public static class AsChild extends AsSlotPart {
        public static final String TYPE = "as_child";
        @Override public String type() { return TYPE; }

        public AsChild(Rule operand) {
            super(operand);
        }

        private AsChild() {}
    }

    @ConfigSerializable
    public static class AsParent extends AsSlotPart {
        public static final String TYPE = "as_parent";
        @Override public String type() { return TYPE; }

        public AsParent(Rule operand) {
            super(operand);
        }

        private AsParent() {}
    }
}
