@file:Suppress("Unused", "Nothing_to_inline")
package io.kna.core.util
import java.lang.foreign.*

/* === PUBLIC CONVERSION FUNCTIONS === */
/**
 * Converts a byte array to a hexadecimal string representation.
 *
 * Example:
 * ```
 * val hex = bytesToHex(byteArrayOf(0x4D, 0x5A, 0x90)) // "4D 5A 90"
 * ```
 *
 * @param bytes The byte array to convert
 * @param separator The separator between hex pairs (default: space)
 *
 * @return Hexadecimal string, e.g., "4D 5A 90 00"
 */
fun bytesToHex(bytes: ByteArray, separator: String = " "): String {
    return bytes.joinToString(separator) { "%02X".format(it) }
}

/**
 * Converts a hexadecimal string back to a byte array.
 *
 * Accepts formats with or without spaces, and optional "0x" prefix.
 *
 * Example:
 * ```
 * val bytes = hexToBytes("90 90 EB 1F") // byteArrayOf(0x90, 0x90, 0xEB, 0x1F)
 * ```
 *
 * @param hex Hexadecimal string (e.g., "4D 5A 90", "4D5A90", or "0x4D5A90")
 *
 * @return Decoded byte array
 *
 * @throws IllegalArgumentException if hex string length is odd
 */
fun hexToBytes(hex: String): ByteArray {
    val clean = hex.replace(" ", "").replace("0x", "")

    require(clean.length % 2 == 0) {
        "Invalid hex string length"
    }

    return clean.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

/**
 * Formats a byte array as a classic hex dump (hex + ASCII representation).
 *
 * Output format resembles traditional hex editors:
 * ```
 * 00000000: 4D 5A 90 00 03 00 00 00 04 00 00 00 FF FF 00 00  |MZ..............|
 * ```
 *
 * @param bytes The byte array to dump
 * @param bytesPerLine Number of bytes per line (default: 16)
 *
 * @return Formatted hex dump string
 */
fun formatHexDump(bytes: ByteArray, bytesPerLine: Int = 16): String {
    return buildString {
        for (i in bytes.indices step bytesPerLine) {
            val line = bytes.slice(i until minOf(i + bytesPerLine, bytes.size))

            append("%08X: ".format(i))

            line.forEach {
                append("%02X ".format(it))
            }

            repeat(bytesPerLine - line.size) {
                append("   ")
            }

            append(" |")

            line.forEach {
                val c = it.toInt().toChar()
                append(if (c in ' '..'~') c else '.')
            }

            repeat(bytesPerLine - line.size) {
                append(' ')
            }

            appendLine("|")
        }
    }
}

/**
 * Creates a hex dump from a MemorySegment.
 *
 * Convenience wrapper around [formatHexDump] that reads the specified
 * number of bytes from a memory segment.
 *
 * Example:
 * ```
 * val dump = toHexDump(processHandle, 64)
 * println(dump) // prints first 64 bytes as hex dump
 * ```
 *
 * @param segment The memory segment to read from
 * @param size Number of bytes to dump (starting from offset 0)
 *
 * @return Formatted hex dump string
 */
fun toHexDump(segment: MemorySegment, size: Int): String {
    val bytes = segment.asSlice(0, size.toLong()).toArray(ValueLayout.JAVA_BYTE)
    return formatHexDump(bytes)
}