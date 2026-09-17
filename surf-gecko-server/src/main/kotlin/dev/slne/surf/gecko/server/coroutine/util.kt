package dev.slne.surf.gecko.server.coroutine

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val Int.ticks: Duration get() = (this * 50L - 25).milliseconds