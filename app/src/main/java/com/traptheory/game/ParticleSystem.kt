package com.traptheory.game

import kotlin.math.cos
import kotlin.math.sin
import java.util.Random

class ParticleSystem(capacity: Int = 180) {
    val particles: Array<Particle> = Array(capacity) { Particle() }
    private val random = Random(3127L)
    private var nextIndex = 0

    fun emit(x: Float, y: Float, color: Int, count: Int, speed: Float = 180f) {
        repeat(count.coerceAtMost(particles.size)) {
            val particle = particles[nextIndex]
            nextIndex = (nextIndex + 1) % particles.size
            val angle = random.nextFloat() * 6.2831855f
            val velocity = speed * (0.3f + random.nextFloat() * 0.7f)
            particle.x = x
            particle.y = y
            particle.velocityX = cos(angle) * velocity
            particle.velocityY = sin(angle) * velocity
            particle.life = 0.35f + random.nextFloat() * 0.65f
            particle.maxLife = particle.life
            particle.size = 2.5f + random.nextFloat() * 5f
            particle.color = color
            particle.active = true
        }
    }

    fun update(deltaSeconds: Float) {
        for (particle in particles) {
            if (!particle.active) continue
            particle.life -= deltaSeconds
            if (particle.life <= 0f) {
                particle.active = false
                continue
            }
            particle.velocityY += 330f * deltaSeconds
            particle.x += particle.velocityX * deltaSeconds
            particle.y += particle.velocityY * deltaSeconds
            particle.velocityX *= 0.988f
        }
    }

    fun clear() {
        particles.forEach { it.active = false }
    }
}

class Particle {
    var x = 0f
    var y = 0f
    var velocityX = 0f
    var velocityY = 0f
    var life = 0f
    var maxLife = 1f
    var size = 1f
    var color = 0
    var active = false
}