package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.PositionWrite
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

class ProjectileBullet(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = CalibreAPI.key("projectile_bullet")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ProjectileBullet::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val a: Boolean = false,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = ProjectileBullet(this)
    }
}

@All(ProjectileBullet::class, PositionWrite::class)
class ProjectileBulletSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mProjectileBullet = mappers.componentMapper<ProjectileBullet>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val projectileBullet = mProjectileBullet.get(entity)
        val positionWrite = mPositionWrite.get(entity)
    }
}
