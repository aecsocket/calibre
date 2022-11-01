package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class LauncherOrigin(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("launcher_origin")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = LauncherOrigin::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : NonReadingComponentProfile {
        override fun readEmpty() = LauncherOrigin(this)
    }
}

@All(LauncherOrigin::class)
class LauncherOriginSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mLauncherOrigin = mappers.componentMapper<LauncherOrigin>()

    @Subscribe
    fun on(event: LauncherSystem.Build, entity: SokolEntity) {
        val launcherOrigin = mLauncherOrigin.get(entity).profile

        event.launchOrigin += launcherOrigin.transform
    }
}
