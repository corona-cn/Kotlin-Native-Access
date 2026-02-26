@file:Suppress("Unused", "Unchecked_cast")
package io.kna.layout
import java.lang.foreign.*

/* === NATIVE STRUCT === */
abstract class NativeStruct : NativeType() {
    /* === PUBLIC COMPONENTS === */
    /* Accessor */
    override fun computeLayout(): MemoryLayout {
        val layouts = getFieldList().map { field ->
            getFieldLayout(field).withName(field.name)
        }
        return MemoryLayout.structLayout(*layouts.toTypedArray())
    }

    override fun computeFields(): List<TypeField> {
        val layout = getLayout()
        return getFieldList().map { field ->
            val offset = layout.byteOffset(MemoryLayout.PathElement.groupElement(field.name))
            TypeField(field, offset, getFieldLayout(field))
        }
    }

    override fun allocate(arena: SegmentAllocator): MemorySegment {
        val segment = arena.allocate(getLayout())
        writeTo(segment)
        return segment
    }

    /* Operator */
    override fun writeTo(segment: MemorySegment) {
        getFields().forEach { (field, offset, layout) ->
            val value = field.get(this)
            when (layout) {
                is GroupLayout -> {
                    val struct = value as? NativeType ?: throw RuntimeException("Expected NativeType for GroupLayout, got ${value?.javaClass}")
                    struct.writeTo(segment.asSlice(offset, layout.byteSize()))
                }

                else -> {
                    writeFieldToSegment(segment, offset, layout, value)
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
}