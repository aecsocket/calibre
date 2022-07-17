package com.gitlab.aecsocket.calibre.paper

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CalibreSettings(
    val enableBstats: Boolean = true,
)
