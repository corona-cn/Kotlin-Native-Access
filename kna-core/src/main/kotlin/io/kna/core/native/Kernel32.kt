@file:Suppress("Unused", "FunctionName")
package io.kna.core.native
import io.kna.core.binder.*

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
     *
     * @return Handle to the specified device, or INVALID_HANDLE_VALUE (-1) on failure
     */
    fun GetStdHandle(nStdHandle: Int): MemorySegment

    /**
     * Opens an existing local process object.
     *
     * @param dwDesiredAccess Access mask to the process object
     * @param bInheritHandle Whether returned handle can be inherited
     * @param dwProcessId Identifier of the local process to open
     *
     * @return Open handle to the process, or NULL on failure
     */
    fun OpenProcess(dwDesiredAccess: Int, bInheritHandle: Int, dwProcessId: Long): MemorySegment

    /**
     * Closes an open object handle.
     *
     * @param hObject Valid handle to any open object
     *
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
     *
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
     *
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
     *
     * @return Handle to the snapshot on success, INVALID_HANDLE_VALUE on failure
     */
    fun CreateToolhelp32Snapshot(dwFlags: Long, th32ProcessID: Long): MemorySegment

    /**
     * Retrieves information about the first process in a snapshot.
     *
     * @param hSnapshot Handle from CreateToolhelp32Snapshot
     * @param lppe Pointer to a PROCESSENTRY32 structure
     *
     * @return Non-zero if successful, zero on failure
     */
    fun Process32FirstW(hSnapshot: MemorySegment, lppe: MemorySegment): Int

    /**
     * Retrieves information about the next process in a snapshot.
     *
     * @param hSnapshot Handle from CreateToolhelp32Snapshot
     * @param lppe Pointer to a PROCESSENTRY32 structure
     *
     * @return Non-zero if successful, zero on failure
     */
    fun Process32NextW(hSnapshot: MemorySegment, lppe: MemorySegment): Int


    /* === SYSTEM INFORMATION === */
    /**
     * Retrieves information about the current system.
     *
     * @param lpSystemInfo Pointer to a SYSTEM_INFO structure that receives the information
     *
     * @return This function does not return a value
     */
    fun GetSystemInfo(lpSystemInfo: MemorySegment)

    /**
     * Retrieves information about the system's current usage of both physical and virtual memory.
     *
     * @param lpBuffer Pointer to a MEMORYSTATUSEX structure that receives the memory information
     *
     * @return Non-zero if successful, zero on failure
     */
    fun GlobalMemoryStatusEx(lpBuffer: MemorySegment): Int


    /* === MEMORY ALLOCATION === */
    /**
     * Reserves, commits, or changes the state of a region of pages in the virtual address space of the calling process.
     *
     * @param lpAddress Pointer to the desired starting address, or NULL to let system determine
     * @param dwSize Size of the region to allocate or reserve, in bytes
     * @param flAllocationType Type of memory allocation (MEM_COMMIT, MEM_RESERVE, etc.)
     * @param flProtect Memory protection for the region (PAGE_READWRITE, PAGE_EXECUTE_READ, etc.)
     *
     * @return Base address of the allocated region if successful, NULL on failure [citation:3]
     */
    fun VirtualAlloc(
        lpAddress: MemorySegment,
        dwSize: Long,
        flAllocationType: Int,
        flProtect: Int
    ): MemorySegment

    /**
     * Releases, decommits, or releases and decommits a region of pages within the virtual address space of the calling process.
     *
     * @param lpAddress Pointer to the base address of the region to be freed
     * @param dwSize Size of the region to free, in bytes (must be 0 for MEM_RELEASE)
     * @param dwFreeType Type of free operation (MEM_RELEASE or MEM_DECOMMIT)
     *
     * @return Non-zero if successful, zero on failure
     */
    fun VirtualFree(
        lpAddress: MemorySegment,
        dwSize: Long,
        dwFreeType: Int
    ): Int

    /**
     * Changes the protection on a region of committed pages in the virtual address space of the calling process.
     *
     * @param lpAddress Pointer to the base address of the region of pages
     * @param dwSize Size of the region whose protection attributes are to be changed, in bytes
     * @param flNewProtect Memory protection option (PAGE_READONLY, PAGE_READWRITE, PAGE_EXECUTE, etc.)
     * @param lpflOldProtect Pointer to a variable that receives the previous protection value [citation:4]
     *
     * @return Non-zero if successful, zero on failure [citation:4]
     */
    fun VirtualProtect(
        lpAddress: MemorySegment,
        dwSize: Long,
        flNewProtect: Int,
        lpflOldProtect: MemorySegment
    ): Int


    /* === FILE OPERATIONS === */
    /**
     * Creates or opens a file or I/O device.
     *
     * @param lpFileName Pointer to a null-terminated string specifying the file name
     * @param dwDesiredAccess Access to the object (GENERIC_READ, GENERIC_WRITE, etc.)
     * @param dwShareMode Sharing mode of the object (FILE_SHARE_READ, FILE_SHARE_WRITE, etc.)
     * @param lpSecurityAttributes Pointer to a SECURITY_ATTRIBUTES structure, or NULL
     * @param dwCreationDisposition Action to take on files that exist/do not exist (CREATE_ALWAYS, OPEN_EXISTING, etc.)
     * @param dwFlagsAndAttributes File attributes and flags (FILE_ATTRIBUTE_NORMAL, FILE_FLAG_OVERLAPPED, etc.)
     * @param hTemplateFile Handle to a template file with GENERIC_READ access, or NULL
     *
     * @return Handle to the opened file if successful, INVALID_HANDLE_VALUE (-1) on failure [citation:5]
     */
    fun CreateFileW(
        lpFileName: MemorySegment,
        dwDesiredAccess: Int,
        dwShareMode: Int,
        lpSecurityAttributes: MemorySegment,
        dwCreationDisposition: Int,
        dwFlagsAndAttributes: Int,
        hTemplateFile: MemorySegment
    ): MemorySegment

    /**
     * Reads data from the specified file or input/output (I/O) device.
     *
     * @param hFile Handle to the file or I/O device to read from
     * @param lpBuffer Pointer to a buffer that receives the data read
     * @param nNumberOfBytesToRead Maximum number of bytes to read
     * @param lpNumberOfBytesRead Pointer to variable that receives the number of bytes read
     * @param lpOverlapped Pointer to an OVERLAPPED structure, or NULL for synchronous reads
     *
     * @return Non-zero if successful, zero on failure
     */
    fun ReadFile(
        hFile: MemorySegment,
        lpBuffer: MemorySegment,
        nNumberOfBytesToRead: Int,
        lpNumberOfBytesRead: MemorySegment,
        lpOverlapped: MemorySegment
    ): Int

    /**
     * Writes data to the specified file or input/output (I/O) device.
     *
     * @param hFile Handle to the file or I/O device to write to
     * @param lpBuffer Pointer to the buffer containing the data to write
     * @param nNumberOfBytesToWrite Number of bytes to write
     * @param lpNumberOfBytesWritten Pointer to variable that receives the number of bytes written
     * @param lpOverlapped Pointer to an OVERLAPPED structure, or NULL for synchronous writes [citation:7]
     *
     * @return Non-zero if successful, zero on failure [citation:5][citation:7]
     */
    fun WriteFile(
        hFile: MemorySegment,
        lpBuffer: MemorySegment,
        nNumberOfBytesToWrite: Int,
        lpNumberOfBytesWritten: MemorySegment,
        lpOverlapped: MemorySegment
    ): Int


    /* === MODULE & ERROR HANDLING === */
    /**
     * Retrieves a module handle for the specified module.
     *
     * @param lpModuleName Pointer to a null-terminated string specifying the module name.
     *                     If NULL, returns a handle to the file used to create the calling process.
     *
     * @return Handle to the specified module if successful, NULL on failure
     */
    fun GetModuleHandleW(lpModuleName: MemorySegment): MemorySegment

    /**
     * Retrieves the address of an exported function or variable from the specified dynamic-link library (DLL).
     *
     * @param hModule Handle to the DLL module containing the function or variable
     * @param lpProcName Pointer to a null-terminated string specifying the function name,
     *                   or an ordinal value (with the high-order word set to zero)
     *
     * @return Address of the exported function if successful, NULL on failure
     */
    fun GetProcAddress(
        hModule: MemorySegment,
        lpProcName: MemorySegment
    ): MemorySegment

    /**
     * Formats a message string based on a message identifier.
     *
     * @param dwFlags Formatting options (FORMAT_MESSAGE_ALLOCATE_BUFFER, FORMAT_MESSAGE_FROM_SYSTEM, etc.)
     * @param lpSource Location of the message definition (depends on dwFlags)
     * @param dwMessageId Message identifier for the requested message
     * @param dwLanguageId Language identifier for the requested message
     * @param lpBuffer Pointer to a buffer that receives the null-terminated message string
     * @param nSize Maximum size of the buffer, or pointer to minimum size if FORMAT_MESSAGE_ALLOCATE_BUFFER is used
     * @param arguments Pointer to an array of values to insert in the formatted message
     *
     * @return Number of characters stored in the output buffer if successful, zero on failure
     */
    fun FormatMessageW(
        dwFlags: Int,
        lpSource: MemorySegment,
        dwMessageId: Int,
        dwLanguageId: Int,
        lpBuffer: MemorySegment,
        nSize: Int,
        arguments: MemorySegment
    ): Int
}