@file:Suppress("Unused")
package io.kna.resolver
import java.util.concurrent.*
import java.lang.reflect.*

import kotlin.reflect.*

/* === METHOD RESOLVER === */
object MethodResolver {
    /* === INTERNAL CACHES === */
    private val methodCache = ConcurrentHashMap<Method, MethodInfo>()


    /* === INTERNAL MODELS === */
    data class MethodInfo(
        val name: String,
        val parameterInfos: List<ParameterInfo>,
        val returnKlass: KClass<*>
    )

    data class ParameterInfo(
        val name: String,
        val klass: KClass<*>,
        val annotations: List<KClass<out Annotation>>
    )


    /* === PUBLIC RESOLVER FUNCTIONS === */
    fun resolveMethodInfo(method: Method): MethodInfo {
        return methodCache.getOrPut(method) {
            MethodInfo(
                name = method.name,
                parameterInfos = method.parameters.map { param ->
                    ParameterInfo(
                        name = param.name,
                        klass = param.type.kotlin,
                        annotations = param.annotations.map { annotation -> annotation.annotationClass }
                    )
                },
                returnKlass = method.returnType.kotlin,
            )
        }
    }

    inline fun forEachMethodInfo(
        method: Method,
        action: (name: String, parameterInfos: List<ParameterInfo>, returnType: KClass<*>) -> Unit
    ) {
        resolveMethodInfo(method).let { info ->
            action(info.name, info.parameterInfos, info.returnKlass)
        }
    }


    /* === PUBLIC MANAGEMENT FUNCTIONS === */
    fun clearCache(method: Method) {
        methodCache.remove(method)
    }

    fun clearAllCaches() {
        methodCache.clear()
    }
}