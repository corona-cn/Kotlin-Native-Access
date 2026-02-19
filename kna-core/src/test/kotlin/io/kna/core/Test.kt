package io.kna.core
import io.kna.core.layout.struct.*
import io.kna.core.binder.*

import java.lang.foreign.*

object Test {
    data class Rect(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0) : NativeStruct()
    data class Point(var x: Int = 0, var y: Int = 0) : NativeStruct()

    interface User32 {
        fun GetForegroundWindow(): MemorySegment
        fun GetWindowRect(hWnd: MemorySegment, lpRect: Rect): Int
        fun GetCursorPos(lpPoint: Point): Int
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val user32 = NativeInterfaceProxyBinder.bind<User32>(NativeFunctionBinder.user32)

        val hWnd = user32.GetForegroundWindow()
        log("Foreground window handle: $hWnd")

        val rect = Rect()
        val result = user32.GetWindowRect(hWnd, rect)
        log("GetWindowRect result: $result")
        log("Window rectangle: $rect")

        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        log("Window size: ${width}x$height")

        val point = Point()
        val cursorResult = user32.GetCursorPos(point)
        log("GetCursorPos result: $cursorResult")
        log("Cursor position: $point")
    }
}