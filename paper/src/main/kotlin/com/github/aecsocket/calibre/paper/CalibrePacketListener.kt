package com.github.aecsocket.calibre.paper

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.entity.Player

internal class CalibrePacketListener(
    private val plugin: CalibrePlugin
) : PacketListenerAbstract() {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        /*val player = event.player
        if (player !is Player)
            return
        val playerData = plugin.playerData(player)
        when (event.packetType) {
            PacketType.Play.Client.USE_ITEM -> {
                playerData.holdingRClick = true
            }
            PacketType.Play.Client.PLAYER_DIGGING -> {
                playerData.holdingRClick = false
            }
        }*/
    }
}
