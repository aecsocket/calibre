package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asVector3
import com.gitlab.aecsocket.sokol.paper.component.CompositeSystem
import com.gitlab.aecsocket.sokol.paper.component.PositionWrite
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable

private const val WORLD = "world"
private const val POSITION = "position"
private const val VELOCITY = "velocity"

data class ProjectileRaycast(
    val profile: Profile,
    private val dPosition: Delta<Vector3>,
    private val dVelocity: Delta<Vector3>
) : PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("projectile_raycast")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ProjectileRaycast::class
    override val key get() = Key

    override val dirty get() = dPosition.dirty || dVelocity.dirty

    constructor(
        profile: Profile,
        position: Vector3,
        velocity: Vector3
    ) : this(profile, Delta(position), Delta(velocity))

    var position by dPosition
    var velocity by dVelocity

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun writeDelta(tag: NBTTag): NBTTag {
        return tag
    }

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val gravity: Vector3 = Vector3(0.0, -9.81, 0.0)
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound { compound -> ProjectileRaycast(this,
            compound.get(POSITION) { asVector3() },
            compound.get(VELOCITY) { asVector3() }
        ) }

        override fun read(node: ConfigurationNode) = ProjectileRaycast(this,
            node.node(POSITION).get { Vector3.Zero },
            node.node(VELOCITY).get { Vector3.Zero }
        )

        override fun readEmpty() = ProjectileRaycast(this, Vector3.Zero, Vector3.Zero)
    }
}

@All(ProjectileRaycast::class)
@Before(CompositeSystem::class)
class ProjectileRaycastSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mProjectileRaycast = mappers.componentMapper<ProjectileRaycast>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val projectileRaycast = mProjectileRaycast.get(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val projectileRaycast = mProjectileRaycast.get(entity)

        projectileRaycast.position += projectileRaycast.velocity
    }
}
