@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeChar(override val layout: ValueLayout.OfChar = ValueLayout.JAVA_CHAR) : NativeValueLayout<Char> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Char {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Char
    ) {
        segment.set(layout, offset, value)
    }
}