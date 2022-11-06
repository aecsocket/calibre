package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import kotlin.reflect.KClass

data class Aiming(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = CalibreAPI.key("aiming")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Aiming::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val a: Boolean = true
    ) : NonReadingComponentProfile {
        override fun readEmpty() = Aiming(this)
    }
}
