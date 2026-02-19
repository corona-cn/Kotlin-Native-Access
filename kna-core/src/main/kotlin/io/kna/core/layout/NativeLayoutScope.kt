@file:Suppress("Unused")
package io.kna.core.layout
import io.kna.core.layout.address.*
import io.kna.core.layout.value.*

abstract class NativeLayoutScope {
    /* === NATIVE LAYOUTS === */
    /* Primitive */
    val byte: NativeByte = NativeByte()

    val short: NativeShort = NativeShort()

    val int: NativeInt = NativeInt()

    val long: NativeLong = NativeLong()

    val float: NativeFloat = NativeFloat()

    val double: NativeDouble = NativeDouble()

    val bool: NativeBoolean = NativeBoolean()

    val char: NativeChar = NativeChar()

    /* Pointer */
    val ptr: NativePointer = NativePointer()
}