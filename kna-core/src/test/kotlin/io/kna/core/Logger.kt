package io.kna.core

fun log(any: Any?) {
    val out = java.io.PrintStream(System.out, true, "UTF-8")
    out.println(any.toString())
}