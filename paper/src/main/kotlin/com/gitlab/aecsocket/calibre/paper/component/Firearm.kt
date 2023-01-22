package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import com.gitlab.aecsocket.sokol.paper.stat.DecimalStat
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class Firearm(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = CalibreAPI.key("firearm")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { FirearmSystem(it) }
            ctx.components.stats.stats(Stats.All)
        }
    }

    object Stats {
        val Damage = DecimalStat(Key.with("damage"))
        val LaunchVelocity = DecimalStat(Key.with("launch_velocity"))
        val All = listOf(Damage, LaunchVelocity)
    }

    override val componentType get() = Firearm::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val a: Boolean = true
    ) : SimpleComponentProfile<Firearm> {
        override val componentType get() = Firearm::class

        override fun createEmpty() = ComponentBlueprint { Firearm(this) }
    }
}

@All(Firearm::class)
class FirearmSystem(ids: ComponentIdAccess) : SokolSystem {
}
