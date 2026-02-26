package io.kna

fun log(any: Any?) {
    java.io.PrintStream(System.out, true, "UTF-8").println(any.toString())
}