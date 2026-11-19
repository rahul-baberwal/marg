package com.rahulbaberwal.marg

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class IndoorLocationState(
    val point: P,
    val bearing: Float,
    val speedMps: Float,
    val accuracy: Float,
    val altitude: Double,
    val isWalking: Boolean,
    val offsetMeters: Double
)

/**
 * Simulates human presence inside a home or building:
 * - Alternates between resting/sitting in a room and walking between rooms.
 * - Confined within [maxRadiusM] of the home [center] (default ~10m).
 * - Smooth kinematic velocity updates (no teleporting).
 * - Realistic indoor GPS accuracy (6 - 16m), natural antenna multipath noise, and altitude variation.
 */
class IndoorWanderer(
    val center: P,
    var maxRadiusM: Double = 10.0
) {
    private var currentOffsetXM = 0.0 // meters East from center
    private var currentOffsetYM = 0.0 // meters North from center
    private var targetOffsetXM = 0.0
    private var targetOffsetYM = 0.0

    private var currentVx = 0.0 // m/s
    private var currentVy = 0.0 // m/s

    private var isWalking = false
    private var ticksRemaining = 5
    private var currentBearing = 0f
    private var altitudeBase = 25.0
    private var totalTicks = 0

    fun tick(): IndoorLocationState {
        totalTicks++
        ticksRemaining--

        if (maxRadiusM <= 0.5) {
            // Strictly stationary at center with tiny GPS receiver jitter (~0.2m)
            val noiseX = (Random.nextDouble() - 0.5) * 0.4
            val noiseY = (Random.nextDouble() - 0.5) * 0.4
            return buildState(noiseX, noiseY, 0f, currentBearing, false)
        }

        if (ticksRemaining <= 0) {
            isWalking = !isWalking
            if (isWalking) {
                // Pick a new room destination within home radius
                val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
                val dist = Random.nextDouble(1.0, maxRadiusM)
                targetOffsetXM = dist * cos(angle)
                targetOffsetYM = dist * sin(angle)
                ticksRemaining = Random.nextInt(4, 10) // walk for 4 to 10 seconds
            } else {
                // Rest in this room for 8 to 22 seconds (couch, desk, bed)
                ticksRemaining = Random.nextInt(8, 22)
            }
        }

        if (isWalking) {
            val dx = targetOffsetXM - currentOffsetXM
            val dy = targetOffsetYM - currentOffsetYM
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 0.6) {
                // Indoor walking pace: 0.75 to 1.15 m/s (~2.7 - 4.1 km/h)
                val targetSpeed = Random.nextDouble(0.75, 1.15)
                val targetVx = (dx / dist) * targetSpeed
                val targetVy = (dy / dist) * targetSpeed
                // Smooth acceleration with inertia
                currentVx = currentVx * 0.6 + targetVx * 0.4
                currentVy = currentVy * 0.6 + targetVy * 0.4
                currentBearing = ((Math.toDegrees(atan2(currentVx, currentVy)) + 360) % 360).toFloat()
            } else {
                // Reached room waypoint
                currentVx *= 0.3
                currentVy *= 0.3
                isWalking = false
                ticksRemaining = Random.nextInt(8, 20)
            }
        } else {
            // Stationary: dampen velocity to near 0
            currentVx *= 0.25
            currentVy *= 0.25
        }

        currentOffsetXM += currentVx
        currentOffsetYM += currentVy

        // Soft boundary reflection / clamping to keep strictly inside home radius
        val distFromCenter = sqrt(currentOffsetXM * currentOffsetXM + currentOffsetYM * currentOffsetYM)
        if (distFromCenter > maxRadiusM) {
            val factor = maxRadiusM / distFromCenter
            currentOffsetXM *= factor
            currentOffsetYM *= factor
            currentVx = -currentVx * 0.5
            currentVy = -currentVy * 0.5
        }

        // Multipath satellite noise (subtle +-0.3m)
        val noiseX = (Random.nextDouble() - 0.5) * 0.6
        val noiseY = (Random.nextDouble() - 0.5) * 0.6

        val speedMps = if (isWalking) sqrt(currentVx * currentVx + currentVy * currentVy).toFloat() else 0.0f
        return buildState(currentOffsetXM + noiseX, currentOffsetYM + noiseY, speedMps, currentBearing, isWalking)
    }

    fun tickStationary(): IndoorLocationState {
        totalTicks++
        // Subtle sensor noise while paused/stationary
        val noiseX = (Random.nextDouble() - 0.5) * 0.4
        val noiseY = (Random.nextDouble() - 0.5) * 0.4
        return buildState(currentOffsetXM + noiseX, currentOffsetYM + noiseY, 0f, currentBearing, false)
    }

    private fun buildState(
        offsetXM: Double,
        offsetYM: Double,
        speedMps: Float,
        bearing: Float,
        walking: Boolean
    ): IndoorLocationState {
        val latRad = Math.toRadians(center.lat)
        val latOffset = offsetYM / 111111.0
        val lonOffset = offsetXM / (111111.0 * cos(latRad).coerceAtLeast(0.01))

        // Natural indoor GPS accuracy fluctuates slowly between 7m and 15m
        val slowDrift = sin(totalTicks * 0.05) * 3.0
        val accuracy = (10.0 + slowDrift + Random.nextDouble(-1.5, 1.5)).toFloat().coerceIn(6f, 18f)

        // Subtle barometric / altitude flutter
        val altitude = altitudeBase + sin(totalTicks * 0.03) * 0.7 + Random.nextDouble(-0.2, 0.2)

        val totalOffset = sqrt(offsetXM * offsetXM + offsetYM * offsetYM)

        return IndoorLocationState(
            point = P(center.lat + latOffset, center.lon + lonOffset),
            bearing = bearing,
            speedMps = speedMps,
            accuracy = accuracy,
            altitude = altitude,
            isWalking = walking,
            offsetMeters = totalOffset
        )
    }
}

