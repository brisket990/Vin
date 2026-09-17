package com.xothiques.vin.util

import com.xothiques.vin.data.remote.dto.BottleDto
import java.time.Duration
import java.time.Instant

/** Mirrors backend/src/bottle/bottle.service.ts TURN_REMINDER_THRESHOLD_DAYS --
 *  keep both in sync if this changes. Classic advice for a bottle stored
 *  lying down under natural cork for a long time: give it a quarter turn
 *  every few months so sediment/the cork don't always settle on one side. */
const val TURN_REMINDER_THRESHOLD_DAYS = 90L

private fun BottleDto.turnBaselineInstant(): Instant? =
    runCatching { Instant.parse(lastTurnedAt ?: createdAt) }.getOrNull()

/** True once an in-cellar bottle has gone TURN_REMINDER_THRESHOLD_DAYS or
 *  more without being (re)turned. Mirrors BottleService.findNeedingTurn
 *  server-side for an immediate client-side check (e.g. on the detail
 *  screen) without a round-trip; the authoritative list still comes from
 *  GET /bottles/needing-turn. */
fun BottleDto.needsTurn(): Boolean {
    if (status != "in_cellar") return false
    val baseline = turnBaselineInstant() ?: return false
    return Duration.between(baseline, Instant.now()).toDays() >= TURN_REMINDER_THRESHOLD_DAYS
}

/** Days since this bottle was last turned (or since it was added, if never
 *  explicitly turned) -- for display ("Tournée il y a 42 jours"). */
fun BottleDto.daysSinceLastTurn(): Long? =
    turnBaselineInstant()?.let { Duration.between(it, Instant.now()).toDays() }
