package com.gitlab.aecsocket.calibre.paper

import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.bukkit.entity.Player

class PlayerState(private val plugin: Calibre, val player: Player) {
    val effector = SokolAPI.effectors.player(player)
}
