@file:Suppress("Unused", "Unchecked_cast")
package io.kna.core.layout.struct
import io.kna.core.layout.*

import java.util.concurrent.*
import java.lang.foreign.*

import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.reflect.*

abstract class NativeStruct {
    companion object {
        private val structLayoutCache = ConcurrentHashMap<KClass<out NativeStruct>, StructLayout>()
        private val offsetCache = ConcurrentHashMap<KClass<out NativeStruct>, ConcurrentHashMap<String, Long>>()
        private val fieldNativeLayoutCache = ConcurrentHashMap<KClass<out NativeStruct>, ConcurrentHashMap<String, NativeLayout<*>>>()

        @JvmStatic
        internal fun getStructLayout(clazz: KClass<out NativeStruct>): StructLayout {
            return structLayoutCache.computeIfAbsent(clazz) { kclass ->
                kclass.memberProperties
                    .map { property -> NativeLayout.from(property.returnType).layout.withName(property.name) }
                    .let { fieldLayouts -> MemoryLayout.structLayout(*fieldLayouts.toTypedArray()) }
            }
        }

        @JvmStatic
        private fun getOffsets(clazz: KClass<out NativeStruct>): ConcurrentHashMap<String, Long> {
            return offsetCache.computeIfAbsent(clazz) { kclass ->
                val layout = getStructLayout(kclass)
                kclass.memberProperties
                    .associate { property -> property.name to layout.byteOffset(MemoryLayout.PathElement.groupElement(property.name)) }
                    .let { map -> ConcurrentHashMap(map) }
            }
        }

        @JvmStatic
        private fun getFieldNativeLayouts(clazz: KClass<out NativeStruct>): ConcurrentHashMap<String, NativeLayout<*>> {
            return fieldNativeLayoutCache.computeIfAbsent(clazz) { kclass ->
                kclass.memberProperties
                    .associate { property -> property.name to NativeLayout.from(property.returnType) }
                    .let { map -> ConcurrentHashMap(map) }
            }
        }
    }

    internal fun allocate(arena: Arena): MemorySegment {
        val segment = arena.allocate(getStructLayout(this::class))
        writeTo(segment)
        return segment
    }

    internal fun readFrom(segment: MemorySegment): NativeStruct {
        val clazz = this::class
        val offsets = getOffsets(clazz)
        val fieldNativeLayouts = getFieldNativeLayouts(clazz)

        clazz.memberProperties.forEach { property ->
            val fieldName = property.name
            val offset = offsets[fieldName] ?: return@forEach
            val layout = fieldNativeLayouts[fieldName] ?: return@forEach
            val value = layout.read(segment, offset)
            property.javaField?.trySetAccessible()
            property.javaField?.set(this, value)
        }

        return this
    }

    internal fun writeTo(segment: MemorySegment) {
        val clazz = this::class
        val offsets = getOffsets(clazz)
        val fieldNativeLayouts = getFieldNativeLayouts(clazz)

        clazz.memberProperties.forEach { property ->
            val fieldName = property.name
            val offset = offsets[fieldName] ?: return@forEach
            val layout = fieldNativeLayouts[fieldName] ?: return@forEach
            val value = property.call(this)
            (layout as NativeLayout<Any?>).write(segment, offset, value)
        }
    }
}