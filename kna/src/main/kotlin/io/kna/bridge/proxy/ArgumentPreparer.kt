@file:Suppress("Unused", "Unchecked_cast")
package io.kna.bridge.proxy
import io.kna.resolver.*
import io.kna.layout.*

import java.util.concurrent.*
import java.lang.foreign.*
import java.lang.reflect.*

import kotlin.reflect.*

/* === ARGUMENT PREPARER === */
object ArgumentPreparer {
    /* === CONSTANTS === */
    private val PREDEFINED_TYPES = setOf(
        Byte::class, Short::class, Int::class, Long::class,
        Float::class, Double::class, Boolean::class, Char::class,
        MemorySegment::class, String::class
    )

    private val OUTPUT_COMBINATIONS = setOf(
        ParameterPassingParser.PassingCombination.OUT_VALUE,
        ParameterPassingParser.PassingCombination.IN_OUT_VALUE,
        ParameterPassingParser.PassingCombination.OUT_REF,
        ParameterPassingParser.PassingCombination.IN_OUT_REF
    )

    private val ALLOCATION_STRATEGIES = ConcurrentHashMap<
        ParameterPassingParser.PassingCombination,
        AllocationStrategy
    >()

    private const val MAX_PARAMETERS = 32


    /* === INTERNAL CACHES === */
    private val methodInfoCache = ConcurrentHashMap<Method, MethodPreparationInfo>()


    /* === INITIALIZATION === */
    init {
        ParameterPassingParser.PassingCombination.entries.forEach { combo ->
            ALLOCATION_STRATEGIES[combo] = when (combo) {
                ParameterPassingParser.PassingCombination.OUT_VALUE -> AllocationStrategy.ALLOCATE_ONLY
                ParameterPassingParser.PassingCombination.OUT_REF -> AllocationStrategy.ALLOCATE_ONLY
                ParameterPassingParser.PassingCombination.IN_OUT_VALUE -> AllocationStrategy.ALLOCATE_AND_WRITE
                ParameterPassingParser.PassingCombination.IN_OUT_REF -> AllocationStrategy.ALLOCATE_AND_WRITE
                ParameterPassingParser.PassingCombination.IN_VALUE -> AllocationStrategy.ALLOCATE_AND_WRITE
                ParameterPassingParser.PassingCombination.IN_REF -> AllocationStrategy.ALLOCATE_AND_WRITE
            }
        }
    }


    /* === INTERNAL TYPES === */
    private enum class AllocationStrategy {
        ALLOCATE_ONLY,
        ALLOCATE_AND_WRITE
    }

    private sealed class ParamInstruction {
        data class Predefined(val index: Int) : ParamInstruction()

        data class NativeType(
            val index: Int,
            val strategy: AllocationStrategy,
            val isOutput: Boolean
        ) : ParamInstruction()

        data class Unknown(val index: Int) : ParamInstruction()
    }

    private data class MethodPreparationInfo(
        val instructions: List<ParamInstruction>,
        val returnType: KClass<*>,
        val outputIndices: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            if (javaClass != other?.javaClass) {
                return false
            }

            other as MethodPreparationInfo

            if (instructions != other.instructions) {
                return false
            }

            if (returnType != other.returnType) {
                return false
            }

            if (!outputIndices.contentEquals(other.outputIndices)) {
                return false
            }

            return true
        }

