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
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class Launcher(val profile: Profile): PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("launcher")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Launcher::class
    override val key get() = Key

    lateinit var launchOrigin: Transform

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val lightCreated: Int = 0,
        val effectLaunch: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val soundLaunch: SoundEngineEffect = SoundEngineEffect.Empty,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = Launcher(this)
    }
}

@All(Launcher::class, PositionRead::class)
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
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mComposite = mappers.componentMapper<Composite>()

    private fun build(entity: SokolEntity) {
        val launcher = mLauncher.get(entity)

        val (launchOrigin) = mComposite.forwardAll(entity, Build())

        launcher.launchOrigin = launchOrigin
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
        val launcher = mLauncher.get(entity).profile
        val player = event._player

        val world = player.world
        val launchOrigin = event.launchOrigin.translation.location(world)
        AlexandriaAPI.soundEngine.play(launchOrigin, launcher.soundLaunch)
        AlexandriaAPI.particleEngine.spawn(launchOrigin, launcher.effectLaunch)

        if (launcher.lightCreated > 0 && launchOrigin.block.type == Material.AIR) {
            val lightData = Material.LIGHT.createBlockData { blockData ->
                (blockData as Light).level = launcher.lightCreated
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
        val positionRead = mPositionRead.get(entity)

        event.addAction(LaunchStart) { (player, _, cancel) ->
            cancel()
            val launchOrigin = positionRead.transform + launcher.launchOrigin

            entity.call(Launch(player, launchOrigin))
            true
        }
    }

    data class Build(var launchOrigin: Transform = Transform.Identity) : SokolEvent

    // todo remove player from here
    data class Launch(val _player: Player, val launchOrigin: Transform) : SokolEvent
}
