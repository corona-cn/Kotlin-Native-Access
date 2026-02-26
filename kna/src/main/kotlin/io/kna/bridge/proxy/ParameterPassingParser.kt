@file:Suppress("Unused")
package io.kna.bridge.proxy
import io.kna.resolver.*
import io.kna.passing.*

import java.util.concurrent.*

import kotlin.reflect.*

/* === PARAMETER PASSING PARSER === */
object ParameterPassingParser {
    /* === INTERNAL CACHES === */
    private val methodParametersCache = ConcurrentHashMap<MethodResolver.MethodInfo, List<PassingCombination>>()


    /* === ENUM DEFINITIONS === */
    enum class Direction { IN, OUT, IN_OUT }

    enum class PassBy { VALUE, REF }

    enum class PassingCombination {
        /* === DIRECTION ENUMS === */
        IN_VALUE,

        OUT_VALUE,

        IN_OUT_VALUE,


        /* === PASSBY ENUMS === */
        IN_REF,

        OUT_REF,

        IN_OUT_REF;


        /* === COMPANION === */
        companion object {
            /* === PASSING COMBINATION FACTORY === */
            fun of(direction: Direction, passBy: PassBy): PassingCombination {
                return when (direction) {
                    Direction.IN -> when (passBy) {
                        PassBy.VALUE -> IN_VALUE
                        PassBy.REF -> IN_REF
                    }

                    Direction.OUT -> when (passBy) {
                        PassBy.VALUE -> OUT_VALUE
                        PassBy.REF -> OUT_REF
                    }

                    Direction.IN_OUT -> when (passBy) {
                        PassBy.VALUE -> IN_OUT_VALUE
                        PassBy.REF -> IN_OUT_REF
                    }
                }
            }
        }
    }


    /* === PUBLIC PARSING FUNCTIONS === */
    fun parsePassingAnnotations(annotations: List<KClass<out Annotation>>): PassingCombination {
        var direction = Direction.IN
        var passBy = PassBy.VALUE

        annotations.forEach { anno ->
            when (anno.java) {
                In::class.java -> direction = Direction.IN
                Out::class.java -> direction = Direction.OUT
                InOut::class.java -> direction = Direction.IN_OUT
                ByRef::class.java -> passBy = PassBy.REF
                ByValue::class.java -> passBy = PassBy.VALUE
            }
        }

        return PassingCombination.of(direction, passBy)
    }

    fun parseMethodParameters(methodInfo: MethodResolver.MethodInfo): List<PassingCombination> {
        return methodParametersCache.getOrPut(methodInfo) {
            methodInfo.parameterInfos.map { info ->
                parsePassingAnnotations(info.annotations)
            }
        }
    }


    /* === PUBLIC MANAGEMENT FUNCTIONS === */
    fun clearCache() {
        methodParametersCache.clear()
    }

    fun clearCacheForMethodInfo(methodInfo: MethodResolver.MethodInfo) {
        methodParametersCache.remove(methodInfo)
    }
}