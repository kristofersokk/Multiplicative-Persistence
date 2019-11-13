package com.timotheteus.multpersistance

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import kotlin.math.min

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

    (1..10).forEach {
        print("\rawesome $it")
        Thread.sleep(300)
    }
}

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

fun BigInteger.multiplicativeList(): ArrayList<BigInteger> {
    val list = ArrayList<BigInteger>()
    var value = this
    while (value.toString().length >= 2) {
        list.add(value)
        value = value.toString().map {
            (it.toInt() - 48).toBigInteger()
        }.mult()
    }
    list.add(value)
    return list
}

fun Collection<BigInteger>.sum(): BigInteger = this.fold(ZERO) { a, b -> a + b }

fun Collection<BigInteger>.mult(): BigInteger = this.fold(ONE) { a, b -> a * b }

fun Int.printPrimeFactors() {
    println(this.toBigInteger().findPrimeFactors().primeCounts)
}

fun BigInteger.factorial(): BigInteger = (ONE..this).mult()

fun HashMap<Int, Int>.findCollectMaps(sort: Boolean = false): List<java.util.HashMap<Int, Int>> {
    val listOfCollectMaps: ArrayList<HashMap<Int, Int>> = ArrayList()
    collectMorePrimesToCollectMap(this, HashMap(), 9, listOfCollectMaps)
    return if (sort)
        listOfCollectMaps.sortedBy { it.noOfPermutations() }
    else listOfCollectMaps
}

fun collectMorePrimesToCollectMap(map: HashMap<Int, Int>, collectMap: HashMap<Int, Int>, numberToExtract: Int, listOfCollectMaps: ArrayList<HashMap<Int, Int>>) {
    if (numberToExtract > 1) {
        when (numberToExtract) {
            9 -> _changeMapNewPermutations(map, collectMap, 3, 2, 9, listOfCollectMaps)
            8 -> _changeMapNewPermutations(map, collectMap, 2, 3, 8, listOfCollectMaps)
            7 -> _changeMapNewPermutations(map, collectMap, 7, 1, 7, listOfCollectMaps)
            6 -> {
                val min6 = min(map.getOrDefault(2, 0), map.getOrDefault(3, 0))
                for (i in 0..min6) {
                    map.modify(2, -i)
                    map.modify(3, -i)
                    collectMap[6] = i
                    collectMorePrimesToCollectMap(map, collectMap, numberToExtract - 1, listOfCollectMaps)
                    map.modify(2, i)
                    map.modify(3, i)
                    collectMap.remove(6)
                }
            }
            5 -> _changeMapNewPermutations(map, collectMap, 5, 1, 5, listOfCollectMaps)
            4 -> _changeMapNewPermutations(map, collectMap, 2, 2, 4, listOfCollectMaps)
            3 -> _changeMapNewPermutations(map, collectMap, 3, 1, 3, listOfCollectMaps)
            2 -> _changeMapNewPermutations(map, collectMap, 2, 1, 2, listOfCollectMaps)
        }
    } else {
        listOfCollectMaps += collectMap.clone() as java.util.HashMap<Int, Int>
    }
}

private fun _changeMapNewPermutations(map: HashMap<Int, Int>, collectMap: HashMap<Int, Int>, base: Int, power: Int, numberToExtract: Int, listOfCollectMaps: ArrayList<HashMap<Int, Int>>) {
    if (numberToExtract in primesUnderTen) {
        collectMap[base] = map.getOrDefault(base, 0)
        map[base] = 0
        collectMorePrimesToCollectMap(map, collectMap, numberToExtract - 1, listOfCollectMaps)
        map[base] = collectMap.getOrDefault(base, 0)
        collectMap.remove(base)
    } else {
        for (i in 0..(map.getOrDefault(base, 0) / power)) {
            map.modify(base, -power * i)
            collectMap[numberToExtract] = i
            collectMorePrimesToCollectMap(map, collectMap, numberToExtract - 1, listOfCollectMaps)
            map.modify(base, power * i)
            collectMap.remove(numberToExtract)
        }
    }
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

