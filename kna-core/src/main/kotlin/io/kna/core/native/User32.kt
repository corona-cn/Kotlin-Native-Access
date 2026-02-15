@file:Suppress("Unused", "FunctionName")
package io.kna.core.native
import io.kna.core.binder.*

import java.lang.foreign.*

/**
 * User32 Windows API interface definitions.
 *
 * Provides native function declarations for user32.dll system calls.
 * All functions follow stdcall calling convention with Windows data types.
 */
interface User32 {
    /* === COMPANION === */
    companion object {
        /* === NATIVE BINDINGS === */
        @JvmField
        val user32 = NativeProxyInterfaceBinder.bind<User32>(NativeFunctionBinder.user32)
    }


    /* === WINDOW FUNCTIONS === */
    /**
     * Retrieves a handle to the foreground window (the window with which the user is currently working).
     *
     * @return A handle to the foreground window, or NULL if no foreground window exists
     */
    fun GetForegroundWindow(): MemorySegment

    /**
     * Retrieves a handle to the desktop window.
     *
     * @return A handle to the desktop window
     */
    fun GetDesktopWindow(): MemorySegment

    /**
     * Retrieves a handle to the window that contains the specified point.
     *
     * @param point The point to be checked, specified in screen coordinates
     *
     * @return A handle to the window that contains the point, or NULL if no window exists at the point
     */
    fun WindowFromPoint(point: Long): MemorySegment

    /**
     * Retrieves the show state and the restored, minimized, and maximized positions of the specified window.
     *
     * @param hwnd Handle to the window
     * @param lpwndpl Pointer to a WINDOWPLACEMENT structure that receives the position and show state
     *
     * @return Non-zero if successful, zero on failure
     */
    fun GetWindowPlacement(hwnd: MemorySegment, lpwndpl: MemorySegment): Int

    /**
     * Sets the show state and the restored, minimized, and maximized positions of the specified window.
     *
     * @param hwnd Handle to the window
     * @param lpwndpl Pointer to a WINDOWPLACEMENT structure that specifies the new position and show state
     *
     * @return Non-zero if successful, zero on failure
     */
    fun SetWindowPlacement(hwnd: MemorySegment, lpwndpl: MemorySegment): Int

    /**
     * Changes the size, position, and Z order of a child, pop-up, or top-level window.
     *
     * @param hWnd Handle to the window
     * @param hWndInsertAfter Handle to the window to precede the positioned window in the Z order
     * @param x New position of the left side of the window
     * @param y New position of the top of the window
     * @param cx New width of the window
     * @param cy New height of the window
     * @param uFlags Window sizing and positioning flags
     *
     * @return Non-zero if successful, zero on failure
     */
    fun SetWindowPos(
        hWnd: MemorySegment,
        hWndInsertAfter: MemorySegment,
        x: Int,
        y: Int,
        cx: Int,
        cy: Int,
        uFlags: Int
    ): Int

    /**
     * Brings the specified window to the top of the Z order.
     *
     * @param hWnd Handle to the window to bring to the top
     *
     * @return Non-zero if successful, zero on failure
     */
    fun BringWindowToTop(hWnd: MemorySegment): Int

    /**
     * Destroys the specified window.
     *
     * @param hWnd Handle to the window to destroy
     *
     * @return Non-zero if successful, zero on failure
     */
    fun DestroyWindow(hWnd: MemorySegment): Int

    /**
     * Enumerates all top-level windows on the screen by passing the handle to each window to an application-defined callback function.
     *
     * @param lpEnumFunc Pointer to an application-defined callback function
     * @param lParam Application-defined value to be passed to the callback function
     *
     * @return Non-zero if successful, zero on failure
     */
    fun EnumWindows(lpEnumFunc: MemorySegment, lParam: MemorySegment): Int

    /**
     * Retrieves the identifier of the thread that created the specified window.
     *
     * @param hWnd Handle to the window
     * @param lpdwProcessId Pointer to a variable that receives the process identifier
     *
     * @return The identifier of the thread that created the window
     */
    fun GetWindowThreadProcessId(hWnd: MemorySegment, lpdwProcessId: MemorySegment): Int


    /* === WINDOW TEXT FUNCTIONS === */
    /**
     * Retrieves the text of the specified window's title bar (if it has one).
     *
     * @param hWnd Handle to the window
     * @param lpString Pointer to the buffer that receives the text
     * @param nMaxCount Maximum number of characters to copy to the buffer
     *
     * @return The length of the copied string, in characters
     */
    fun GetWindowTextW(hWnd: MemorySegment, lpString: MemorySegment, nMaxCount: Int): Int

