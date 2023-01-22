package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.gitlab.aecsocket.sokol.paper.component.ColliderInstance
import com.gitlab.aecsocket.sokol.paper.component.PositionAccess
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

private const val ENABLED = "enabled"

data class LaserEffects(
    val profile: Profile,
    val dEnabled: Delta<Boolean>
) : PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("laser_effects")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { LaserEffectsSystem(it) }
            ctx.system { LaserEffectsForwardSystem(it) }
        }
    }

    override val componentType get() = LaserEffects::class
    override val key get() = Key

    override val dirty get() = dEnabled.dirty
    var enabled by dEnabled

    constructor(
        profile: Profile,
        enabled: Boolean
    ) : this(profile, Delta(enabled))

    override fun clean() {
        dEnabled.clean()
    }

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(ENABLED) { makeBoolean(enabled) }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()
        dEnabled.ifDirty { compound.set(ENABLED) { makeBoolean(it) } }
        return compound
    }

    override fun serialize(node: ConfigurationNode) {
        node.node(ENABLED).set(enabled)
    }

    @ConfigSerializable
    data class Profile(
        @Required val maxDistance: Double,
        val stepInterval: Double = 0.0,
        val startTransform: Transform = Transform.Identity,
        val hitParticle: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val stepParticle: ParticleEngineEffect = ParticleEngineEffect.Empty,
    ) : ComponentProfile<LaserEffects> {
        override val componentType get() = LaserEffects::class

        override fun read(tag: NBTTag): ComponentBlueprint<LaserEffects> {
            val compound = tag.asCompound()
            val enabled = compound[ENABLED]?.asBoolean() ?: true
            return ComponentBlueprint { LaserEffects(this, enabled) }
        }

        override fun deserialize(node: ConfigurationNode): ComponentBlueprint<LaserEffects> {
            val enabled = node.node(ENABLED).get { true }
            return ComponentBlueprint { LaserEffects(this, enabled) }
        }

        override fun createEmpty() = ComponentBlueprint { LaserEffects(this, true) }
    }
}

@All(LaserEffects::class, PositionAccess::class)
class LaserEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mLaserEffects = ids.mapper<LaserEffects>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    object Update : SokolEvent

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val laserEffects = mLaserEffects.get(entity)
        val positionAccess = mPositionAccess.get(entity)
        val colliderInstance = mColliderInstance.getOr(entity)
        if (!laserEffects.enabled) return

        val world = positionAccess.world
        val maxDistance = laserEffects.profile.maxDistance
        val transform = positionAccess.transform * laserEffects.profile.startTransform
        val direction = transform.forward
        val from = transform.position
        val to = from + direction * maxDistance
        val physObj = colliderInstance?.physObj

        CraftBulletAPI.executePhysics {
            val rayTest = CraftBulletAPI.spaceOf(world)
                .rayTestWorld(from.bullet(), to.bullet())
                .firstOrNull {
                    physObj == null || it.collisionObject !== physObj
                }

            val hitDistance = maxDistance * (rayTest?.hitFraction ?: 1f)
            rayTest?.let {
                val hit = from + direction * hitDistance
                AlexandriaAPI.particles.spawn(laserEffects.profile.hitParticle, hit.location(world))
            }

            val stepInterval = laserEffects.profile.stepInterval
            if (stepInterval > 0.0) {
                var travelled = 0.0
                while (travelled < hitDistance) {
                    AlexandriaAPI.particles.spawn(
                        laserEffects.profile.stepParticle,
                        (from + direction * travelled).location(world)
                    )
                    travelled += stepInterval
                }
            }
        }
    }
}

class LaserEffectsForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, LaserEffectsSystem.Update)
    }
}
