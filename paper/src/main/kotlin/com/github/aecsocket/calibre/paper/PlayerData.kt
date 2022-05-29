package com.github.aecsocket.calibre.paper

import com.github.aecsocket.alexandria.core.bound.Ray
import com.github.aecsocket.alexandria.core.bound.Raycast
import com.github.aecsocket.alexandria.core.vector.Vector2
import com.github.aecsocket.alexandria.core.vector.Vector3
import com.github.aecsocket.alexandria.paper.bound.PaperBoundable
import com.github.aecsocket.alexandria.paper.effect.PaperEffectors
import com.github.aecsocket.alexandria.paper.extension.*
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Light
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

private const val EPSILON = 0.001
private const val TP_FLAGS = (0x01 or 0x02 or 0x04 or 0x08 or 0x10).toByte()

private fun offset(dir: Vector3, offset: Vector3): Vector3 {
    val xzTan = Vector3(-dir.z, 0.0, dir.x).normalized
    val yTan = xzTan.cross(dir).normalized
    return (xzTan * offset.x) + ((dir * offset.z) + (yTan * offset.y))
}

data class PlayerData(
    val plugin: CalibrePlugin,
    val player: Player
) {
    private val world: World
        get() = player.world

    data class Recoil(
        var toApply: Vector2 = Vector2.ZERO,
        var toRecover: Vector2 = Vector2.ZERO,
        var speed: Double = 0.0,
        var recover: Double = 0.0,
        var recoverSpeed: Double = 0.0,
        var recoverAfter: Long = 0
    )

    var holdingRClick = false
    val recoil = Recoil()

    fun applyRecoil(apply: Vector2, speed: Double, recover: Double, recoverSpeed: Double, recoverAfter: Long) {
        recoil.toApply = apply
        recoil.speed = speed
        recoil.recover = recover
        recoil.recoverSpeed = recoverSpeed
        recoil.recoverAfter = System.currentTimeMillis() + recoverAfter
    }

    fun syncTick(dt: Long) {
        if (holdingRClick) {
            val eyeLocation = player.eyeLocation
            val position = offset(eyeLocation.direction.alexandria(), plugin.settings.offset) + eyeLocation
            val zeroFocus = eyeLocation + (eyeLocation.direction * 1000.0)
            val direction = (zeroFocus - position).vector().normalized

            val thisBlock = eyeLocation.block

            val outdoors = thisBlock.lightFromSky / 15f

            val effector = plugin.effectors.world(world)
            val sounds = plugin.sounds
            plugin.settings.outdoorSounds.forEach { sounds.play(world, position, it.copy(volume = it.sound.volume() * outdoors)) }
            plugin.settings.indoorSounds.forEach { sounds.play(world, position, it.copy(volume = it.sound.volume() * (1 - outdoors))) }
            effector.apply {
                plugin.settings.fireParticle.forEach {
                    showParticle(it, position)
                }
            }

            if (thisBlock.type == Material.AIR) {
                val blockData = thisBlock.blockData
                player.sendBlockChange(eyeLocation, Material.LIGHT.createBlockData {
                    (it as Light).level = 15
                })
                plugin.scheduleDelayed(1L) {
                    player.sendBlockChange(eyeLocation, blockData)
                }
            }

            val rng = ThreadLocalRandom.current()
            applyRecoil(
                plugin.settings.recoil + Vector2(rng.nextGaussian(), rng.nextGaussian()) * plugin.settings.recoilRandom,
                plugin.settings.recoilSpeed,
                plugin.settings.recoilRecover,
                plugin.settings.recoilRecoverSpeed,
                plugin.settings.recoilRecoverAfter
            )

            run {
                val raycaster = plugin.raycast(world)

                var projPosition = position
                var projVelocity = direction * plugin.settings.speed
                var leftPlayer = false

                fun tick(dt: Long) {
                    if (!world.isChunkLoaded((projPosition.x / 16).toInt(), (projPosition.z / 16).toInt()))
                        return

                    val step = dt / 1000.0
                    val speed = projVelocity.length
                    val projDirection = projVelocity.normalized
                    val distance = speed * step

                    val raycast = raycaster.cast(Ray(projPosition, projDirection), distance)
                    var stepped = 0.0
                    val particleStep = 4.0
                    while (stepped < raycast.travelled) {
                        val particlePos = projPosition + projDirection * stepped
                        effector.apply {
                            plugin.settings.trailParticle.forEach {
                                showParticle(it.copy(size = projVelocity), particlePos)
                            }
                        }
                        stepped += particleStep
                    }
                    when (raycast) {
                        is Raycast.Result.Hit -> {
                            when (val hit = raycast.hit) {
                                is PaperBoundable.OfEntity -> {
                                    val entity = hit.entity
                                    if (entity == player && !leftPlayer) {
                                        projPosition = raycast.posOut + projDirection * 0.001
                                        leftPlayer = true
                                        plugin.scheduleDelayed(1) { tick(50) }
                                    } else if (
                                        entity is LivingEntity && entity.isValid
                                        && (entity !is Player || entity.gameMode.survival)
                                    ) {
                                        entity.damage(0.0001, player)
                                        entity.velocity = Vector(0.0, 0.0, 0.0)
                                        entity.noDamageTicks = 0
                                        entity.health = max(0.0, entity.health - plugin.settings.damage)
                                        effector.apply {
                                            plugin.settings.entityHitParticle.forEach {
                                                showParticle(it, raycast.position)
                                            }
                                        }
                                    }
                                }
                                is PaperBoundable.OfBlock -> {
                                    val block = hit.block
                                    world.players.forEach { recv ->
                                        // TODO if chunk tracked
                                        recv.sendBlockDamage(rng.nextInt(), block.location.point(), 4)
                                        effector.apply {
                                            plugin.settings.impactParticle.forEach {
                                                val data = if (it.data == null && PaperEffectors.particleByKey(it.particle)?.dataType == BlockData::class.java) {
                                                    it.copy(data = block.blockData)
                                                } else it
                                                showParticle(data, raycast.position)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is Raycast.Result.Miss -> {
                            projPosition = raycast.position
                            projVelocity = projVelocity.y(projVelocity.y - (9.8 * step))
                            plugin.scheduleDelayed(1) { tick(50) }
                        }
                    }
                }

                plugin.scheduleDelayed { tick(50) }
            }

            if (player.gameMode.survival) {
                val radius = plugin.settings.entityTargetRadius
                world.getNearbyEntities(eyeLocation, radius, radius, radius).forEach { entity ->
                    if (entity is Monster && entity.target == null) {
                        entity.target = player
                    }
                }
            }
        }
    }

    fun asyncTick(dt: Long) {
        var applied = recoil.toApply * recoil.speed
        recoil.toApply *= 1 - recoil.speed
        recoil.toRecover += applied * recoil.recover

        if (System.currentTimeMillis() >= recoil.recoverAfter) {
            applied -= recoil.toRecover * recoil.recoverSpeed
            recoil.toRecover *= 1 - recoil.recoverSpeed
        }

        if (applied.manhattanLength >= EPSILON) {
            PacketEvents.getAPI().playerManager.sendPacketAsync(player, WrapperPlayServerPlayerPositionAndLook(
                0.0, 0.0, 0.0, applied.x.toFloat(), -applied.y.toFloat(), TP_FLAGS, 0, false
            ))
        }
    }
}

val GameMode.survival: Boolean
    get() = this == GameMode.SURVIVAL || this == GameMode.ADVENTURE
