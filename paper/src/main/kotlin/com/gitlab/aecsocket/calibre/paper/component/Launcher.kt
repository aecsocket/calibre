package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import com.gitlab.aecsocket.sokol.paper.stat.BooleanStat
import com.gitlab.aecsocket.sokol.paper.stat.DecimalStat
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class Launcher(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = CalibreAPI.key("launcher")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { LauncherSystem(it) }
            ctx.components.stats.stats(Stats.All)
        }
    }

    object Stats {
        val Velocity = DecimalStat(Key.with("velocity"))
        val Spread = DecimalStat(Key.with("spread"))
        val Delay = DecimalStat(Key.with("delay"))
        val RequireRelease = BooleanStat(Key.with("require_release"))
        val All = listOf(Velocity, Spread, Delay, RequireRelease)
    }

    override val componentType get() = Launcher::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val a: Boolean = true
    ) : SimpleComponentProfile<Launcher> {
        override val componentType get() = Launcher::class

        override fun createEmpty() = ComponentBlueprint { Launcher(this) }
    }
}

@All(Launcher::class)
class LauncherSystem(ids: ComponentIdAccess) : SokolSystem {
}
