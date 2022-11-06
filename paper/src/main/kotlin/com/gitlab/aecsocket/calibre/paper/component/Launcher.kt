package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleDelayed
import com.gitlab.aecsocket.alexandria.paper.extension.trackedPlayers
import com.gitlab.aecsocket.calibre.paper.Calibre
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import org.bukkit.Material
import org.bukkit.block.data.type.Light
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable

private const val NEXT_AVAILABLE_AT = "next_available_at"

data class Launcher(
    val profile: Profile,
    private val dNextAvailableAt: Delta<Long>,
): PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("launcher")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Launcher::class
    override val key get() = Key

    override val dirty get() = dNextAvailableAt.dirty

    constructor(
        profile: Profile,
        nextAvailableAt: Long,
    ) : this(profile, Delta(nextAvailableAt))

    var nextAvailableAt by dNextAvailableAt

    lateinit var launchTransform: Transform

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(NEXT_AVAILABLE_AT) { makeLong(nextAvailableAt) }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()
        dNextAvailableAt.ifDirty { compound.set(NEXT_AVAILABLE_AT) { makeLong(it) } }
        return tag
    }

    override fun write(node: ConfigurationNode) {
        node.node(NEXT_AVAILABLE_AT).set(nextAvailableAt)
    }

    @ConfigSerializable
    data class Profile(
        val launchInterval: Long = 0L,
        val lightCreated: Int = 0,
        val effectLaunch: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val soundLaunch: SoundEngineEffect = SoundEngineEffect.Empty,
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound { compound -> Launcher(this,
            compound.get(NEXT_AVAILABLE_AT) { asLong() }) }

        override fun read(node: ConfigurationNode) = Launcher(this,
            node.node(NEXT_AVAILABLE_AT).get { 0L })

        override fun readEmpty() = Launcher(this, 0L)
    }
}

@All(Launcher::class, LaunchOrigin::class)
@After(LaunchOriginTarget::class)
class LauncherSystem(
    private val calibre: Calibre,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val LaunchStart = CalibreAPI.key("launcher/launch_start")
        val LaunchStop = CalibreAPI.key("launcher/launch_stop")
        val Launch = CalibreAPI.key("launcher/launch")
    }

    private val mLauncher = mappers.componentMapper<Launcher>()
    private val mLaunchOrigin = mappers.componentMapper<LaunchOrigin>()
    private val mComposite = mappers.componentMapper<Composite>()

    private fun build(entity: SokolEntity) {
        val launcher = mLauncher.get(entity)

        val (launchTransform) = mComposite.forwardAll(entity, Build())

        launcher.launchTransform = launchTransform
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        build(entity)
    }

    @Subscribe
    fun on(event: CompositeSystem.TreeMutate, entity: SokolEntity) {
        build(entity)
    }

    @Subscribe
    fun on(event: Launch, entity: SokolEntity) {
        val launcher = mLauncher.get(entity)
        val player = event._player

        val world = player.world
        val launchOrigin = event.launchOrigin.translation.location(world)
        AlexandriaAPI.soundEngine.play(launchOrigin, launcher.profile.soundLaunch)
        AlexandriaAPI.particleEngine.spawn(launchOrigin, launcher.profile.effectLaunch)
        launcher.nextAvailableAt = System.currentTimeMillis() + launcher.profile.launchInterval

        if (launcher.profile.lightCreated > 0 && launchOrigin.block.type == Material.AIR) {
            val lightData = Material.LIGHT.createBlockData { blockData ->
                (blockData as Light).level = launcher.profile.lightCreated
            }

            launchOrigin.chunk.trackedPlayers().forEach { target ->
                target.sendBlockChange(launchOrigin, lightData)
            }

            calibre.scheduleDelayed(2) {
                launchOrigin.block.state.update(true)
            }
        }
    }

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val launcher = mLauncher.get(entity)
        val launchOrigin = mLaunchOrigin.get(entity)

        event.addAction(LaunchStart) { (player, _, cancel) ->
            cancel()
            if (System.currentTimeMillis() < launcher.nextAvailableAt) return@addAction true

            val launchOrigin = launchOrigin.transform + launcher.launchTransform

            entity.call(Launch(player, launchOrigin))
            true
        }
    }

    data class Build(var launchOrigin: Transform = Transform.Identity) : SokolEvent

    // todo remove player from here
    data class Launch(val _player: Player, val launchOrigin: Transform) : SokolEvent
}
