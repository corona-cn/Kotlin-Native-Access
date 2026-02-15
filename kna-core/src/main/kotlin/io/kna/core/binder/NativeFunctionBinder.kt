@file:Suppress("Unused", "PropertyName")
package io.kna.core.binder
import java.lang.foreign.*
import java.lang.invoke.*
import java.nio.*

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
 * ```
 * // Bind to user32!MessageBoxW
 * val user32 = SymbolLookup.libraryLookup("user32", NativeFunctionBinder.sharedArena)
 * val messageBox = NativeFunctionBinder.bind(user32, "MessageBoxW") {
 *     params(       // HWND hWnd
 *         address,  // LPCWSTR lpText
 *         address,  // LPCWSTR lpCaption
 *         address,  // UINT uType
 *         uint
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
 * For libraries other than kernel32, use the generic [bind] function:
 *
 * ```
 * // Bind to user32!MessageBoxW (alternative approach)
 * val user32 = SymbolLookup.libraryLookup("user32", NativeFunctionBinder.sharedArena)
 * val messageBox = NativeFunctionBinder.bind(user32, "MessageBoxW") {
 *     params(address, address, address, uint)
 *     returns(int)
 * }
 * ```
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
    class FunctionDescriptorBuilder {
        /* === PROVIDED VALUE LAYOUTS === */
        /* Primitive */
        /**
         * 1-byte signed integer, byte order dependent on JVM
         */
        val byte: ValueLayout.OfByte = ValueLayout.JAVA_BYTE

        /**
         * 2-byte signed integer, byte order dependent on JVM
         */
        val short: ValueLayout.OfShort = ValueLayout.JAVA_SHORT

        /**
         * 4-byte signed integer, byte order dependent on JVM
         */
        val int: ValueLayout.OfInt = ValueLayout.JAVA_INT

        /**
         * 8-byte signed integer, byte order dependent on JVM
         */
        val long: ValueLayout.OfLong = ValueLayout.JAVA_LONG

        /**
         * 4-byte float, byte order dependent on JVM
         */
        val float: ValueLayout.OfFloat = ValueLayout.JAVA_FLOAT

        /**
         * 8-byte double, byte order dependent on JVM
         */
        val double: ValueLayout.OfDouble = ValueLayout.JAVA_DOUBLE

        /**
         * 1-byte boolean, byte order dependent on JVM
         */
        val boolean: ValueLayout.OfBoolean = ValueLayout.JAVA_BOOLEAN

        /**
         * 2-byte Unicode character, byte order dependent on JVM
         */
        val char: ValueLayout.OfChar = ValueLayout.JAVA_CHAR

        /* Pointer */
        /**
         * Native pointer sized memory address (8-byte on 64-bit systems, 4-byte on 32-bit systems)
         */
        val address: AddressLayout = ValueLayout.ADDRESS

        /**
         * Native pointer with little-endian byte order
         */
        val address_le: AddressLayout = ValueLayout.ADDRESS.withOrder(ByteOrder.LITTLE_ENDIAN)

        /**
         * Native pointer with big-endian byte order
         */
        val address_be: AddressLayout = ValueLayout.ADDRESS.withOrder(ByteOrder.BIG_ENDIAN)


        /* Unsinged Primitive */
        /**
         * 1-byte unsigned integer (0 to 255) - use byte layout
         */
        val ubyte: ValueLayout.OfByte = ValueLayout.JAVA_BYTE

        /**
         * 2-byte unsigned integer (0 to 65535) - use short layout
         */
        val ushort: ValueLayout.OfShort = ValueLayout.JAVA_SHORT

        /**
         * 4-byte unsigned integer (0 to 2^32-1) - use int layout
         */
        val uint: ValueLayout.OfInt = ValueLayout.JAVA_INT

        /**
         * 8-byte unsigned integer (0 to 2^64-1) - use long layout
         */
        val ulong: ValueLayout.OfLong = ValueLayout.JAVA_LONG


        /* C/C++ Compatibility */
        /**
         * C 'char' type - 1 byte signed/unsigned (implementation-defined)
         */
        val c_char: ValueLayout.OfByte = ValueLayout.JAVA_BYTE

        /**
         * C 'short' type - 2 bytes signed
         */
        val c_short: ValueLayout.OfShort = ValueLayout.JAVA_SHORT

        /**
         * C 'int' type - 4 bytes signed
         */
        val c_int: ValueLayout.OfInt = ValueLayout.JAVA_INT

        /**
         * C 'long' type - 8 bytes on 64-bit, 4 bytes on 32-bit systems (use long for 64-bit)
         */
        val c_long: ValueLayout.OfLong = ValueLayout.JAVA_LONG

        /**
         * C 'long long' type - 8 bytes signed
         */
        val c_longlong: ValueLayout.OfLong = ValueLayout.JAVA_LONG

        /**
         * C 'float' type - 4 bytes
         */
        val c_float: ValueLayout.OfFloat = ValueLayout.JAVA_FLOAT

        /**
         * C 'double' type - 8 bytes
         */
        val c_double: ValueLayout.OfDouble = ValueLayout.JAVA_DOUBLE

        /**
         * C 'void*' type - native pointer
         */
        val c_pointer: AddressLayout = ValueLayout.ADDRESS

        /**
         * C 'size_t' type - unsigned integer size (64-bit on 64-bit systems)
         */
        val c_size_t: ValueLayout.OfLong = ValueLayout.JAVA_LONG


        /* === INTERNAL BUILDER COMPONENTS === */
        /**
         * Mutable list of argument layouts
         */
        @PublishedApi
        internal val arguments = mutableListOf<MemoryLayout>()

        /**
         * Return layout (null if not set)
         */
        @PublishedApi
        internal var returnLayout: MemoryLayout? = null


        /* === PUBLIC BUILDER FUNCTIONS === */
        /**
         * Sets function parameter layouts in order
         */
        fun params(vararg layouts: MemoryLayout) {
            arguments.addAll(layouts)
        }

        /**
         * Sets function return layout
         */
        fun returns(layout: MemoryLayout) {
            returnLayout = layout
        }


        /* === INTERNAL BUILDER FUNCTIONS === */
        /**
         * Builds FunctionDescriptor from configured layouts
         */
        @PublishedApi
        internal fun build(): FunctionDescriptor {
            if (returnLayout == null) {
                throw IllegalStateException("FunctionDescriptorBuilder returnLayout is null")
            }

            return FunctionDescriptor.of(returnLayout, *arguments.toTypedArray())
        }
    }


    /* === PUBLIC BINDING FUNCTIONS === */
    /**
     * Binds a native function to MethodHandle using DSL builder
     *
     * @param library SymbolLookup of target library
     * @param methodName Name of the native function
     * @param builder DSL block for configuring function descriptor
     *
     * @return MethodHandle for the native function
     */
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
     * Binds a Kernel32 function to MethodHandle using DSL builder
     *
     * @param methodName Name of the Kernel32 native function
     * @param builder DSL block for configuring function descriptor
     *
     * @return MethodHandle for the native function
     */
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
}