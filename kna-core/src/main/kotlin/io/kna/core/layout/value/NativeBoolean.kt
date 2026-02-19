@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeBoolean(override val layout: ValueLayout.OfBoolean = ValueLayout.JAVA_BOOLEAN) : NativeValueLayout<Boolean> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Boolean {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Boolean
    ) {
        segment.set(layout, offset, value)
    }
}