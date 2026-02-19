@file:Suppress("Unused", "PropertyName")
package io.kna.core.binder
import io.kna.core.layout.*

import java.lang.foreign.*
import java.lang.invoke.*

/**
 * Native function binder for Windows API.
 *
 * Provides a DSL-style fluent API for creating [MethodHandle] instances that bind to native
 * functions. This object serves as the central entry point for all native function binding
 * operations, offering pre-configured symbol lookups for common Windows libraries and
 * a builder pattern for constructing function descriptors.
 *
 * The binder handles the complexity of the Java Foreign Function API (FFI) by providing
 * a more concise and readable syntax for native function declarations. It manages memory
 * arenas, symbol lookups, and method handle creation automatically.
 *
 * The primary use case is binding to Windows API functions:
 *
 * ```kotlin
 * // Bind to library user32
 * val user32 = SymbolLookup.libraryLookup("user32", NativeFunctionBinder.sharedArena)
 *
 * // Bind to function "MessageBoxW"
 * val messageBox = NativeFunctionBinder.bind(user32, "MessageBoxW") {
 *     params(
 *         address,  // HWND hWnd
 *         address,  // LPCWSTR lpText
 *         address,  // LPCWSTR lpCaption
 *         uint      // UINT uType
 *     )
 *     returns(int)  // int return value
 * }
 *
 * // Bind to function "MessageBoxW" in another way
 * val otherMessageBox = NativeFunctionBinder.bindUser32("MessageBoxW") {
 *     params(
 *         address,  // HWND hWnd
 *         address,  // LPCWSTR lpText
 *         address,  // LPCWSTR lpCaption
 *         uint      // UINT uType
 *     )
 *     returns(int)  // int return value
 * }
 *
 * // Display a message box with UTF-16 encoding
 * Arena.ofConfined().use { arena ->
 *     val text = arena.allocateFrom("Hello World", StandardCharsets.UTF_16LE)
 *     val caption = arena.allocateFrom("Native Dialog", StandardCharsets.UTF_16LE)
 *     val result = messageBox.invokeExact(
 *         MemorySegment.NULL,  // hWnd (no parent window)
 *         text,                // lpText
 *         caption,             // lpCaption
 *         0                    // uType (MB_OK)
 *     ) as Int
 *     println("MessageBox returned: $result")  // 1 = IDOK
 * }
 * ```
 *
 * For libraries other than the pre-configured ones, you can provide
 * any [SymbolLookup] instance:
 *
 * ```kotlin
 * // Load your own custom library
 * val myLib = SymbolLookup.libraryLookup("mylib.dll", NativeFunctionBinder.sharedArena)
 *
 * // mylib.dll exports:
 * // int add(int a, int b);
 * // double multiply(double a, double b);
 *
 * // Bind to function add
 * val add = NativeFunctionBinder.bind(myLib, "add") {
 *     params(int, int)
 *     returns(int)
 * }
 *
 * // Bind to function multiply
 * val multiply = NativeFunctionBinder.bind(myLib, "multiply") {
 *     params(double, double)
 *     returns(double)
 * }
 *
 * // Use
 * println(add.invokeExact(2, 3))           // 5
 * println(multiply.invokeExact(2.5, 4.0))  // 10.0
 * ```
 *
 */
object NativeFunctionBinder {
    /* === PUBLIC NATIVE COMPONENTS === */
    /* Linker */
    /**
     * Native linker for downcall handles
     */
    @JvmField
    val nativeLinker: Linker = Linker.nativeLinker()

    /* Arena */
    /**
     * Shared arena for library lookups
     */
    @JvmField
    val sharedArena: Arena = Arena.ofShared()

    /* Native Library */
    /**
     * Kernel32 library symbol lookup
     */
    @JvmField
    val kernel32: SymbolLookup = SymbolLookup.libraryLookup("kernel32", sharedArena)

    /**
     * User32 library symbol lookup
     */
    @JvmField
    val user32: SymbolLookup = SymbolLookup.libraryLookup("user32", sharedArena)


    /* === PUBLIC BINDING BUILDERS === */
    /**
     * DSL builder for FunctionDescriptor creation
     */
    class FunctionDescriptorBuilder : NativeLayoutScope() {
        /* === INTERNAL BUILDER COMPONENTS === */
        /**
         * Mutable list of argument layouts
         */
        @JvmField
        @PublishedApi
        internal val arguments = mutableListOf<MemoryLayout>()

        /**
         * Return layout (null if not set)
         */
        @JvmField
        @PublishedApi
        internal var returnLayout: MemoryLayout? = null


        /* === PUBLIC BUILDER FUNCTIONS === */
        /**
         * Sets function parameter layouts in order.
         */
        fun params(vararg layouts: NativeLayout<*>) {
            arguments.addAll(layouts.map { it.layout })
        }

        /**
         * Sets function return layout.
         */
        fun returns(layout: NativeLayout<*>) {
            returnLayout = layout.layout
        }


        /* === INTERNAL BUILDER FUNCTIONS === */
        /**
         * Builds FunctionDescriptor from configured layouts.
         */
        @PublishedApi
        internal fun build(): FunctionDescriptor {
            return if (returnLayout == null) {
                FunctionDescriptor.ofVoid(*arguments.toTypedArray())
            } else {
                FunctionDescriptor.of(returnLayout, *arguments.toTypedArray())
            }
        }
    }


    /* === PUBLIC BINDING FUNCTIONS === */
    /**
     * Binds a native function to MethodHandle using DSL builder.
     *
     * @param library SymbolLookup of target library
     * @param methodName Name of the native function
     * @param builder DSL block for configuring function descriptor
     *
     * @return MethodHandle for the native function
     */
    @JvmStatic
    inline fun bind(
        library: SymbolLookup,
        methodName: String,
        builder: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        val descriptor = FunctionDescriptorBuilder().apply(builder).build()
        return nativeLinker.downcallHandle(
            library.find(methodName).orElseThrow { NoSuchElementException("Cannot find symbol: $methodName") },
            descriptor
        )
    }

    /**
     * Binds a Kernel32 function to MethodHandle using DSL builder.
     *
     * @param methodName Name of the Kernel32 native function
     * @param builder DSL block for configuring function descriptor
     *
     * @return MethodHandle for the native function
     */
    @JvmStatic
    inline fun bindKernel32(
        methodName: String,
        builder: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        val descriptor = FunctionDescriptorBuilder().apply(builder).build()
        return nativeLinker.downcallHandle(
            kernel32.find(methodName).orElseThrow { NoSuchElementException("Cannot find symbol: $methodName") },
            descriptor
        )
    }

    /**
     * Binds a User32 function to MethodHandle using DSL builder.
     *
     * @param methodName Name of the User32 native function
     * @param builder DSL block for configuring function descriptor
     *
     * @return MethodHandle for the native function
     */
    @JvmStatic
    inline fun bindUser32(
        methodName: String,
        builder: FunctionDescriptorBuilder.() -> Unit
    ): MethodHandle {
        val descriptor = FunctionDescriptorBuilder().apply(builder).build()
        return nativeLinker.downcallHandle(
            user32.find(methodName).orElseThrow { NoSuchElementException("Cannot find symbol: $methodName") },
            descriptor
        )
    }
}