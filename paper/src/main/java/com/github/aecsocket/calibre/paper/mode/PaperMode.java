package com.github.aecsocket.calibre.paper.mode;

import com.github.aecsocket.calibre.core.mode.Mode;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.List;

@ConfigSerializable
public record PaperMode(
        @Required String id,
        @Nullable List<PreciseSound> applySound,
        @Nullable Animation applyAnimation,
        @Nullable StatLists stats
) implements Mode {}
