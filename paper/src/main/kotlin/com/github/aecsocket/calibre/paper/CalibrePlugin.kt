package com.github.aecsocket.calibre.paper

import com.github.aecsocket.alexandria.core.Input
import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.LogList
import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import com.github.aecsocket.alexandria.core.effect.SoundEffect
import com.github.aecsocket.alexandria.core.extension.MS_PER_TICK
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.vector.Vector2
import com.github.aecsocket.alexandria.core.vector.Vector3
import com.github.aecsocket.alexandria.paper.bound.PaperRaycast
import com.github.aecsocket.alexandria.paper.effect.PaperEffectors
import com.github.aecsocket.alexandria.paper.effect.SimulatedSounds
import com.github.aecsocket.alexandria.paper.extension.registerEvents
import com.github.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.github.aecsocket.alexandria.paper.packet.PacketInputListener
import com.github.aecsocket.alexandria.paper.plugin.BasePlugin
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException

class CalibrePlugin : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val recoil: Vector2,
        val recoilRandom: Vector2,
        val recoilSpeed: Double,
        val recoilRecover: Double,
        val recoilRecoverSpeed: Double,
        val recoilRecoverAfter: Long,
        val entityTargetRadius: Double,
        val damage: Double,
        val speed: Double,
        val offset: Vector3,
        val indoorSounds: List<SoundEffect>,
        val outdoorSounds: List<SoundEffect>,
        val fireParticle: List<ParticleEffect>,
        val trailParticle: List<ParticleEffect>,
        val impactParticle: List<ParticleEffect>,
        val entityHitParticle: List<ParticleEffect>
    )

    private val players = HashMap<Player, PlayerData>()
    lateinit var effectors: PaperEffectors
    lateinit var sounds: SimulatedSounds
    lateinit var settings: Settings

    override fun onLoad() {
        super.onLoad()
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings
            .checkForUpdates(false)
            .bStats(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        super.onEnable()
        effectors = PaperEffectors(this)
        sounds = SimulatedSounds(this, effectors::player)
        PacketEvents.getAPI().eventManager.apply {
            registerListener(CalibrePacketListener(this@CalibrePlugin))
            registerListener(PacketInputListener { event ->
                val player = event.player
                when (val input = event.input) {
                    is Input.Mouse -> {
                        if (input.button == Input.MOUSE_RIGHT) {
                            when (input.state) {
                                Input.MOUSE_DOWN -> true
                                Input.MOUSE_UP -> false
                                else -> null
                            }?.let { playerData(event.player).holdingRClick = it }
                        }
                    }
                }
            }, PacketListenerPriority.NORMAL)
        }
        PacketEvents.getAPI().init()
        CalibreCommand(this)
        registerEvents(CalibreListener(this))

        scheduleRepeating { syncTick(MS_PER_TICK) }

        Thread {
            var last = System.currentTimeMillis()
            while (true) {
                while (System.currentTimeMillis() < last + 10) {
                    Thread.sleep(1)
                }
                val time = System.currentTimeMillis()
                asyncTick(time - last)
                last = time
            }
        }.start()

        DemoInspectView(this)
    }

    override fun onDisable() {
        super.onDisable()
        PacketEvents.getAPI().terminate()
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {

            log.line(LogLevel.VERBOSE) { "Verbose" }
            log.line(LogLevel.INFO) { "Info" }
            log.line(LogLevel.WARNING) { "Warning" }
            log.line(LogLevel.ERROR) { "Error" }

            try {
                this.settings = settings.force()
            } catch (ex: SerializationException) {
                log.line(LogLevel.ERROR, ex) { "Could not load settings" }
                return false
            }
            return true
        }
        return false
    }

    fun playerData(player: Player) = players.computeIfAbsent(player) { PlayerData(this, it) }

    internal fun removePlayerData(player: Player) = players.remove(player)

    fun raycast(world: World) = PaperRaycast(world, 2.0)

    private fun syncTick(dt: Long) {
        if (Bukkit.getCurrentTick() % 2 != 0)
            return

        Bukkit.getOnlinePlayers().forEach { player ->
            playerData(player).syncTick(dt)
        }
    }

    private fun asyncTick(dt: Long) {
        players.forEach { (_, playerData) ->
            playerData.asyncTick(dt)
        }
    }
}
