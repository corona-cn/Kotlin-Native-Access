@file:Suppress("Unused", "Unchecked_cast")
package io.kna.bridge.proxy
import java.util.concurrent.*
import java.lang.reflect.*
import java.lang.invoke.*

/* === PROXY FACTORY === */
object ProxyHandlerFactory {
    /* === INTERNAL CACHES === */
    private val methodHandleCache = ConcurrentHashMap<Method, MethodHandle>()


    /* === PUBLIC PROXY HANDLER FACTORY === */
    fun <InterfaceType : Any> proxyHandlerFor(
        interfaceClass: Class<InterfaceType>,
        methodTransformer: (method: Method) -> MethodHandle,
        handler: (proxy: Any, method: Method, methodHandle: MethodHandle, args: Array<out Any>) -> Any?
    ): InterfaceType {
        if (!interfaceClass.isInterface) {
            throw RuntimeException("$interfaceClass is not an interface")
        }

        return Proxy.newProxyInstance(interfaceClass.classLoader, arrayOf(interfaceClass)) { proxy, method, args ->
            val handle = methodHandleCache.getOrPut(method) { methodTransformer(method) }
            when {
                args == null -> {
                    when {
                        method.parameterCount == 0 -> {
                            try {
                                val result = handle.invoke()
                                when {
                                    method.returnType == Void.TYPE -> null
                                    else -> result
                                }
                            } catch (e: Throwable) {
                                throw RuntimeException("Failed to invoke method handle of ${method.name}", e)
                            }
                        }

                        else -> {
                            throw RuntimeException("Method ${method.name} requires ${method.parameterCount} parameters, but got null args array")
                        }
                    }
                }

                args.isEmpty() -> {
                    when {
                        method.parameterCount == 0 -> {
                            try {
                                val result = handle.invoke()
                                when {
                                    method.returnType == Void.TYPE -> null
                                    else -> result
                                }
                            } catch (e: Throwable) {
                                throw RuntimeException("Failed to invoke method handle of ${method.name}", e)
                            }
                        }

                        else -> {
                            throw RuntimeException("Method ${method.name} requires ${method.parameterCount} parameters, but got empty args array")
                        }
                    }
                }

                else -> {
                    handler(proxy, method, handle, args)
                }
            }
        } as InterfaceType
    }
}