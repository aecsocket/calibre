package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.craftbullet.core.times
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.component.Collider
import com.gitlab.aecsocket.sokol.paper.component.RigidBody
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class LauncherPhysicsRecoil(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("launcher_physics_recoil")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = LauncherPhysicsRecoil::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val impulse: Float = 0f,
        val torqueImpulse: Vector3 = Vector3.Zero,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = LauncherPhysicsRecoil(this)
    }
}

@All(LauncherPhysicsRecoil::class, Launcher::class, Collider::class, RigidBody::class)
class LauncherPhysicsRecoilSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mLauncherPhysicsRecoil = mappers.componentMapper<LauncherPhysicsRecoil>()
    private val mCollider = mappers.componentMapper<Collider>()

    @Subscribe
    fun on(event: LauncherSystem.Launch, entity: SokolEntity) {
        val launcherPhysicsRecoil = mLauncherPhysicsRecoil.get(entity).profile
        val collider = mCollider.get(entity)
        val body = collider.body?.body?.body as? PhysicsRigidBody ?: return

        val launchOrigin = event.launchOrigin
        val impulse = (launchOrigin.rotation * Vector3.Backward).bullet() * launcherPhysicsRecoil.impulse
        body.applyCentralImpulse(impulse)
        body.applyTorqueImpulse((launchOrigin.rotation * launcherPhysicsRecoil.torqueImpulse).bullet())
    }
}
