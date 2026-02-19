@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeInt(override val layout: ValueLayout.OfInt = ValueLayout.JAVA_INT) : NativeValueLayout<Int> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Int {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Int
    ) {
        segment.set(layout, offset, value)
    }
}