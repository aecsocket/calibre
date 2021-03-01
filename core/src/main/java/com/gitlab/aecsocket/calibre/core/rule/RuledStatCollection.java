package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Objects;

@ConfigSerializable
public class RuledStatCollection {
    private Rule rule;
    private StatCollection ifTrue;
    private StatCollection ifFalse;

    public RuledStatCollection(Rule rule, StatCollection ifTrue, StatCollection ifFalse) {
        this.rule = rule;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    public RuledStatCollection(RuledStatCollection o) {
        rule = o.rule;
        ifTrue = o.ifTrue == null ? null : new StatCollection(o.ifTrue);
        ifFalse = o.ifFalse == null ? null : new StatCollection(o.ifFalse);
    }

    public RuledStatCollection() {}

    public Rule rule() { return rule; }
    public void rule(Rule rule) { this.rule = rule; }

    public StatCollection ifTrue() { return ifTrue; }
    public void ifTrue(StatCollection ifTrue) { this.ifTrue = ifTrue; }

    public StatCollection ifFalse() { return ifFalse; }
    public void ifFalse(StatCollection ifFalse) { this.ifFalse = ifFalse; }

    public void clear() {
        rule = null;
        ifTrue = null;
        ifFalse = null;
    }

    public StatCollection forComponent(CalibreComponent<?> component) {
        return rule == null || rule.applies(component) ? ifTrue : ifFalse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuledStatCollection that = (RuledStatCollection) o;
        return Objects.equals(rule, that.rule) && Objects.equals(ifTrue, that.ifTrue) && Objects.equals(ifFalse, that.ifFalse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, ifTrue, ifFalse);
    }

    @Override public String toString() { return rule + " > " + ifTrue + " > " + ifFalse; }
}
