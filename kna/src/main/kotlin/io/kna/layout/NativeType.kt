@file:Suppress("Unused", "Unchecked_cast")
package io.kna.layout
import java.util.concurrent.*
import java.lang.foreign.*
import java.lang.reflect.*

/* === NATIVE TYPE === */
abstract class NativeType {
    /* === COMPANION === */
    companion object {
        /* === INTERNAL CACHES === */
        private val layoutCache = ConcurrentHashMap<Class<out NativeType>, MemoryLayout>()
        private val fieldCache = ConcurrentHashMap<Class<out NativeType>, List<TypeField>>()

        /* === UNSAFE FOR INSTANCE ALLOCATION === */
        private val UNSAFE: sun.misc.Unsafe = run {
            val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
            unsafeField.isAccessible = true
            unsafeField.get(null) as sun.misc.Unsafe
        }

        /* === INSTANCE ALLOCATION CACHE === */
        private val instanceCache = ConcurrentHashMap<Class<out NativeType>, NativeType>()

        /* === GET TEMPORARY INSTANCE FOR LAYOUT COMPUTATION === */
        internal fun getTemporaryInstance(type: Class<out NativeType>): NativeType {
            return instanceCache.getOrPut(type) {
                UNSAFE.allocateInstance(type) as NativeType
            }
        }
    }


    /* === PUBLIC MODAL === */
    internal data class TypeField(
        val field: Field,
        val offset: Long,
        val layout: MemoryLayout
    ) {
        init {
            field.isAccessible = true
        }
    }


    /* === INTERNAL FUNCTIONS === */
    internal fun getLayout(): MemoryLayout {
        return layoutCache.getOrPut(this::class.java) {
            computeLayout()
        }
    }

    internal fun getFields(): List<TypeField> {
        return fieldCache.getOrPut(this::class.java) {
            computeFields()
        }
    }


    /* === ABSTRACT FUNCTIONS === */
    /* Internal-Only */
    internal abstract fun computeLayout(): MemoryLayout

    internal abstract fun computeFields(): List<TypeField>

    /* Public */
    abstract fun allocate(arena: SegmentAllocator): MemorySegment

    abstract fun writeTo(segment: MemorySegment)

    abstract fun readFrom(segment: MemorySegment)


    /* === INHERITANCE HELPER FUNCTIONS === */
    /* Helper Accessor */
    protected fun getFieldList(): List<Field> {
        return this::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !Modifier.isTransient(it.modifiers) }
            .also { fields -> fields.forEach { it.isAccessible = true } }
    }

    protected fun getFieldLayout(field: Field): MemoryLayout {
        return when (field.type) {
            Byte::class.java -> ValueLayout.JAVA_BYTE
            Short::class.java -> ValueLayout.JAVA_SHORT
            Int::class.java -> ValueLayout.JAVA_INT
            Long::class.java -> ValueLayout.JAVA_LONG
            Float::class.java -> ValueLayout.JAVA_FLOAT
            Double::class.java -> ValueLayout.JAVA_DOUBLE
            Boolean::class.java -> ValueLayout.JAVA_BOOLEAN
            Char::class.java -> ValueLayout.JAVA_CHAR
            MemorySegment::class.java -> ValueLayout.ADDRESS
            else -> {
                if (NativeType::class.java.isAssignableFrom(field.type)) {
                    val type = field.type as Class<out NativeType>
                    val tempInstance = getTemporaryInstance(type)
                    tempInstance.getLayout()
                } else {
                    throw RuntimeException("Unsupported field type: ${field.type} in ${this::class.java.simpleName}")
                }
            }
        }
    }

    /* Read-Write */
    protected fun writeFieldToSegment(
        segment: MemorySegment,
        offset: Long,
        layout: MemoryLayout,
        value: Any?
    ) {
        when (layout) {
            is ValueLayout.OfByte -> segment.set(layout, offset, value as Byte)
            is ValueLayout.OfShort -> segment.set(layout, offset, value as Short)
            is ValueLayout.OfInt -> segment.set(layout, offset, value as Int)
            is ValueLayout.OfLong -> segment.set(layout, offset, value as Long)
            is ValueLayout.OfFloat -> segment.set(layout, offset, value as Float)
            is ValueLayout.OfDouble -> segment.set(layout, offset, value as Double)
            is ValueLayout.OfBoolean -> segment.set(layout, offset, value as Boolean)
            is ValueLayout.OfChar -> segment.set(layout, offset, value as Char)
            is AddressLayout -> segment.set(layout, offset, value as MemorySegment)
            is GroupLayout -> {
                val struct = value as? NativeType ?: throw RuntimeException("Expected NativeType for GroupLayout, got ${value?.javaClass}")
                struct.writeTo(segment.asSlice(offset, layout.byteSize()))
            }
            else -> throw RuntimeException("Unsupported layout type: $layout")
        }
    }

    protected fun readFieldFromSegment(
        segment: MemorySegment,
        offset: Long,
        layout: MemoryLayout
    ): Any? {
        return when (layout) {
            is ValueLayout.OfByte -> segment.get(layout, offset)
            is ValueLayout.OfShort -> segment.get(layout, offset)
            is ValueLayout.OfInt -> segment.get(layout, offset)
            is ValueLayout.OfLong -> segment.get(layout, offset)
            is ValueLayout.OfFloat -> segment.get(layout, offset)
            is ValueLayout.OfDouble -> segment.get(layout, offset)
            is ValueLayout.OfBoolean -> segment.get(layout, offset)
            is ValueLayout.OfChar -> segment.get(layout, offset)
            is AddressLayout -> segment.get(layout, offset)
            is GroupLayout -> segment.asSlice(offset, layout.byteSize())
            else -> throw RuntimeException("Unsupported layout type: $layout")
        }
    }
}