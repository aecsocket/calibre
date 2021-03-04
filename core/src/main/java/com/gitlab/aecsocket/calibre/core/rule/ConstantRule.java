package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.rule.visitor.Visitor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Objects;

@ConfigSerializable
public final class ConstantRule implements Rule {
    public static final String TYPE = "constant";
    @Override public String type() { return TYPE; }

    @Required private final boolean value;

    public ConstantRule(boolean value) {
        this.value = value;
    }

    private ConstantRule() { value = false; }

    public boolean value() { return value; }

    @Override public boolean applies(CalibreComponent<?> component) { return value; }

    @Override public void visit(Visitor visitor) { visitor.visit(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantRule that = (ConstantRule) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