    /**
     * Retrieves the length, in characters, of the specified window's title bar text.
     *
     * @param hWnd Handle to the window
     *
     * @return The length of the window's title bar text, in characters
     */
    fun GetWindowTextLengthW(hWnd: MemorySegment): Int

    /**
     * Changes the text of the specified window's title bar (if it has one).
     *
     * @param hWnd Handle to the window
     * @param lpString Pointer to the new title or control text
     *
     * @return Non-zero if successful, zero on failure
     */
    fun SetWindowTextW(hWnd: MemorySegment, lpString: MemorySegment): Int

    /**
     * Retrieves the text of the specified window's class.
     *
     * @param hWnd Handle to the window
     * @param lpClassName Pointer to the buffer that receives the class name
     * @param nMaxCount Length of the buffer, in characters
     *
     * @return The number of characters copied to the buffer
     */
    fun GetClassNameW(hWnd: MemorySegment, lpClassName: MemorySegment, nMaxCount: Int): Int


    /* === WINDOW RECTANGLE FUNCTIONS === */
    /**
     * Retrieves the dimensions of the bounding rectangle of the specified window.
     *
     * @param hWnd Handle to the window
     * @param lpRect Pointer to a RECT structure that receives the screen coordinates
     *
     * @return Non-zero if successful, zero on failure
     */
    fun GetWindowRect(hWnd: MemorySegment, lpRect: MemorySegment): Int

    /**
     * Retrieves the dimensions of the client area of the specified window.
     *
     * @param hWnd Handle to the window
     * @param lpRect Pointer to a RECT structure that receives the client coordinates
     *
     * @return Non-zero if successful, zero on failure
     */
    fun GetClientRect(hWnd: MemorySegment, lpRect: MemorySegment): Int

    /**
     * Converts the client-area coordinates of a point to screen coordinates.
     *
     * @param hWnd Handle to the window whose client area is used for the conversion
     * @param lpPoint Pointer to a POINT structure that contains the client coordinates to be converted
     *
     * @return Non-zero if successful, zero on failure
     */
    fun ClientToScreen(hWnd: MemorySegment, lpPoint: MemorySegment): Int

    /**
     * Converts the screen coordinates of a point to client-area coordinates.
     *
     * @param hWnd Handle to the window whose client area is used for the conversion
     * @param lpPoint Pointer to a POINT structure that contains the screen coordinates to be converted
     *
     * @return Non-zero if successful, zero on failure
     */
    fun ScreenToClient(hWnd: MemorySegment, lpPoint: MemorySegment): Int


    /* === MESSAGE FUNCTIONS === */
    /**
     * Sends the specified message to a window or windows.
     *
     * @param hWnd Handle to the window whose window procedure will receive the message
     * @param msg Message to be sent
     * @param wParam Additional message-specific information
     * @param lParam Additional message-specific information
     *
     * @return The result of the message processing, depending on the message sent
     */
    fun SendMessageW(
        hWnd: MemorySegment,
        msg: Int,
        wParam: Long,
        lParam: Long
    ): Long

    /**
     * Places a message in the message queue associated with the thread that created the specified window.
     *
     * @param hWnd Handle to the window whose window procedure will receive the message
     * @param msg Message to be posted
     * @param wParam Additional message-specific information
     * @param lParam Additional message-specific information
     *
     * @return Non-zero if successful, zero on failure
     */
    fun PostMessageW(
        hWnd: MemorySegment,
        msg: Int,
        wParam: Long,
        lParam: Long
    ): Int

    /**
     * Dispatches messages to the appropriate window procedure.
     *
     * @param lpMsg Pointer to an MSG structure that contains the message
     *
     * @return The value returned by the window procedure
     */
    fun DispatchMessageW(lpMsg: MemorySegment): Long

    /**
     * Translates virtual-key messages into character messages.
     *
     * @param lpMsg Pointer to an MSG structure that contains message information
     *
     * @return Non-zero if the message is translated, zero otherwise
     */
    fun TranslateMessage(lpMsg: MemorySegment): Int

    /**
     * Retrieves a message from the calling thread's message queue.
     *
     * @param lpMsg Pointer to an MSG structure that receives message information
     * @param hWnd Handle to the window whose messages are to be retrieved
     * @param wMsgFilterMin Specifies the first message value to be retrieved
     * @param wMsgFilterMax Specifies the last message value to be retrieved
     *
     * @return Non-zero if a message is retrieved, zero otherwise
     */
    fun GetMessageW(
        lpMsg: MemorySegment,
        hWnd: MemorySegment,
        wMsgFilterMin: Int,
        wMsgFilterMax: Int
    ): Int

