@file:Suppress("Unused", "Nothing_to_inline")
package io.kna.core.util
import java.lang.foreign.*

/* === PUBLIC READER FUNCTIONS === */
/**
 * Reads a signed 8-bit byte from a memory segment at the specified offset.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return The byte value at the given offset
 */
inline fun readByte(segment: MemorySegment, offset: Long = 0): Byte {
    return segment.get(ValueLayout.JAVA_BYTE, offset)
}

/**
 * Reads a signed 16-bit short from a memory segment at the specified offset.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return The short value at the given offset
 */
inline fun readShort(segment: MemorySegment, offset: Long = 0): Short {
    return segment.get(ValueLayout.JAVA_SHORT, offset)
}

/**
 * Reads a signed 32-bit integer from a memory segment at the specified offset.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return The integer value at the given offset
 */
inline fun readInt(segment: MemorySegment, offset: Long = 0): Int {
    return segment.get(ValueLayout.JAVA_INT, offset)
}

/**
 * Reads a signed 64-bit long from a memory segment at the specified offset.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return The long value at the given offset
 */
inline fun readLong(segment: MemorySegment, offset: Long = 0): Long {
    return segment.get(ValueLayout.JAVA_LONG, offset)
}

/**
 * Reads a 32-bit float from a memory segment at the specified offset.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return The float value at the given offset
 */
inline fun readFloat(segment: MemorySegment, offset: Long = 0): Float {
    return segment.get(ValueLayout.JAVA_FLOAT, offset)
}

/**
 * Reads a 64-bit double from a memory segment at the specified offset.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return The double value at the given offset
 */
inline fun readDouble(segment: MemorySegment, offset: Long = 0): Double {
    return segment.get(ValueLayout.JAVA_DOUBLE, offset)
}

/**
 * Reads a memory address (pointer) from a memory segment at the specified offset.
 *
 * The returned MemorySegment represents the pointer value itself, not the data it points to.
 * Use this function when a struct contains a pointer field that you need to dereference later.
 *
 * @param segment The memory segment to read from
 * @param offset Byte offset within the segment (default: 0)
 *
 * @return A MemorySegment representing the pointer value
 */
inline fun readPointer(segment: MemorySegment, offset: Long = 0): MemorySegment {
    return segment.get(ValueLayout.ADDRESS, offset)
}

/**
 * Reads a null-terminated UTF-16 (wide) string from a memory segment.
 *
 * This function interprets the memory as a sequence of 16-bit characters
 * and stops at the first null terminator ('\0'). It assumes the string
 * is encoded in UTF-16LE (standard for Windows APIs).
 *
 * Example:
 * ```
 * val message = readWideString(segment, 44) // read from offset 44
 * ```
 *
 * @param segment The memory segment containing the string
 * @param offset Byte offset where the string begins (default: 0)
 *
 * @return The decoded string, or empty string if the first character is null
 */
fun readWideString(segment: MemorySegment, offset: Long = 0): String {
    val start = if (offset == 0L) segment else segment.asSlice(offset)
    val maxLen = 1024

    return buildString {
        for (i in 0 until maxLen step 2) {
            val char = start.get(ValueLayout.JAVA_CHAR, i.toLong())
            if (char == '\u0000') {
                break
            }
            append(char)
        }
    }
}