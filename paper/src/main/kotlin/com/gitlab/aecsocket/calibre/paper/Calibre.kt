package com.gitlab.aecsocket.calibre.paper

import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.paper.effect.SimulatedSounds
import com.gitlab.aecsocket.alexandria.paper.plugin.BasePlugin
import com.github.retrooper.packetevents.PacketEvents
import com.gitlab.aecsocket.alexandria.paper.plugin.ConfigOptionsAction
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bstats.bukkit.Metrics
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

private const val BSTATS_ID = 10479

private lateinit var instance: Calibre
val CalibreAPI get() = instance

class Calibre : BasePlugin<Calibre.LoadScope>() {
    interface LoadScope : BasePlugin.LoadScope

    override fun createLoadScope(configOptionActions: MutableList<ConfigOptionsAction>) = object : LoadScope {
        override fun onConfigOptionsSetup(action: ConfigOptionsAction) {
            configOptionActions.add(action)
        }
    }

    private val _playerState = HashMap<Player, PlayerState>()
    val playerState: Map<Player, PlayerState> = _playerState

    val sounds = SimulatedSounds(this, { SokolAPI.effectors.player(it) })
    lateinit var settings: CalibreSettings

    fun playerState(player: Player) = _playerState.computeIfAbsent(player) { PlayerState(this, it) }

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

        PacketEvents.getAPI().eventManager.registerListener(CalibrePacketListener(this@Calibre))
        PacketEvents.getAPI().init()
        CalibreCommand(this)
    }

    override fun onDisable() {
        super.onDisable()
        PacketEvents.getAPI().terminate()
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
            try {
                this.settings = settings.force()
            } catch (ex: SerializationException) {
                log.line(LogLevel.Error, ex) { "Could not load settings" }
                return false
            }

            if (this.settings.enableBstats) {
                Metrics(this, BSTATS_ID)
            }
            return true
        }
        return false
    }
}
