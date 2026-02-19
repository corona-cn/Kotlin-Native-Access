@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeFloat(override val layout: ValueLayout.OfFloat = ValueLayout.JAVA_FLOAT) : NativeValueLayout<Float> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Float {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Float
    ) {
        segment.set(layout, offset, value)
    }
}