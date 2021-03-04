package com.gitlab.aecsocket.calibre.core.rule.visitor;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.rule.Rule;
import com.gitlab.aecsocket.calibre.core.rule.SlotRule;

public class SlotSetterVisitor implements Visitor {
    private final CalibreComponent<?> child;
    private final CalibreComponent<?> parent;

    public SlotSetterVisitor(CalibreComponent<?> child, CalibreComponent<?> parent) {
        this.child = child;
        this.parent = parent;
    }

    public CalibreComponent<?> child() { return child; }
    public CalibreComponent<?> parent() { return parent; }

    @Override
    public void visit(Rule rule) {
        if (rule instanceof SlotRule.AsChild) {
            ((SlotRule.AsChild) rule).part(child);
        }

        if (rule instanceof SlotRule.AsParent) {
            ((SlotRule.AsParent) rule).part(parent);
        }
    }
}
