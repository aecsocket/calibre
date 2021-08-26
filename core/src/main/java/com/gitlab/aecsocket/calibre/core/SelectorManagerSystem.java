package com.gitlab.aecsocket.calibre.core;

import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class SelectorManagerSystem<S extends SelectorHolderSystem<T>, T> extends AbstractSystem.Instance {
    public record Reference<S extends SelectorHolderSystem<T>, T>(S system, int index, T selection) {}

    protected @Nullable SystemPath targetSystem;
    protected int targetIndex;

    protected Reference<S, T> selected;

    public SelectorManagerSystem(TreeNode parent, @Nullable SystemPath targetSystem, int targetIndex) {
        super(parent);
        this.targetSystem = targetSystem;
        this.targetIndex = targetIndex;
    }

    public SelectorManagerSystem(TreeNode parent) {
        this(parent, null, 0);
    }

    protected abstract Optional<? extends T> fallback();
    public Optional<SystemPath> targetSystem() { return Optional.ofNullable(targetSystem); }
    public int targetIndex() { return targetIndex; }

    public Optional<Reference<S, T>> selectedRef(SystemPath targetSystem, int targetIndex) {
        return targetSystem.<S>get(parent)
                .map(sys -> targetIndex >= sys.selections().size()
                        ? null
                        : new Reference<>(sys, targetIndex, sys.selections().get(targetIndex)));
    }

    public Optional<Reference<S, T>> selectedRef() {
        if (selected != null)
            return Optional.of(selected);
        Optional<Reference<S, T>> result = targetSystem == null ? Optional.empty() : selectedRef(targetSystem, targetIndex);
        result.ifPresent(v -> selected = v);
        return result;
    }

    public Optional<T> selected() {
        return selectedRef().map(r -> r.selection).or(this::fallback);
    }

    protected abstract System.Key<S> holderKey();

    public List<Reference<S, T>> collect() {
        List<Reference<S, T>> result = new ArrayList<>();
        parent.visitNodes((node, path) -> node.system(holderKey()).ifPresent(sys -> {
            List<T> selections = sys.selections();
            for (int i = 0; i < selections.size(); i++) {
                result.add(new Reference<>(sys, i, selections.get(i)));
            }
        }));
        return result;
    }

    public int selectedIndex(List<Reference<S, T>> refs) {
        if (targetSystem == null)
            return -1;
        for (int i = 0; i < refs.size(); i++) {
            Reference<S, T> ref = refs.get(i);
            if (ref.index == targetIndex && Arrays.equals(ref.system.parent().path(), targetSystem.nodes()))
                return i;
        }
        return -1;
    }
}