        override fun hashCode(): Int {
            var result = instructions.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + outputIndices.contentHashCode()
            return result
        }
    }


    /* === PUBLIC PROCESSORS === */
    /* Core Preparer */
    fun prepareArguments(
        method: Method,
        args: Array<out Any>,
        arena: Arena
    ): PreparedArguments {
        val prepInfo = getMethodPreparationInfo(method)
        val preparedArgs = arrayOfNulls<Any>(args.size)

        val outputIndices = IntArray(MAX_PARAMETERS) { -1 }
        val outputStructs = arrayOfNulls<NativeType>(MAX_PARAMETERS)
        var outputCount = 0

        prepInfo.instructions.forEach { instruction ->
            when (instruction) {
                is ParamInstruction.Predefined -> {
                    preparedArgs[instruction.index] = args[instruction.index]
                }

                is ParamInstruction.NativeType -> {
                    val struct = args[instruction.index] as? NativeType
                    if (struct == null) {
                        preparedArgs[instruction.index] = MemorySegment.NULL
                    } else {
                        val segment = when (instruction.strategy) {
                            AllocationStrategy.ALLOCATE_ONLY -> {
                                arena.allocate(struct.getLayout())
                            }

                            AllocationStrategy.ALLOCATE_AND_WRITE -> {
                                struct.allocate(arena)
                            }
                        }

                        preparedArgs[instruction.index] = segment

                        if (instruction.isOutput) {
                            outputIndices[outputCount] = instruction.index
                            outputStructs[outputCount] = struct
                            outputCount++
                        }
                    }
                }

                is ParamInstruction.Unknown -> {
                    preparedArgs[instruction.index] = args[instruction.index]
                }
            }
        }

        return PreparedArguments(
            args = preparedArgs,
            outputIndices = outputIndices,
            outputStructs = outputStructs,
            outputCount = outputCount,
            returnType = prepInfo.returnType
        )
    }

    /* Output Processor */
    fun writeBackOutputs(preparedArgs: PreparedArguments) {
        val outputIndices = preparedArgs.outputIndices
        val outputStructs = preparedArgs.outputStructs
        val outputCount = preparedArgs.outputCount
        val args = preparedArgs.args
        for (i in 0 until outputCount) {
            val index = outputIndices[i]
            val struct = outputStructs[i] ?: continue
            val segment = args[index] as? MemorySegment ?: throw RuntimeException("No segment found for output parameter at index $index")
            struct.readFrom(segment)
        }
    }


    /* === INTERNAL PROCESSORS === */
    private fun getMethodPreparationInfo(method: Method): MethodPreparationInfo {
        return methodInfoCache.getOrPut(method) {
            val methodInfo = MethodResolver.resolveMethodInfo(method)
            val paramPassings = ParameterPassingParser.parseMethodParameters(methodInfo)

            val instructions = methodInfo.parameterInfos.mapIndexed { index, info ->
                val passing = paramPassings[index]
                when {
                    isPredefinedType(info.klass) -> {
                        ParamInstruction.Predefined(index)
                    }

                    NativeType::class.java.isAssignableFrom(info.klass.java) -> {
                        ParamInstruction.NativeType(
                            index = index,
                            strategy = ALLOCATION_STRATEGIES[passing]!!,
                            isOutput = passing in OUTPUT_COMBINATIONS
                        )
                    }

                    else -> {
                        ParamInstruction.Unknown(index)
                    }
                }
            }

            val outputIndices = instructions.mapNotNull { instruction ->
                if (instruction is ParamInstruction.NativeType && instruction.isOutput) {
                    instruction.index
                } else null
            }.toIntArray()

            MethodPreparationInfo(
                instructions = instructions,
                returnType = methodInfo.returnKlass,
                outputIndices = outputIndices
            )
        }
    }

    /* Type Checkers */
    fun isPredefinedType(kClass: KClass<*>): Boolean {
        return kClass in PREDEFINED_TYPES
    }


    /* === PUBLIC RESULT CLASS === */
    class PreparedArguments(
        val args: Array<Any?>,
        val outputIndices: IntArray,
        val outputStructs: Array<NativeType?>,
        val outputCount: Int,
        val returnType: KClass<*>
    ) {
        fun hasOutputs(): Boolean {
            return outputCount > 0
        }

        fun writeBackOutputs() {
            writeBackOutputs(this)
        }
    }


    /* === PUBLIC MANAGEMENT FUNCTIONS === */
    fun clearCache() {
        methodInfoCache.clear()
    }

    fun clearCacheForMethod(method: Method) {
        methodInfoCache.remove(method)
    }
}