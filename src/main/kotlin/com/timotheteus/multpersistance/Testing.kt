package com.timotheteus.multpersistance

import java.math.BigInteger
import java.math.BigInteger.ONE

fun main() {
    //28224.printPermutations()

    //384.printPermutations()
    //768.printPermutations()
    //4374.printPermutations()
    //8232.printPermutations()
    //4644864.printPrimeFactors()
    //4644864.printPermutations(true)
    //println()
    //4478976
    //val permutations = 438939648.toBigInteger().noOfPermutations()
//    println("permutations: $permutations")
//    println(permutations.computationDuration(1000000))
//    438939648.printPrimeFactors()
//    println(438939648.toBigInteger().multiplicativeList())
//    println(getNextCandidates(listOf(68889.toBigInteger())))

//    val dataHandler = JsonHandler()
//    println(dataHandler.json)

    //438939648.printPermutations(true)

//    println(12.toBigInteger().findPrimeFactors().primeCounts.findCollectMaps())

//    val file = File("test.json")
//    val klaxon = Klaxon().fieldConverter(TypeBigInteger::class, bigIntegerConverter)
//    file.createNewFile()
//    file.writeText(klaxon.toJsonString(Test(240.toBigInteger())))
//    println(klaxon.parse<Test>(file) ?: Test())

    println(2352.toBigInteger().noOfPermutations())

//    (1..10).forEach {
//        print("\rawesome $it")
//        Thread.sleep(300)
//    }
//    println(2352.toBigInteger().findPrimeFactors().primeCounts)
//    println(listOf(2352.toBigInteger()).getAllSolutionsInNextLevel())
//    println(query.hasCandidate(438939648.toBigInteger()))
//    println(query.candidate(438939648.toBigInteger()).nextCandidates)

    val result = HashMap<Int, Int>()
    result[0] = 2
    result[1] = 3
    val result2 = result.deepClone()
    result2[1] = 4
    println(result)
    println(result2)
}

val query = JsonQuery(JsonHandler())

const val secondsInMinute = 60L
const val secondsInHour = 60 * secondsInMinute
const val secondsInDay = 24 * secondsInHour
const val secondsInYear = 365 * secondsInDay

fun BigInteger.computationDuration(ops: Int): String {
    var seconds = this.divide(ops.toBigInteger()).toLong()
    val years = seconds / secondsInYear
    seconds %= secondsInYear
    val days = seconds / secondsInDay
    seconds %= secondsInDay
    val hours = seconds / secondsInHour
    seconds %= secondsInHour
    val minutes = seconds / secondsInMinute
    seconds %= secondsInMinute
    return "computations will be completed in $years years, $days days, $hours hours, $minutes minutes and $seconds seconds"
}

operator fun BigInteger.rangeTo(other: BigInteger) = BigIntegerRange(this, other)

class BigIntegerRange(override val start: BigInteger, override val endInclusive: BigInteger) : ClosedRange<BigInteger>, Iterable<BigInteger> {
    override operator fun iterator(): Iterator<BigInteger> = BigIntegerRangeIterator(this)

    fun mult(): BigInteger = this.fold(ONE) { a, b -> a * b }
}

class BigIntegerRangeIterator(private val range: ClosedRange<BigInteger>) : Iterator<BigInteger> {

    private var current = range.start

    override fun hasNext(): Boolean = current <= range.endInclusive

    override fun next(): BigInteger {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return current++
    }
}

