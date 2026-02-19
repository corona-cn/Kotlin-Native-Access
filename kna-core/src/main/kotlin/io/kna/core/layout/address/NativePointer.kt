@file:Suppress("Unused")
package io.kna.core.layout.address
import java.lang.foreign.*

@JvmInline
value class NativePointer(override val layout: AddressLayout = ValueLayout.ADDRESS) : NativeAddressLayout<MemorySegment?> {
    override fun read(
        segment: MemorySegment,
        offset: Long
    ): MemorySegment {
        return segment.get(layout, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: MemorySegment?
    ) {
        segment.set(layout, offset, value ?: MemorySegment.NULL)
    }
}