@file:Suppress("Unused", "Unchecked_cast")
package io.kna.core.binder
import java.util.concurrent.*
import java.lang.reflect.*
import java.lang.foreign.*
import java.lang.invoke.*

/**
 * Provides dynamic implementation of native interface methods using JDK Proxy
 * and direct MethodHandle calls.
 *
 * This object serves as the primary entry point for creating proxy instances that delegate
 * interface method calls to native functions. It eliminates reflection overhead by using
 * **MethodHandle.invoke()** for optimal performance while maintaining the convenience of
 * interface-based native function binding.
 *
 * The binder automatically caches both proxy instances and method handles for repeated use,
 * ensuring minimal performance impact on subsequent bindings. All native function lookups
 * are performed through the provided [SymbolLookup] instance, with proper error handling
 * for missing symbols.
 *
 * The binder supports all Java primitive types, [String], and [MemorySegment] as both
 * parameters and return values. Complex native types should be represented as
 * [MemorySegment] and managed through the Foreign Function API.
 *
 * Example usage with Windows Kernel32 interface:
 *
 * ```kotlin
 * interface Kernel32 {
 *     fun GetCurrentProcessId(): Int
 *     fun GetLastError(): Int
 *     fun GetStdHandle(nStdHandle: Int): MemorySegment
 *     fun OpenProcess(dwDesiredAccess: Int, bInheritHandle: Int, dwProcessId: Long): MemorySegment
 *     fun CloseHandle(hObject: MemorySegment): Int
 *     fun ReadProcessMemory(
 *         hProcess: MemorySegment,
 *         lpBaseAddress: MemorySegment,
 *         lpBuffer: MemorySegment,
 *         nSize: Long,
 *         lpNumberOfBytesRead: MemorySegment
 *     ): Int
 * }
 *
 * // Create proxy instance bound to kernel32 library
 * val kernel32 = NativeProxyInterfaceBinder.bind<Kernel32>(NativeFunctionBinder.kernel32)
 *
 * // Use native functions directly through the proxy
 * val processId = kernel32.GetCurrentProcessId()
 * val processHandle = kernel32.OpenProcess(PROCESS_ALL_ACCESS, 0, processId.toLong())
 *
 * // ... perform operations
 * kernel32.CloseHandle(processHandle)
 * ```
 *
 * For libraries other than the pre-configured ones, you can provide
 * any [SymbolLookup] instance:
 *
 * ```kotlin
 * // Load your own custom library
 * val myLib = SymbolLookup.libraryLookup("mylib.dll", NativeFunctionBinder.sharedArena)
 *
 * // Define interface matching the native functions
 * interface MyLibrary {
 *     fun add(a: Int, b: Int): Int
 *     fun multiply(a: Double, b: Double): Double
 * }
 *
 * // Bind and use
 * val math = NativeProxyInterfaceBinder.bind<MyLibrary>(myLib)
 * val result = math.add(5, 3)  // Calls native add function
 * ```
 *
 * @see NativeFunctionBinder
 */
object NativeProxyInterfaceBinder {
    /* === INTERNAL CACHES === */
    /**
     * Cache for created proxy instances keyed by interface class
     */
    private val proxyCache = ConcurrentHashMap<Class<*>, Any>()

    /**
     * Cache for MethodHandles keyed by (functionName, returnType)
     */
    private val methodHandleCache = ConcurrentHashMap<Pair<String, Class<*>>, MethodHandle>()


    /* === PUBLIC BINDER === */
    /**
     * Binds a native interface to a proxy implementation using the provided library lookup.
     *
     * This is the primary entry point for creating proxy instances that delegate
     * interface method calls to native functions. The returned proxy handles all
     * method invocations through direct MethodHandle calls without reflection overhead.
     *
     * @param T Interface type defining native function signatures
     * @param library SymbolLookup for the target native library (e.g., kernel32, user32)
     *
     * @return Proxy instance implementing T with direct native function binding
     *
     * @throws NoSuchElementException if any native function symbol is not found in the library
     * @throws IllegalArgumentException if interface contains unsupported parameter/return types
     */
    @JvmStatic
    inline fun <reified T : Any> bind(library: SymbolLookup): T {
        return create(T::class.java, library)
    }


