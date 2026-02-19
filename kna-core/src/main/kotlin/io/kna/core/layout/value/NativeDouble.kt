@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeDouble(override val layout: ValueLayout.OfDouble = ValueLayout.JAVA_DOUBLE) : NativeValueLayout<Double> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Double {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Double
    ) {
        segment.set(layout, offset, value)
    }
}