    /**
     * Dispatches incoming messages, checks the thread message queue, and processes posted messages.
     *
     * @param flags The behavior of the function (PM_NOREMOVE, PM_REMOVE, etc.)
     * @param hWnd Handle to the window whose messages are to be checked
     * @param wMsgFilterMin Specifies the first message value to be examined
     * @param wMsgFilterMax Specifies the last message value to be examined
     *
     * @return Non-zero if a message is available, zero otherwise
     */
    fun PeekMessageW(
        lpMsg: MemorySegment,
        hWnd: MemorySegment,
        wMsgFilterMin: Int,
        wMsgFilterMax: Int,
        flags: Int
    ): Int


    /* === INPUT FUNCTIONS === */
    /**
     * Synthesizes keystrokes, mouse motions, and button clicks.
     *
     * @param cInputs The number of structures in the pInputs array
     * @param pInputs Pointer to an array of INPUT structures
     * @param cbSize The size, in bytes, of an INPUT structure
     *
     * @return The number of events successfully inserted into the input stream
     */
    fun SendInput(
        cInputs: Int,
        pInputs: MemorySegment,
        cbSize: Int
    ): Int

    /**
     * Retrieves the status of the specified virtual key.
     *
     * @param nVirtKey The virtual key code
     *
     * @return The status of the specified virtual key
     */
    fun GetKeyState(nVirtKey: Int): Short

    /**
     * Retrieves the asynchronous status of the specified virtual key.
     *
     * @param vKey The virtual key code
     *
     * @return The asynchronous status of the specified virtual key
     */
    fun GetAsyncKeyState(vKey: Int): Short

    /**
     * Retrieves the cursor's position in screen coordinates.
     *
     * @param lpPoint Pointer to a POINT structure that receives the screen coordinates
     *
     * @return Non-zero if successful, zero on failure
     */
    fun GetCursorPos(lpPoint: MemorySegment): Int

    /**
     * Moves the cursor to the specified screen coordinates.
     *
     * @param x New x-coordinate of the cursor
     * @param y New y-coordinate of the cursor
     *
     * @return Non-zero if successful, zero on failure
     */
    fun SetCursorPos(x: Int, y: Int): Int


    /* === DISPLAY FUNCTIONS === */
    /**
     * Retrieves information about one or all system display devices.
     *
     * @param lpDevice Pointer to a string that specifies the display device name
     * @param dwDevNum Index value that specifies the display device of interest
     * @param lpDisplayDevice Pointer to a DISPLAY_DEVICEW structure that receives information
     * @param dwFlags Set to zero
     *
     * @return Non-zero if successful, zero on failure
     */
    fun EnumDisplayDevicesW(
        lpDevice: MemorySegment,
        dwDevNum: Int,
        lpDisplayDevice: MemorySegment,
        dwFlags: Int
    ): Int

    /**
     * Retrieves information about the display device's current mode.
     *
     * @param lpszDeviceName Pointer to a string that specifies the display device name
     * @param lpDevMode Pointer to a DEVMODEW structure that receives the current settings
     *
     * @return Non-zero if successful, zero on failure
     */
    fun EnumDisplaySettingsW(
        lpszDeviceName: MemorySegment,
        iModeNum: Int,
        lpDevMode: MemorySegment
    ): Int


    /* === CLIPBOARD FUNCTIONS === */
    /**
     * Opens the clipboard for examination and prevents other applications from modifying the clipboard content.
     *
     * @param hWndNewOwner Handle to the window to be associated with the open clipboard
     *
     * @return Non-zero if successful, zero on failure
     */
    fun OpenClipboard(hWndNewOwner: MemorySegment): Int

    /**
     * Closes the clipboard.
     *
     * @return Non-zero if successful, zero on failure
     */
    fun CloseClipboard(): Int

    /**
     * Retrieves the handle to the current clipboard data in the specified format.
     *
     * @param uFormat The clipboard format
     *
     * @return Handle to the clipboard data if successful, NULL on failure
     */
    fun GetClipboardData(uFormat: Int): MemorySegment

    /**
     * Empties the clipboard and frees handles to data in the clipboard.
     *
     * @return Non-zero if successful, zero on failure
     */
    fun EmptyClipboard(): Int

    /**
     * Places data on the clipboard in a specified clipboard format.
     *
     * @param uFormat The clipboard format
     * @param hMem Handle to the data in the specified format
     *
     * @return Non-zero if successful, zero on failure
     */
    fun SetClipboardData(uFormat: Int, hMem: MemorySegment): MemorySegment