    /* === INTERNAL BINDER === */
    /* Binding Builder */
    /**
     * Creates a proxy implementation of the specified native interface
     * using the provided library lookup.
     *
     * @param T Interface type representing native functions
     * @param interfaceClass Class object of the interface to implement
     * @param library SymbolLookup for the target native library
     *
     * @return Proxy instance implementing T
     *
     * @throws NoSuchElementException if any native function symbol is not found
     * @throws IllegalArgumentException if interface contains unsupported parameter/return types
     */
    @JvmStatic
    @PublishedApi
    internal fun <T : Any> create(interfaceClass: Class<T>, library: SymbolLookup): T {
        val cachedProxy = proxyCache[interfaceClass]
        if (cachedProxy != null) {
            return cachedProxy as T
        }

        val methodHandles = interfaceClass.methods
            .filter { method -> method.declaringClass != Any::class.java }
            .associateWith { method -> getOrCreateMethodHandle(method, library) }

        val handler = InvocationHandler { _, method, args ->
            if (method.declaringClass == Any::class.java) {
                return@InvocationHandler try {
                    when (method.name) {
                        "toString" -> "NativeProxyInterface for ${method.declaringClass.simpleName}"
                        "hashCode" -> System.identityHashCode(method.declaringClass)
                        "equals" -> args?.get(0) === method.declaringClass
                        else -> null
                    }
                } catch (e: Throwable) {
                    throw RuntimeException("Failed to handle Object method: ${method.name}", e)
                }
            }

            try {
                val handle = methodHandles[method] ?: throw NoSuchMethodException("Method ${method.name} not found in native interface")
                when {
                    args == null || args.isEmpty() -> {
                        handle.invoke()
                    }

                    else -> {
                        when (args.size) {
                            1 -> handle.invoke(args[0])
                            2 -> handle.invoke(args[0], args[1])
                            3 -> handle.invoke(args[0], args[1], args[2])
                            4 -> handle.invoke(args[0], args[1], args[2], args[3])
                            5 -> handle.invoke(args[0], args[1], args[2], args[3], args[4])
                            6 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5])
                            7 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6])
                            8 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
                            else -> handle.invokeWithArguments(*args)
                        }
                    }
                }
            } catch (e: Throwable) {
                throw RuntimeException("Failed to invoke native function: ${method.name}", e)
            }
        }

        val proxy = Proxy.newProxyInstance(interfaceClass.classLoader, arrayOf(interfaceClass), handler) as T
        proxyCache[interfaceClass] = proxy

        return proxy
    }

    /* Method Handle Processor */
    /**
     * Gets or creates a MethodHandle for a native function based on interface method.
     *
     * Uses NativeFunctionBinder's DSL to build the function descriptor from
     * method parameter and return types.
     *
     * @param method Interface method representing the native function
     * @param library SymbolLookup for the target library
     *
     * @return MethodHandle for direct native invocation
     *
     * @throws NoSuchElementException if native function symbol not found
     * @throws IllegalArgumentException if parameter/return types are unsupported
     */
    @JvmStatic
    private fun getOrCreateMethodHandle(method: Method, library: SymbolLookup): MethodHandle {
        val key = method.name to method.returnType
        return methodHandleCache.computeIfAbsent(key) {
            try {
                val descriptor = NativeFunctionBinder.FunctionDescriptorBuilder().apply {
                    val paramLayouts = method.parameterTypes.map { paramType ->
                        when (paramType) {
                            Byte::class.java -> byte
                            Short::class.java -> short
                            Int::class.java -> int
                            Long::class.java -> long
                            Float::class.java -> float
                            Double::class.java -> double
                            Boolean::class.java -> boolean
                            Char::class.java -> char
                            MemorySegment::class.java -> address
                            String::class.java -> address
                            else -> throw IllegalArgumentException("Unsupported parameter type: $paramType for method ${method.name}")
                        }
                    }.toTypedArray()

                    params(*paramLayouts)

                    when (method.returnType) {
                        Void.TYPE -> returns(byte)
                        Byte::class.java -> returns(byte)
                        Short::class.java -> returns(short)
                        Int::class.java -> returns(int)
                        Long::class.java -> returns(long)
                        Float::class.java -> returns(float)
                        Double::class.java -> returns(double)
                        Boolean::class.java -> returns(boolean)
                        Char::class.java -> returns(char)
                        MemorySegment::class.java -> returns(address)
                        String::class.java -> returns(address)
                        else -> throw IllegalArgumentException("Unsupported return type: ${method.returnType} for method ${method.name}")
                    }
                }.build()

                if (method.returnType == Void.TYPE) {
                    return@computeIfAbsent NativeFunctionBinder.nativeLinker.downcallHandle(
                        library.find(method.name).orElseThrow { NoSuchElementException("Cannot find symbol: ${method.name}") },
                        FunctionDescriptor.ofVoid(*descriptor.argumentLayouts().toTypedArray()),
                    )
                }

                NativeFunctionBinder.nativeLinker.downcallHandle(
                    library.find(method.name).orElseThrow { NoSuchElementException("Cannot find symbol: ${method.name}") },
                    descriptor,
                )
            } catch (e: Throwable) {
                throw RuntimeException("Failed to create MethodHandle for native function: ${method.name}", e)
            }
        }
    }
}