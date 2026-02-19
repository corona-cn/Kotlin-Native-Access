@file:Suppress("Unused", "Unchecked_cast")
package io.kna.core.binder
import io.kna.core.layout.struct.*
import io.kna.core.layout.*

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
 * val kernel32 = NativeInterfaceProxyBinder.bind<Kernel32>(NativeFunctionBinder.kernel32)
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
 * val math = NativeInterfaceProxyBinder.bind<MyLibrary>(myLib)
 * val result = math.add(5, 3)  // Calls native add function
 * ```
 *
 * Use struct parameters and return value:
 *
 * ```kotlin
 * // Define your structs by extending NativeStruct
 * data class Rect(
 *     var left: Int = 0,
 *     var top: Int = 0,
 *     var right: Int = 0,
 *     var bottom: Int = 0
 * ) : NativeStruct()
 *
 * data class Point(
 *     var x: Int = 0,
 *     var y: Int = 0
 * ) : NativeStruct()
 *
 * // Define interface with struct parameters
 * interface User32 {
 *     fun GetForegroundWindow(): MemorySegment
 *     fun GetWindowRect(hWnd: MemorySegment, lpRect: Rect): Int
 *     fun GetCursorPos(lpPoint: Point): Int
 *     fun SetWindowPos(
 *         hWnd: MemorySegment,
 *         hWndInsertAfter: MemorySegment,
 *         X: Int,
 *         Y: Int,
 *         cx: Int,
 *         cy: Int,
 *         uFlags: Int
 *     ): Boolean
 * }
 *
 * // Create proxy instance
 * val user32 = NativeInterfaceProxyBinder.bind<User32>(NativeFunctionBinder.user32)
 *
 * // Get foreground window
 * val hWnd = user32.GetForegroundWindow()
 *
 * // Get window rectangle - struct is automatically populated
 * val rect = Rect()
 * val result = user32.GetWindowRect(hWnd, rect)
 * println("Window rectangle: $rect") // rect is automatically updated with native data
 *
 * // Get cursor position - struct is automatically populated
 * val point = Point()
 * val cursorResult = user32.GetCursorPos(point)
 * println("Cursor position: $point") // point is automatically updated
 *
 * // Set window position - struct is automatically converted
 * rect.left = 100
 * rect.top = 100
 * user32.SetWindowPos(hWnd, MemorySegment.NULL, rect.left, rect.top, 800, 600, 0)
 * ```
 *
 * @see NativeFunctionBinder
 */
object NativeInterfaceProxyBinder {
    /* === INTERNAL CACHES === */
    /**
     * Cache for created proxy instances keyed by interface class
     */
    private val proxyCache = ConcurrentHashMap<Class<*>, Any>()

    /**
     * Cache for MethodHandles keyed by interface class and Method
     */
    private val methodHandleCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<Method, MethodHandle>>()

    /**
     * Cache for MemorySegments of Methods keyed by interface class and Method
     */
    private val methodSegmentCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<Method, MemorySegment>>()

    /**
     * Cache for argument-spreader MethodHandles keyed by interface class and Method and argument count
     */
    private val spreaderCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<Method, ConcurrentHashMap<Int, MethodHandle>>>()

    /**
     * Metadata for native interface methods that contain struct parameters or return values.
     */
    private class MethodMetadata(
        val structParamIndices: IntArray,
        val returnType: Class<out NativeStruct>?
    )


    /* === PUBLIC BINDER === */
    /**
     * Binds a native interface to a proxy implementation using the provided library lookup.
     *
     * This is the primary entry point for creating proxy instances that delegate
     * interface method calls to native functions. The returned proxy handles all
     * method invocations through direct MethodHandle calls without reflection overhead.
     *
     * @param Type Interface type defining native function signatures
     * @param library SymbolLookup for the target native library (e.g., kernel32, user32)
     *
     * @return Proxy instance implementing T with direct native function binding
     *
     * @throws NoSuchElementException if any native function symbol is not found in the library
     * @throws IllegalArgumentException if interface contains unsupported parameter/return types
     */
    @JvmStatic
    inline fun <reified Type : Any> bind(library: SymbolLookup): Type {
        return createInterfaceProxy(Type::class.java, library)
    }


    /* === INTERNAL BINDING PROCESSORS === */
    /* Binding Builder */
    /**
     * Creates a proxy implementation of the specified native interface
     * using the provided library lookup.
     *
     * @param Type Interface type representing native functions
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
    internal fun <Type : Any> createInterfaceProxy(interfaceClass: Class<Type>, library: SymbolLookup): Type {
        // Attempt cache reuse for proxy
        val cachedProxy = proxyCache[interfaceClass]
        if (cachedProxy != null) {
            return cachedProxy as Type
        }

        // Create or cache method handles for this interface
        val methodHandleMap = methodHandleCache.computeIfAbsent(interfaceClass) {
            interfaceClass.methods.asSequence()
                // Filter out methods inherited from Any (equals(), hashCode(), toString())
                .filter { method ->
                    method.declaringClass != Any::class.java
                }

                // Ensure at least one valid method remains
                .ifEmpty {
                    throw IllegalArgumentException("Interface $interfaceClass must declare at least one method")
                }

                // Create a method handle for each method
                .associateWith { method ->
                    // Create or cache MemorySegment for this method
                    val methodSegmentMap = methodSegmentCache.computeIfAbsent(interfaceClass) { ConcurrentHashMap() }
                    val methodSegment = methodSegmentMap.computeIfAbsent(method) {
                        library.find(method.name).orElseThrow {
                            NoSuchElementException("Cannot find function ${method.name} from SymbolLookup: $library")
                        }
                    }

                    // Create MemoryLayouts for parameters
                    val parameterMemoryLayouts = method.parameterTypes.map { parameterType ->
                        when {
                            NativeStruct::class.java.isAssignableFrom(parameterType) -> ValueLayout.ADDRESS
                            else -> NativeLayout.from(parameterType).layout
                        }
                    }.toTypedArray()

                    // Create NativeLayouts for return
                    val returnLayout = when {
                        method.returnType == Void.TYPE -> null
                        NativeStruct::class.java.isAssignableFrom(method.returnType) -> ValueLayout.ADDRESS
                        else -> NativeLayout.from(method.returnType).layout
                    }

                    // Create FunctionDescriptor for this method
                    val functionDescriptor = if (method.returnType == Void.TYPE) {
                        FunctionDescriptor.ofVoid(*parameterMemoryLayouts)
                    } else {
                        FunctionDescriptor.of(returnLayout, *parameterMemoryLayouts)
                    }

                    // Create MethodHandle for this method
                    NativeFunctionBinder.nativeLinker.downcallHandle(
                        methodSegment,
                        functionDescriptor
                    )
                }

                // Convert the Map to a ConcurrentHashMap
                .let { map ->
                    ConcurrentHashMap(map)
                }
        }

        // Create proxy invocation handler
        val handler = InvocationHandler { _, method, args ->
            // Delegate standard Object methods to local handling instead of native calls
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
                val handle = methodHandleMap[method] ?: throw NoSuchMethodException("Method ${method.name} not found in native interface")

                // Identify struct parameter positions and return type
                val paramTypes = method.parameterTypes
                val structParamIndices = paramTypes.indices
                    .filter { NativeStruct::class.java.isAssignableFrom(paramTypes[it]) }
                    .toList()

                val returnsStruct = NativeStruct::class.java.isAssignableFrom(method.returnType)

                // No struct parameters or return value
                if (structParamIndices.isEmpty() && !returnsStruct) {
                    return@InvocationHandler when {
                        args == null || args.isEmpty() -> handle.invoke()
                        else -> {
                            val methodMap = spreaderCache.getOrPut(interfaceClass) {
                                ConcurrentHashMap()
                            }

                            val methodArgumentCountMap = methodMap.getOrPut(method) {
                                ConcurrentHashMap()
                            }

                            val spreader = methodArgumentCountMap.getOrPut(args.size) {
                                handle.asSpreader(Array<Any>::class.java, args.size)
                            }

                            spreader.invoke(args)
                        }
                    }
                }

                // Handle struct parameters with Arena lifecycle management
                Arena.ofConfined().use { arena ->
                    // Create a map to track original struct instances and their allocated segments
                    val structToSegment = mutableMapOf<NativeStruct, MemorySegment>()

                    // Create a new args array with structs replaced by MemorySegments
                    val processedArgs = args?.mapIndexed { index, arg ->
                        if (index in structParamIndices) {
                            val struct = arg as NativeStruct
                            val segment = struct.allocate(arena)
                            structToSegment[struct] = segment
                            segment
                        } else {
                            arg
                        }
                    }?.toTypedArray() ?: emptyArray()

                    // Invoke native function
                    val result = when (processedArgs.size) {
                        0 -> handle.invoke()
                        else -> {
                            val methodMap = spreaderCache.getOrPut(interfaceClass) {
                                ConcurrentHashMap()
                            }

                            val methodArgumentCountMap = methodMap.getOrPut(method) {
                                ConcurrentHashMap()
                            }

                            val spreader = methodArgumentCountMap.getOrPut(processedArgs.size) {
                                handle.asSpreader(Array<Any>::class.java, processedArgs.size)
                            }

                            spreader.invoke(processedArgs)
                        }
                    }

                    // Read back modified data into NativeStruct instances
                    structToSegment.forEach { (struct, segment) ->
                        struct.readFrom(segment)
                    }

                    // Handle return value if it's a struct
                    if (returnsStruct && result is MemorySegment) {
                        val structClass = method.returnType as Class<NativeStruct>
                        structClass.getDeclaredConstructor().newInstance().apply { readFrom(result) }
                    } else {
                        result
                    }
                }

            } catch (e: Throwable) {
                throw RuntimeException("Failed to invoke native function: ${method.name}", e)
            }
        }

        // Create the dynamic proxy instance that implements the native interface
        // All method calls to this proxy will be intercepted by the predefined invocation handler
        val proxy = Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
            handler
        ) as Type

        // Cache proxy for better performance
        proxyCache[interfaceClass] = proxy

        return proxy
    }
}