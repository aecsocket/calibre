package com.gitlab.aecsocket.calibre.paper

import com.github.aecsocket.alexandria.core.bound.Boundable
import com.github.aecsocket.alexandria.core.bound.Ray
import com.github.aecsocket.alexandria.core.bound.Raycast
import com.github.aecsocket.alexandria.core.vector.Vector3
import com.gitlab.aecsocket.alexandria.core.physics.Raycast
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import kotlin.math.min

const val GRAVITY = 9.81 // m/s^2

abstract class Projectile<B : Boundable>(
    val raycast: Raycast<B>,
    val source: B?,
    var position: Vector3,
    var velocity: Vector3,
    val physics: Physics,
    var fluid: Fluid<B>
) {
    private var leftSource = false

    data class Physics(
        val gravity: Double = GRAVITY, // m/s^2
        val dragCoeff: Double,
        val mass: Double // kg
    )

    interface Fluid<B : Boundable> {
        val density: Double // kg/m^3

        fun sameAs(boundable: B): Boolean
    }

    protected open fun computeAcceleration(dt: Double): Vector3 {
        val speed = velocity.length
        val direction = velocity.normalized

        // gravity
        var res = Vector3(0.0, -physics.gravity, 0.0)

        // drag acceleration magnitude
        // dragForce = 0.5 * (density * speed^2 * area * dragCoeff)
        // dragDeceleration = dragForce / mass
        val dragMagnitude = (0.5 * (fluid.density * speed*speed * physics.dragCoeff)) / physics.mass
        // · applied in the opposite direction to our current velocity
        // · if more than the speed, only cancel out the speed (don't invert the velocity direction)
        val drag = -direction * min(speed / dt, dragMagnitude)

        res += drag

        return res
    }

    protected open fun updatePhysics(dt: Double) {
        val acceleration = computeAcceleration(dt)
        velocity += acceleration * dt
    }

    protected open fun computeCollisions(dt: Double) {
        if (velocity.manhattanLength > 0) {
            val direction = velocity.normalized
            val speed = velocity.length
            val step = speed * dt
            val result = raycast.cast(Ray(position, direction), step) {
                (leftSource || it != source) && !fluid.sameAs(it)
            }
            leftSource = true
            computeCollision(dt, result)
        }
    }

    protected abstract fun computeCollision(dt: Double, result: Raycast.Result<B>)
}
