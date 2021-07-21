package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

@ConfigSerializable
public record Sight(
        @Required String id,
        double zoom,
        StatLists stats
) {}
