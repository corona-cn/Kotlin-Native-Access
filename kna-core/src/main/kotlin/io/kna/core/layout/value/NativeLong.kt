@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeLong(override val layout: ValueLayout.OfLong = ValueLayout.JAVA_LONG) : NativeValueLayout<Long> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Long {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Long
    ) {
        segment.set(layout, offset, value)
    }
}