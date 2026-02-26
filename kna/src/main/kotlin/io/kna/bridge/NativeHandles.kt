@file:Suppress("Unused")
package io.kna.bridge
import io.kna.*

import java.util.concurrent.*
import java.lang.foreign.*
import java.lang.invoke.*
import java.nio.file.*

/* === NATIVE HANDLES === */
object NativeHandles {
    /* === INTERNAL CACHES === */
    @PublishedApi
    internal val methodHandleCache = ConcurrentHashMap<String, MethodHandle>()


    /* === PUBLIC BINDING BUILDERS === */
    class FunctionDescriptorBuilder {
        /* === INTERNAL BUILDER COMPONENTS === */
        /* Parameter */
        private val parameterLayouts = mutableListOf<MemoryLayout>()

        /* Return */
        private var returnLayout: MemoryLayout? = null


        /* === PUBLIC BUILDER CONFIGURATIONS === */
        /* Memory Layout Parameter */
        fun param(layout: MemoryLayout) {
            parameterLayouts.add(layout)
        }

        fun params(vararg layouts: MemoryLayout) {
            parameterLayouts.addAll(layouts.map { it })
        }

        /* Memory Layout Return */
        fun returns(layout: MemoryLayout) {
            if (returnLayout != null) {
                throw RuntimeException("Return layout already set")
            }

            returnLayout = layout
        }


        /* === PUBLIC BUILDER === */
        fun build(): FunctionDescriptor {
            return try {
                when {
                    returnLayout == null -> when {
                        parameterLayouts.isEmpty() -> FunctionDescriptor.ofVoid()
                        else -> FunctionDescriptor.ofVoid(*parameterLayouts.toTypedArray())
                    }

                    else -> when {
                        parameterLayouts.isEmpty() -> FunctionDescriptor.of(returnLayout)
                        else -> FunctionDescriptor.of(returnLayout, *parameterLayouts.toTypedArray())
                    }
                }
            } catch (e: Throwable) {
                throw RuntimeException("Failed to build FunctionDescriptor: ${e.message}", e)
            }
        }


        /* === PROVIDED PRIMITIVE LAYOUTS === */
        /* Value */
        val byte: MemoryLayout = ValueLayout.JAVA_BYTE

        val short: MemoryLayout = ValueLayout.JAVA_SHORT

        val int: MemoryLayout = ValueLayout.JAVA_INT

        val long: MemoryLayout = ValueLayout.JAVA_LONG

        val float: MemoryLayout = ValueLayout.JAVA_FLOAT

        val double: MemoryLayout = ValueLayout.JAVA_DOUBLE

        val bool: MemoryLayout = ValueLayout.JAVA_BOOLEAN

        val char: MemoryLayout = ValueLayout.JAVA_CHAR

        /* Address */
        val ptr: MemoryLayout = ValueLayout.ADDRESS
    }


    /* === PUBLIC BINDING FUNCTIONS === */
    /* Base */
    @JvmStatic
    inline fun handleFrom(
        libraryLookup: SymbolLookup,
        functionName: String,
        functionDescriptor: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        return try {
            val descriptor = FunctionDescriptorBuilder().apply(functionDescriptor).build()
            val cacheKey = generateCacheKey(libraryLookup, functionName, descriptor)
            methodHandleCache.getOrPut(cacheKey) {
                Natives.nativeLinker.downcallHandle(
                    libraryLookup.find(functionName).orElseThrow {
                        RuntimeException("Function $functionName not found in native library")
                    },
                    descriptor
                )
            }
        } catch (e: Throwable) {
            throw RuntimeException("Could not create downcall handle for function $functionName from native library", e)
        }
    }

    @JvmStatic
    inline fun handleFrom(
        libraryName: String,
        functionName: String,
        functionDescriptor: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        return try {
            val descriptor = FunctionDescriptorBuilder().apply(functionDescriptor).build()
            val libraryLookup = SymbolLookup.libraryLookup(libraryName, Natives.sharedArena)
            val cacheKey = generateCacheKey(libraryLookup, functionName, descriptor)
            methodHandleCache.getOrPut(cacheKey) {
                Natives.nativeLinker.downcallHandle(
                    libraryLookup.find(functionName).orElseThrow {
                        RuntimeException("Function $functionName not found in native library $libraryName")
                    },
                    descriptor
                )
            }
        } catch (e: Throwable) {
            throw RuntimeException("Could not create downcall handle for function $functionName from native library $libraryName", e)
        }
    }

    @JvmStatic
    inline fun handleFrom(
        libraryPath: Path,
        functionName: String,
        functionDescriptor: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        return try {
            val descriptor = FunctionDescriptorBuilder().apply(functionDescriptor).build()
            val libraryLookup = SymbolLookup.libraryLookup(libraryPath, Natives.sharedArena)
            val cacheKey = generateCacheKey(libraryLookup, functionName, descriptor)
            methodHandleCache.getOrPut(cacheKey) {
                Natives.nativeLinker.downcallHandle(
                    libraryLookup.find(functionName).orElseThrow {
                        RuntimeException("Function $functionName not found in native library $libraryPath")
                    },
                    descriptor
                )
            }
        } catch (e: Throwable) {
            throw RuntimeException("Could not create downcall handle for function $functionName from native library $libraryPath", e)
        }
    }

    /* Extension */
    @JvmStatic
    inline fun handleFromKernel32(
        functionName: String,
        functionDescriptor: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        return handleFrom(Natives.kernel32Lookup, functionName, functionDescriptor)
    }

    @JvmStatic
    inline fun handleFromUser32(
        functionName: String,
        functionDescriptor: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        return handleFrom(Natives.user32Lookup, functionName, functionDescriptor)
    }


    /* === INTERNAL CACHE HELPER FUNCTIONS === */
    @JvmStatic
    @PublishedApi
    internal fun generateCacheKey(lookup: SymbolLookup, functionName: String, descriptor: FunctionDescriptor): String {
        return "${lookup.hashCode()}:$functionName:$descriptor"
    }
}


/* === PUBLIC NATIVE BINDING FUNCTIONS === */
inline fun SymbolLookup.handleFrom(
    functionName: String,
    functionDescriptor: NativeHandles.FunctionDescriptorBuilder.() -> Unit
): MethodHandle {
    return NativeHandles.handleFrom(this, functionName, functionDescriptor)
}