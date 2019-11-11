package com.timotheteus.multpersistance

fun main() {

    mainLoop@ while (true) {
        print("Insert starting number: ")
        val startStr = readLine()
        while (startStr.isNullOrBlank())
            continue@mainLoop
        val start = startStr!!.toBigInteger()
        start.multiplicativeList().forEach { println(it) }
    }
}