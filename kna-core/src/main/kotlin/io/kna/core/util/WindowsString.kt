@file:Suppress("Unused", "Nothing_to_inline")
package io.kna.core.util
import java.lang.foreign.*
import java.nio.charset.*

/* === PUBLIC STRING HELPER FUNCTIONS === */
/**
 * Converts a Kotlin string to a Windows UTF-16LE (wide) string in native memory.
 *
 * Windows APIs that accept `LPCWSTR` (wide string pointers) expect
 * null-terminated UTF-16LE encoded strings. This function handles the
 * encoding and memory allocation automatically.
 *
 * Example:
 *
 * ```kotlin
 * Arena.ofConfined().use { arena ->
 *     val wide = "Hello World".toWideString(arena)
 *     user32.MessageBoxW(NULL, wide, NULL, 0)
 * }
 * ```
 *
 * or
 *
 * ```kotlin
 * val wide = "Hello World".toWideString() // arena = Arena.ofAuto()
 * user32.MessageBoxW(NULL, wide, NULL, 0)
 * ```
 *
 * @param arena The memory arena that will manage the allocated memory
 *
 * @return A memory segment containing the null-terminated UTF-16LE string
 */
inline fun String.toWideString(arena: Arena = Arena.ofAuto()): MemorySegment {
    return arena.allocateFrom(this, Charset.forName("UTF-16LE"))
}

/**
 * Converts a Kotlin string to a Windows ANSI string in native memory.
 *
 * Windows APIs that accept `LPCSTR` (ANSI string pointers) expect
 * null-terminated strings in the system's default ANSI code page.
 * This function uses CP1252 (Windows Western European) encoding.
 *
 * @param arena The memory arena that will manage the allocated memory
 *
 * @return A memory segment containing the null-terminated ANSI string
 */
inline fun String.toAnsiString(arena: Arena = Arena.ofAuto()): MemorySegment {
    return arena.allocateFrom(this, Charset.forName("CP1252"))
}

/**
 * Allocates a string in native memory with configurable encoding.
 *
 * Convenience function that dispatches to either [toWideString] or
 * [toAnsiString] based on the [isUnicode] parameter.
 *
 * Example:
 *
 * ```kotlin
 * val wide = allocateString("Hello", arena, isUnicode = true)   // for MessageBoxW
 * val ansi = allocateString("Hello", arena, isUnicode = false)  // for MessageBoxA
 * ```
 *
 * @param arena The memory arena that will manage the allocated memory
 * @param isUnicode If true, use UTF-16LE (wide); if false, use CP1252 (ANSI)
 *
 * @return A memory segment containing the null-terminated string
 */
fun String.allocateString(arena: Arena = Arena.ofAuto(), isUnicode: Boolean = true): MemorySegment {
    return if (isUnicode) {
        this.toWideString(arena)
    } else {
        this.toAnsiString(arena)
    }
}

/**
 * Reads a null-terminated ANSI string from a memory segment.
 *
 * This function reads bytes from the segment until it encounters
 * a null terminator (0), then decodes them using CP1252 encoding.
 *
 * Example:
 *
 * ```kotlin
 * val ansiString = fromAnsiString(segment)
 * ```
 *
 * @param segment The memory segment containing the ANSI string
 *
 * @return The decoded string, or empty string if the first byte is null
 */
fun fromAnsiString(segment: MemorySegment): String {
    val bytes = segment.asSlice(0, 1024).toArray(ValueLayout.JAVA_BYTE)
    val nullIndex = bytes.indexOf(0)
    val actualBytes = if (nullIndex >= 0) bytes.sliceArray(0 until nullIndex) else bytes
    return String(actualBytes, Charset.forName("CP1252"))
}