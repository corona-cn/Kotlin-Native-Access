@file:Suppress("Unused")
package io.kna.core.layout.address
import io.kna.core.layout.*

import java.lang.foreign.*

interface NativeAddressLayout<Type> : NativeLayout<Type> {
    override val layout: AddressLayout
}