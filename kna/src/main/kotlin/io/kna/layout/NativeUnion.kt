@file:Suppress("Unused", "Unchecked_cast")
package io.kna.layout
import java.lang.foreign.*

/* === NATIVE UNION === */
@Deprecated(
    message = "NativeUnion requires compile-time code generation to properly handle field interception and active field tracking. Use annotation processor or compiler plugin to generate correct implementations.",
    level = DeprecationLevel.ERROR
)
abstract class NativeUnion : NativeType() {
    /* === PUBLIC COMPONENTS === */
    /* Accessor */
    override fun computeLayout(): MemoryLayout {
        val layouts = getFieldList().map { field ->
            getFieldLayout(field).withName(field.name)
        }
        return MemoryLayout.unionLayout(*layouts.toTypedArray())
    }

    override fun computeFields(): List<TypeField> {
        return getFieldList().map { field ->
            TypeField(field, 0L, getFieldLayout(field))
        }
    }

    override fun allocate(arena: SegmentAllocator): MemorySegment {
        val segment = arena.allocate(getLayout())
        writeTo(segment)
        return segment
    }

    /* Operator */
    override fun writeTo(segment: MemorySegment) {
        getFields().forEach { (field, _, layout) ->
            val value = field.get(this)
            when (layout) {
                is GroupLayout -> {
                    val struct = value as? NativeType ?: throw RuntimeException("Expected NativeType for GroupLayout, got ${value?.javaClass}")
                    struct.writeTo(segment.asSlice(0L, layout.byteSize()))
                }

                else -> {
                    writeFieldToSegment(segment, 0L, layout, value)
                }
            }
        }
    }

    override fun readFrom(segment: MemorySegment) {
        getFields().forEach { (field, offset, layout) ->
            when (layout) {
                is GroupLayout -> {
                    val struct = field.get(this) as? NativeType ?: run {
                        val type = field.type as Class<out NativeType>
                        type.getDeclaredConstructor().newInstance()
                    }
                    struct.readFrom(segment.asSlice(offset, layout.byteSize()))
                    field.set(this, struct)
                }

                else -> {
                    val value = readFieldFromSegment(segment, offset, layout)
                    field.set(this, value)
                }
            }
        }
    }

    /* === PUBLIC EXTENSION FUNCTIONS === */
    fun sizeOf(): Long {
        return getLayout().byteSize()
    }

    fun clear(arena: SegmentAllocator): MemorySegment {
        val segment = arena.allocate(getLayout())
        segment.fill(0x00.toByte())
        return segment
    }
}