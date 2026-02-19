@file:Suppress("Unused")
package io.kna.core.layout.struct
import io.kna.core.layout.*

import java.lang.foreign.*

import kotlin.reflect.*

class NativeStructLayout(structKClass: KClass<out NativeStruct>) : NativeLayout<MemorySegment> {
    override val layout: StructLayout = NativeStruct.getStructLayout(structKClass)

    override fun read(
        segment: MemorySegment,
        offset: Long
    ): MemorySegment {
        return segment.get(ValueLayout.ADDRESS, offset)
    }

    override fun write(
        segment: MemorySegment,
        offset: Long,
        value: MemorySegment
    ) {
        segment.set(ValueLayout.ADDRESS, offset, value)
    }
}