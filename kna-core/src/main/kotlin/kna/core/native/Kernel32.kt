@file:Suppress("Unused", "Nothing_to_inline", "FunctionName")
package kna.core.native
import kna.core.binder.*

import java.lang.foreign.*

/**
 * Kernel32 Windows API interface definitions.
 *
 * Provides native function declarations for kernel32.dll system calls.
 * All functions follow stdcall calling convention with Windows data types.
 */
interface Kernel32 {
    /* === COMPANION === */
    companion object {
        /* === NATIVE BINDINGS === */
        @JvmField
        val kernel32 = NativeProxyInterfaceBinder.bind<Kernel32>(NativeFunctionBinder.kernel32)
    }


    /* === PROCESS & THREAD FUNCTIONS === */
    /**
     * Retrieves the process identifier of the calling process.
     *
     * @return The process identifier of the current process
     */
    fun GetCurrentProcessId(): Int

    /**
     * Retrieves the calling thread's last-error code value.
     *
     * @return The last-error code for the current thread
     */
    fun GetLastError(): Int

    /**
     * Retrieves a handle to the specified standard device.
     *
     * @param nStdHandle Standard device identifier:
     *                   -10 (STD_INPUT_HANDLE)
     *                   -11 (STD_OUTPUT_HANDLE)
     *                   -12 (STD_ERROR_HANDLE)
     * @return Handle to the specified device, or INVALID_HANDLE_VALUE (-1) on failure
     */
    fun GetStdHandle(nStdHandle: Int): MemorySegment

    /**
     * Opens an existing local process object.
     *
     * @param dwDesiredAccess Access mask to the process object
     * @param bInheritHandle Whether returned handle can be inherited
     * @param dwProcessId Identifier of the local process to open
     * @return Open handle to the process, or NULL on failure
     */
    fun OpenProcess(dwDesiredAccess: Int, bInheritHandle: Int, dwProcessId: Long): MemorySegment

    /**
     * Closes an open object handle.
     *
     * @param hObject Valid handle to any open object
     * @return Non-zero if successful, zero on failure
     */
    fun CloseHandle(hObject: MemorySegment): Int


    /* === PROCESS MEMORY FUNCTIONS === */
    /**
     * Reads data from an area of memory in a specified process.
     *
     * @param hProcess Handle to the process with PROCESS_VM_READ access
     * @param lpBaseAddress Pointer to the base address in the remote process
     * @param lpBuffer Pointer to a buffer receiving the contents
     * @param nSize Number of bytes to read
     * @param lpNumberOfBytesRead Pointer to variable receiving bytes transferred
     * @return Non-zero if successful, zero on failure
     */
    fun ReadProcessMemory(
        hProcess: MemorySegment,
        lpBaseAddress: MemorySegment,
        lpBuffer: MemorySegment,
        nSize: Long,
        lpNumberOfBytesRead: MemorySegment
    ): Int

    /**
     * Writes data to an area of memory in a specified process.
     *
     * @param hProcess Handle to the process with PROCESS_VM_WRITE and PROCESS_VM_OPERATION access
     * @param lpBaseAddress Pointer to the base address in the remote process
     * @param lpBuffer Pointer to the buffer containing data to write
     * @param nSize Number of bytes to write
     * @param lpNumberOfBytesWritten Pointer to variable receiving bytes transferred
     * @return Non-zero if successful, zero on failure
     */
    fun WriteProcessMemory(
        hProcess: MemorySegment,
        lpBaseAddress: MemorySegment,
        lpBuffer: MemorySegment,
        nSize: Long,
        lpNumberOfBytesWritten: MemorySegment
    ): Int


    /* === PROCESS TOOL HELP FUNCTIONS === */
    /**
     * Creates a snapshot of the specified processes, heaps, modules, and threads.
     *
     * @param dwFlags Portions of the system to include in the snapshot
     * @param th32ProcessID Process identifier to be included in snapshot
     * @return Handle to the snapshot on success, INVALID_HANDLE_VALUE on failure
     */
    fun CreateToolhelp32Snapshot(dwFlags: Long, th32ProcessID: Long): MemorySegment

    /**
     * Retrieves information about the first process in a snapshot.
     *
     * @param hSnapshot Handle from CreateToolhelp32Snapshot
     * @param lppe Pointer to a PROCESSENTRY32 structure
     * @return Non-zero if successful, zero on failure
     */
    fun Process32FirstW(hSnapshot: MemorySegment, lppe: MemorySegment): Int

    /**
     * Retrieves information about the next process in a snapshot.
     *
     * @param hSnapshot Handle from CreateToolhelp32Snapshot
     * @param lppe Pointer to a PROCESSENTRY32 structure
     * @return Non-zero if successful, zero on failure
     */
    fun Process32NextW(hSnapshot: MemorySegment, lppe: MemorySegment): Int
}