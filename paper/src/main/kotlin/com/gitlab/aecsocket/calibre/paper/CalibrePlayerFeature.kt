package com.gitlab.aecsocket.calibre.paper

import com.gitlab.aecsocket.alexandria.paper.AlexandriaPlayer
import com.gitlab.aecsocket.alexandria.paper.PlayerFeature
import com.gitlab.aecsocket.alexandria.paper.alexandria
import org.bukkit.entity.Player

class CalibrePlayerFeature(
    private val calibre: Calibre
) : PlayerFeature<CalibrePlayerFeature.PlayerData> {
    class PlayerData(

    ) : PlayerFeature.PlayerData

    override fun createFor(player: AlexandriaPlayer) = PlayerData()
}

val Player.calibre get() = alexandria.featureData(CalibreAPI.players)
