@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeShort(override val layout: ValueLayout.OfShort = ValueLayout.JAVA_SHORT) : NativeValueLayout<Short> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Short {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Short
    ) {
        segment.set(layout, offset, value)
    }
}