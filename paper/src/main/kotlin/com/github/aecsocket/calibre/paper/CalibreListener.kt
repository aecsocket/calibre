package com.github.aecsocket.calibre.paper

import com.github.aecsocket.alexandria.core.effect.SoundEffect
import com.github.aecsocket.alexandria.paper.extension.alexandria
import com.github.aecsocket.alexandria.paper.extension.scheduleDelayed
import net.kyori.adventure.key.Key.key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.sound
import org.bukkit.Material
import org.bukkit.block.data.type.Light
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

internal class CalibreListener(private val plugin: CalibrePlugin) : Listener {
    @EventHandler
    fun onEvent(event: PlayerQuitEvent) {
        plugin.removePlayerData(event.player)
    }

    @EventHandler
    fun onEvent(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND)
            return

    }
}
