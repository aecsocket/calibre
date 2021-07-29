package com.gitlab.aecsocket.calibre.paper.sight;

import com.gitlab.aecsocket.calibre.core.sight.Sight;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.List;

@ConfigSerializable
public record PaperSight(
        @Required String id,
        double zoom,
        @Nullable List<PreciseSound> applySound,
        @Nullable Animation applyAnimation,
        @Nullable ItemDescriptor shaderData,
        long shaderDataDelay,
        @Nullable StatLists stats
) implements Sight {}
