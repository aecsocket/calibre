package com.gitlab.aecsocket.calibre.paper

import com.github.aecsocket.alexandria.core.bound.Raycast
import com.github.aecsocket.alexandria.core.vector.Vector3
import com.github.aecsocket.alexandria.paper.bound.PaperBoundable
import com.github.aecsocket.alexandria.paper.extension.scheduleDelayed
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import kotlin.math.abs

private const val EPSILON = 0.1

class PaperProjectile(
    private val plugin: Calibre,
    val world: World,
    source: PaperBoundable?,
    position: Vector3,
    velocity: Vector3,
    physics: Physics,
    fluid: Fluid<PaperBoundable>
) : Projectile<PaperBoundable>(
    plugin.raycast(world),
    source,
    position,
    velocity,
    physics,
    fluid
) {
    override fun computeCollision(dt: Double, result: Raycast.Result<PaperBoundable>) {
        when (result) {
            is Raycast.Result.Hit -> {
                val direction = result.ray.direction
                val epsilon = direction * EPSILON
                position = result.posIn - epsilon

                // apply force

                // enter/exit
                if (!fluid.sameAs(result.hit)) {
                    if (result.hit.fluid =)
                    when (result.hit.fluid) {
                        Material.WATER -> {

                        }
                    }
                }

                // calculate resultant velocity
                if (abs(direction.dot(result.normal)) < 0.2) {
                    // ricochet
                    velocity = Vector3.reflect(velocity, result.normal) * 0.8
                } else {

                }
            }
            is Raycast.Result.Miss -> {
                position = result.position
            }
        }
    }

    fun tick(dt: Double) {
        // todo
        Bukkit.getOnlinePlayers().forEach { if (it.isSneaking) return }

        val from = position

        updatePhysics(dt)

        if (!Location(world, position.x, position.y, position.z).isChunkLoaded)
            return

        computeCollisions(dt)

        val to = position
        val delta = to - from

        val direction = delta.normalized
        val effector = plugin.effectors.world(world)
        val particles = plugin.settings.trailParticle.map { it.copy(size = velocity) }

        var step = 0.0
        while (step < delta.length) {
            particles.forEach { effector.showParticle(it, from + direction * step) }
            step += 1.0
        }

        plugin.scheduleDelayed(1) { tick(0.05) }
    }
}
