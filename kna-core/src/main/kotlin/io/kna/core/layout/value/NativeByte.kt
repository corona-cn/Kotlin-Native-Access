@file:Suppress("Unused")
package io.kna.core.layout.value
import java.lang.foreign.*

@JvmInline
value class NativeByte(override val layout: ValueLayout.OfByte = ValueLayout.JAVA_BYTE) : NativeValueLayout<Byte> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): Byte {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: Byte
    ) {
        segment.set(layout, offset, value)
    }
}