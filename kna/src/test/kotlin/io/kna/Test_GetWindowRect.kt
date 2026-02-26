@file:Suppress("Unused", "ClassName")
package io.kna
import io.kna.bridge.*
import io.kna.library.*

object Test_GetWindowRect {
    private val user32: User32 = NativeInterfaceProxy.proxyFor<User32>(Natives.user32Lookup)
    private val rect: Rect = Rect()

    fun test() {
        val hWnd = user32.GetDesktopWindow()
        val result = user32.GetWindowRect(hWnd, rect)
        log("GetWindowRect Result: $result")
        log("RectStruct Result: $rect")
    }
}