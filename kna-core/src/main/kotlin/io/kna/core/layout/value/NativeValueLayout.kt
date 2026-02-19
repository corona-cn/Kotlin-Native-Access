@file:Suppress("Unused")
package io.kna.core.layout.value
import io.kna.core.layout.*

import java.lang.foreign.*

interface NativeValueLayout<Type> : NativeLayout<Type> {
    override val layout: ValueLayout
}