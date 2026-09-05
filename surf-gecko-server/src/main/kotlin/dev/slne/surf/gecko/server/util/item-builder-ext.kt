package dev.slne.surf.gecko.server.util

import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

fun <T> ItemStack.Builder.withTag(tag: Tag<T>, value: T) = apply {
    this.setTag(tag, value)
}