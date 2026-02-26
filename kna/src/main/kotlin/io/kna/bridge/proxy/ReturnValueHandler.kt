@file:Suppress("Unused", "Unchecked_cast")
package io.kna.bridge.proxy
import io.kna.layout.*

import java.util.concurrent.*
import java.lang.foreign.*
import java.lang.reflect.*

import kotlin.reflect.*

/* === RETURN VALUE HANDLER === */
object ReturnValueHandler {
    /* === INTERNAL CACHES === */
    private val returnTypeCache = ConcurrentHashMap<KClass<*>, ReturnTypeInfo>()


    /* === INTERNAL TYPES === */
    private sealed class ReturnTypeInfo {
        data object VoidReturn : ReturnTypeInfo()

        data class NativeTypeReturn(
            val constructor: Constructor<out NativeType>
        ) : ReturnTypeInfo()

        data object PredefinedReturn : ReturnTypeInfo()
    }


    /* === PUBLIC HANDLER FUNCTIONS === */
    fun handleReturn(
        result: Any?,
        returnType: KClass<*>
    ): Any? {
        return when (val typeInfo = getReturnTypeInfo(returnType)) {
            is ReturnTypeInfo.VoidReturn -> {
                null
            }

            is ReturnTypeInfo.NativeTypeReturn -> {
                handleNativeTypeReturn(result as? MemorySegment, typeInfo.constructor)
            }

            is ReturnTypeInfo.PredefinedReturn -> {
                result
            }
        }
    }


    /* === INTERNAL HANDLER FUNCTIONS === */
    private fun handleNativeTypeReturn(
        segment: MemorySegment?,
        constructor: Constructor<out NativeType>
    ): Any? {
        if (segment == null || segment.address() == MemorySegment.NULL.address()) {
            return null
        }

        val struct = constructor.newInstance()
        struct.readFrom(segment)
        return struct
    }


    /* === INTERNAL PROCESSORS === */
    private fun getReturnTypeInfo(returnType: KClass<*>): ReturnTypeInfo {
        return returnTypeCache.getOrPut(returnType) {
            when {
                returnType == Unit::class || returnType == Void::class -> {
                    ReturnTypeInfo.VoidReturn
                }

                NativeType::class.java.isAssignableFrom(returnType.java) -> {
                    val structClass = returnType.java as Class<out NativeType>
                    val constructor = structClass.getDeclaredConstructor().apply {
                        isAccessible = true
                    }
                    ReturnTypeInfo.NativeTypeReturn(constructor)
                }

                else -> {
                    ReturnTypeInfo.PredefinedReturn
                }
            }
        }
    }


    /* === PUBLIC MANAGEMENT FUNCTIONS === */
    fun clearCache() {
        returnTypeCache.clear()
    }

    fun clearCacheForType(returnType: KClass<*>) {
        returnTypeCache.remove(returnType)
    }
}