    /* === CARET FUNCTIONS === */
    /**
     * Creates a new caret for the specified window.
     *
     * @param hWnd Handle to the window that owns the caret
     * @param hBitmap Handle to the bitmap that defines the caret shape
     * @param nWidth Width of the caret
     * @param nHeight Height of the caret
     *
     * @return Non-zero if successful, zero on failure
     */
    fun CreateCaret(
        hWnd: MemorySegment,
        hBitmap: MemorySegment,
        nWidth: Int,
        nHeight: Int
    ): Int

    /**
     * Destroys the caret's current shape.
     *
     * @return Non-zero if successful, zero on failure
     */
    fun DestroyCaret(): Int

    /**
     * Removes the caret from the current window.
     *
     * @param hWnd Handle to the window that contains the caret
     *
     * @return Non-zero if successful, zero on failure
     */
    fun HideCaret(hWnd: MemorySegment): Int

    /**
     * Displays the caret on the screen at the caret's current position.
     *
     * @param hWnd Handle to the window that contains the caret
     *
     * @return Non-zero if successful, zero on failure
     */
    fun ShowCaret(hWnd: MemorySegment): Int

    /**
     * Moves the caret to the specified coordinates.
     *
     * @param x New x-coordinate of the caret
     * @param y New y-coordinate of the caret
     *
     * @return Non-zero if successful, zero on failure
     */
    fun SetCaretPos(x: Int, y: Int): Int


    /* === TIMER FUNCTIONS === */
    /**
     * Creates a timer with the specified time-out value.
     *
     * @param hWnd Handle to the window to be associated with the timer
     * @param nIDEvent Timer identifier
     * @param uElapse Time-out value, in milliseconds
     * @param lpTimerFunc Pointer to the function to be notified when the time-out value elapses
     *
     * @return Timer identifier if successful, NULL on failure
     */
    fun SetTimer(
        hWnd: MemorySegment,
        nIDEvent: Int,
        uElapse: Int,
        lpTimerFunc: MemorySegment
    ): Long

    /**
     * Destroys the specified timer.
     *
     * @param hWnd Handle to the window associated with the timer
     * @param uIDEvent Timer identifier
     *
     * @return Non-zero if successful, zero on failure
     */
    fun KillTimer(hWnd: MemorySegment, uIDEvent: Long): Int


    /* === MENU FUNCTIONS === */
    /**
     * Retrieves a handle to the menu assigned to the specified window.
     *
     * @param hWnd Handle to the window
     *
     * @return Handle to the menu if successful, NULL on failure
     */
    fun GetMenu(hWnd: MemorySegment): MemorySegment

    /**
     * Retrieves a handle to the system menu for the specified window.
     *
     * @param hWnd Handle to the window
     * @param bRevert If FALSE, returns a handle to the system menu; if TRUE, resets the system menu
     *
     * @return Handle to the system menu if successful, NULL on failure
     */
    fun GetSystemMenu(hWnd: MemorySegment, bRevert: Int): MemorySegment

    /**
     * Draws the specified menu bar.
     *
     * @param hWnd Handle to the window whose menu bar is to be redrawn
     *
     * @return Non-zero if successful, zero on failure
     */
    fun DrawMenuBar(hWnd: MemorySegment): Int


    /* === HOOK FUNCTIONS === */
    /**
     * Installs an application-defined hook procedure into a hook chain.
     *
     * @param idHook Type of hook procedure to be installed
     * @param lpfn Pointer to the hook procedure
     * @param hmod Handle to the DLL containing the hook procedure
     * @param dwThreadId Thread identifier with which the hook procedure is to be associated
     *
     * @return Handle to the hook procedure if successful, NULL on failure
     */
    fun SetWindowsHookExW(
        idHook: Int,
        lpfn: MemorySegment,
        hmod: MemorySegment,
        dwThreadId: Int
    ): MemorySegment

    /**
     * Removes a hook procedure installed in a hook chain by the SetWindowsHookEx function.
     *
     * @param hhk Handle to the hook to be removed
     *
     * @return Non-zero if successful, zero on failure
     */
    fun UnhookWindowsHookEx(hhk: MemorySegment): Int

    /**
     * Passes the hook information to the next hook procedure in the current hook chain.
     *
     * @param hhk Handle to the current hook
     * @param nCode Hook code passed to the current hook procedure
     * @param wParam Value passed to the current hook procedure
     * @param lParam Value passed to the current hook procedure
     *
     * @return Value returned by the next hook procedure in the chain
     */
    fun CallNextHookEx(
        hhk: MemorySegment,
        nCode: Int,
        wParam: Long,
        lParam: Long
    ): Long
}