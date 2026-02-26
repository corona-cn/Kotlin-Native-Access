@file:Suppress("Unused")
package io.kna
import java.lang.foreign.*

/* === NATIVES === */
object Natives {
    /* === PUBLIC NATIVE COMPONENTS === */
    /* Linker */
    @JvmField
    val nativeLinker: Linker = Linker.nativeLinker()

    /* Arena */
    @JvmField
    val sharedArena: Arena = Arena.ofShared()

    /* Library */
    @JvmField
    val kernel32Lookup: SymbolLookup = SymbolLookup.libraryLookup("kernel32", sharedArena)

    @JvmField
    val user32Lookup: SymbolLookup = SymbolLookup.libraryLookup("user32", sharedArena)
}