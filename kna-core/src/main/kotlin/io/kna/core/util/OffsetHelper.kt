@file:Suppress("Unused", "Nothing_to_inline")
package io.kna.core.util
import java.lang.foreign.*

/* === PUBLIC OFFSET HELPER FUNCTIONS === */
/**
 * Reads a value of the specified type from this memory segment at the given offset.
 *
 * This generic function provides a unified way to read primitive types and pointers
 * from a memory segment without having to remember individual read functions.
 *
 * Example:
 * ```
 * val pid = processEntry.readAt<Int>(8)                // read Int at offset 8
 * val namePtr = processEntry.readAt<MemorySegment>(44) // read pointer at offset 44
 * ```
 *
 * @param T The type of value to read (must be supported: Byte, Short, Int, Long, Float, Double, or MemorySegment)
 * @param offset Byte offset within the segment
 *
 * @return The value at the specified offset, cast to type T
 *
 * @throws IllegalArgumentException if T is not a supported type
 */
inline fun <reified T> MemorySegment.readAt(offset: Long): T {
    return when (T::class) {
        Byte::class -> this.get(ValueLayout.JAVA_BYTE, offset) as T
        Short::class -> this.get(ValueLayout.JAVA_SHORT, offset) as T
        Int::class -> this.get(ValueLayout.JAVA_INT, offset) as T
        Long::class -> this.get(ValueLayout.JAVA_LONG, offset) as T
        Float::class -> this.get(ValueLayout.JAVA_FLOAT, offset) as T
        Double::class -> this.get(ValueLayout.JAVA_DOUBLE, offset) as T
        MemorySegment::class -> this.get(ValueLayout.ADDRESS, offset) as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

/**
 * Writes a value of the specified type to this memory segment at the given offset.
 *
 * This generic function provides a unified way to write primitive types and pointers
 * to a memory segment. It complements [readAt] for symmetric read/write operations.
 *
 * Example:
 * ```
 * buffer.writeAt(0, 42)                 // write Int at offset 0
 * buffer.writeAt(8, somePointer)        // write pointer at offset 8
 * ```
 *
 * @param T The type of value to write (must be supported: Byte, Short, Int, Long, Float, Double, or MemorySegment)
 * @param offset Byte offset within the segment
 * @param value The value to write
 *
 * @throws IllegalArgumentException if value type is not supported
 */
inline fun <reified T> MemorySegment.writeAt(offset: Long, value: T) {
    when (value) {
        is Byte -> this.set(ValueLayout.JAVA_BYTE, offset, value)
        is Short -> this.set(ValueLayout.JAVA_SHORT, offset, value)
        is Int -> this.set(ValueLayout.JAVA_INT, offset, value)
        is Long -> this.set(ValueLayout.JAVA_LONG, offset, value)
        is Float -> this.set(ValueLayout.JAVA_FLOAT, offset, value)
        is Double -> this.set(ValueLayout.JAVA_DOUBLE, offset, value)
        is MemorySegment -> this.set(ValueLayout.ADDRESS, offset, value)
        else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
    }
}