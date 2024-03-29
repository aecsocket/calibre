package com.gitlab.aecsocket.calibre.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BasePlugin
import com.gitlab.aecsocket.alexandria.paper.BasePluginSettings
import com.gitlab.aecsocket.alexandria.paper.PluginManifest
import com.gitlab.aecsocket.calibre.paper.component.Launcher
import com.gitlab.aecsocket.calibre.paper.component.LaserEffects
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.TextColor
import org.bstats.bukkit.Metrics
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

private const val BSTATS_ID = 10479

private lateinit var instance: Calibre
val CalibreAPI get() = instance

class Calibre : BasePlugin<Calibre.Settings>(PluginManifest("calibre",
    accentColor = TextColor.color(0xd75e50),
    langPaths = listOf(
        "lang/default_en-US.conf"
    ),
    savedPaths = listOf(
        "settings.conf"
    )
), Settings::class, Settings()
) {
    @ConfigSerializable
    data class Settings(
        override val logLevel: LogLevel = LogLevel.Verbose,
        val enableBstats: Boolean = true,
    ) : BasePluginSettings

    init {
        instance = this
    }

    val players = CalibrePlayerFeature(this)

    override fun onEnable() {
        super.onEnable()
        CalibreCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = { ctx ->
                ctx.serializers
            },
            onLoad = { ctx ->
                ctx.addDefaultI18N()
            }
        )
        SokolAPI.registerConsumer(
            onInit = { ctx ->
                Launcher.init(ctx)
                LaserEffects.init(ctx)
            }
        )
        PacketEvents.getAPI().eventManager.registerListener(object : PacketListenerAbstract() {
            override fun onPacketReceive(event: PacketReceiveEvent) {
                val player = event.player as? Player ?: return
                //player.sendMessage("pkt ${event.packetType}")
            }
        })
    }

    override fun loadInternal(log: LogList): Boolean {
        if (!super.loadInternal(log)) return false

        if (settings.enableBstats) {
            Metrics(this, BSTATS_ID)
        }

        return true
    }
}
