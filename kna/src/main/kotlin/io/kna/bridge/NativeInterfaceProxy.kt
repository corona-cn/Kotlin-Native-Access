@file:Suppress("Unused")
package io.kna.bridge
import io.kna.bridge.proxy.*
import io.kna.resolver.*
import io.kna.layout.*

import java.lang.reflect.*
import java.lang.foreign.*
import java.lang.invoke.*

import kotlin.reflect.*

/* === NATIVE INTERFACE PROXY === */
object NativeInterfaceProxy {
    /* === INTERNAL CACHE POOL === */
    private val arenaPool = ThreadLocal.withInitial { Arena.ofConfined() }


    /* === PUBLIC PROXY FACTORY === */
    inline fun <reified Interface : Any> proxyFor(interfaceLibraryLookup: SymbolLookup): Interface {
        return ProxyHandlerFactory.proxyHandlerFor(
            interfaceClass = Interface::class.java,
            methodTransformer = { method ->
                createBaseMethodHandle(interfaceLibraryLookup, method)
            }
        ) { proxy, method, methodHandle, args ->
            invokeWithPassing(methodHandle, method, args)
        }
    }

    /* === INTERNAL PROXY PROCESSORS === */
    /* Method Handle */
    @PublishedApi
    internal fun createBaseMethodHandle(
        libraryLookup: SymbolLookup,
        method: Method
    ): MethodHandle {
        val methodInfo = MethodResolver.resolveMethodInfo(method)
        return NativeHandles.handleFrom(libraryLookup, methodInfo.name) {
            // Parameter
            methodInfo.parameterInfos.forEach { info ->
                when {
                    // Predefined type param
                    ArgumentPreparer.isPredefinedType(info.klass) -> {
                        param(info.klass.toPredefinedLayout())
                    }

                    // Native type param
                    NativeType::class.java.isAssignableFrom(info.klass.java) -> {
                        param(ptr)
                    }

                    // Exception
                    else -> {
                        throw RuntimeException("Unsupported parameter type: ${info.klass}")
                    }
                }
            }

            // Return
            when {
                // Empty return
                methodInfo.returnKlass == Unit::class || methodInfo.returnKlass == Void::class -> {

                }

                // Predefined type return
                ArgumentPreparer.isPredefinedType(methodInfo.returnKlass) -> {
                    returns(methodInfo.returnKlass.toPredefinedLayout())
                }

                // Native type return
                NativeType::class.java.isAssignableFrom(methodInfo.returnKlass.java) -> {
                    returns(ptr)
                }

                // Exception
                else -> {
                    throw RuntimeException("Unsupported return type: ${methodInfo.returnKlass}")
                }
            }
        }
    }

    /* Invocation */
    @PublishedApi
    internal fun invokeWithPassing(
        methodHandle: MethodHandle,
        method: Method,
        args: Array<out Any>
    ): Any? {
        arenaPool.get().use { arena ->
            // Prepare arguments
            val prepared = ArgumentPreparer.prepareArguments(method, args, arena)

            // Invoke
            val result = methodHandle.invokeWithArguments(*prepared.args)

            // Handle return
            val finalResult = ReturnValueHandler.handleReturn(result, prepared.returnType)

            // Write back outputs if needed
            if (prepared.hasOutputs()) {
                prepared.writeBackOutputs()
            }

            return finalResult
        }
    }

    /* Layout Mapper */
    @PublishedApi
    internal fun KClass<*>.toPredefinedLayout(): MemoryLayout {
        return when (this) {
            Byte::class -> ValueLayout.JAVA_BYTE
            Short::class -> ValueLayout.JAVA_SHORT
            Int::class -> ValueLayout.JAVA_INT
            Long::class -> ValueLayout.JAVA_LONG
            Float::class -> ValueLayout.JAVA_FLOAT
            Double::class -> ValueLayout.JAVA_DOUBLE
            Boolean::class -> ValueLayout.JAVA_BOOLEAN
            Char::class -> ValueLayout.JAVA_CHAR
            MemorySegment::class -> ValueLayout.ADDRESS
            String::class -> ValueLayout.ADDRESS
            else -> throw RuntimeException("Cannot get predefined layout for type: $this")
        }
    }
}