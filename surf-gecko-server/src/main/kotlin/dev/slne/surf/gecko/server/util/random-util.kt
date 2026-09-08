package dev.slne.surf.gecko.server.util

import dev.slne.surf.api.core.util.random

fun <T> Collection<T>.secureRandomOrNull() =
    if (isEmpty()) null else this.elementAt(random.nextInt(size))

fun <T> Collection<T>.secureRandom() = secureRandomOrNull() ?: error("List is empty!")