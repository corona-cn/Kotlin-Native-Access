@file:Suppress("Unused", "Unchecked_cast")
package io.kna.core.layout
import io.kna.core.layout.address.*
import io.kna.core.layout.struct.*
import io.kna.core.layout.value.*

import java.lang.foreign.*

import kotlin.reflect.*

interface NativeLayout<Type> {
    companion object {
        fun from(kclass: KClass<*>): NativeLayout<*> {
            return when (kclass) {
                Byte::class -> NativeByte()
                Short::class -> NativeShort()
                Int::class -> NativeInt()
                Long::class -> NativeLong()
                Float::class -> NativeFloat()
                Double::class -> NativeDouble()
                Boolean::class -> NativeBoolean()
                Char::class -> NativeChar()
                String::class -> NativePointer()
                MemorySegment::class -> NativePointer()
                else -> {
                    when {
                        kclass.java.isArray -> NativePointer()
                        NativeStruct::class.java.isAssignableFrom(kclass.java) -> NativeStructLayout(kclass as KClass<out NativeStruct>)
                        else -> throw IllegalArgumentException("Unsupported type: $kclass")
                    }
                }
            }
        }

        fun from(type: KType): NativeLayout<*> {
            val kclass = type.classifier as? KClass<*> ?: throw IllegalArgumentException("Unsupported type: $type")
            return from(kclass)
        }

        fun from(jclass: Class<*>): NativeLayout<*> {
            val kclass = jclass.kotlin
            return from(kclass)
        }
    }

    val layout: MemoryLayout

    fun read(
        segment: MemorySegment,
        offset: Long
    ) : Type

    fun write(
        segment: MemorySegment,
        offset: Long,
        value: Type
    )
}