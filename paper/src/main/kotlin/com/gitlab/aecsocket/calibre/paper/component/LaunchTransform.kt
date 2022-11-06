package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class LaunchTransform(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = CalibreAPI.key("launch_transform")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = LaunchTransform::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : NonReadingComponentProfile {
        override fun readEmpty() = LaunchTransform(this)
    }
}

@All(LaunchTransform::class)
class LaunchTransformSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mLaunchTransform = mappers.componentMapper<LaunchTransform>()

    @Subscribe
    fun on(event: LauncherSystem.Build, entity: SokolEntity) {
        val launcherOrigin = mLaunchTransform.get(entity).profile
        event.launchOrigin += launcherOrigin.transform
    }
}
