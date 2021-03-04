package com.gitlab.aecsocket.calibre.core.rule.visitor;

import com.gitlab.aecsocket.calibre.core.rule.Rule;

public interface Visitor {
    void visit(Rule rule);
}
