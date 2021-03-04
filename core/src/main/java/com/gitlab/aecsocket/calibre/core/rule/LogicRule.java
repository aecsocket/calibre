package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.rule.visitor.Visitor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class LogicRule {
    private LogicRule() {}

    @ConfigSerializable
    public static class Not implements Rule {
        public static final String TYPE = "not";
        @Override public String type() { return TYPE; }

        @Required private final Rule operand;

        public Not(Rule operand) {
            this.operand = operand;
        }

        private Not() { operand = null; }

        public Rule operand() { return operand; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            return !operand.applies(component);
        }

        @Override public void visit(Visitor visitor) {
            visitor.visit(this);
            operand.visit(visitor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operand);
        }

        @Override public String toString() { return "! " + operand; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Not not = (Not) o;
            return operand.equals(not.operand);
        }
    }

    public static abstract class TakesOperands implements Rule {
        @Required protected final List<Rule> operands = new ArrayList<>();

        public List<Rule> operands() { return operands; }

        @Override
        public int hashCode() {
            return Objects.hash(operands);
        }

        @Override public void visit(Visitor visitor) {
            visitor.visit(this);
            for (Rule operand : operands) {
                operand.visit(visitor);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TakesOperands that = (TakesOperands) o;
            return operands.equals(that.operands);
        }

        protected abstract String separator();

        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(separator());
            for (Rule operand : operands) {
                result.add("(" + operand.toString() + ")");
            }
            return result.toString();
        }
    }

    @ConfigSerializable
    public static class And extends TakesOperands {
        public static final String TYPE = "and";
        @Override public String type() { return TYPE; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            for (Rule rule : operands) {
                if (!rule.applies(component))
                    return false;
            }
            return true;
        }

        @Override protected String separator() { return " & "; }
    }

    @ConfigSerializable
    public static class Or extends TakesOperands {
        public static final String TYPE = "or";
        @Override public String type() { return TYPE; }

        @Override
        public boolean applies(CalibreComponent<?> component) {
            for (Rule rule : operands) {
                if (rule.applies(component))
                    return true;
            }
            return false;
        }

        @Override protected String separator() { return " | "; }
    }
}
