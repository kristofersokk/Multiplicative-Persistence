package com.timotheteus.multpersistance

import java.math.BigInteger
import java.math.BigInteger.*
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min
import kotlin.streams.toList

const val maxRange = 20
const val onesPerCollectMap = 1
const val ops = 700000
val candidateList = ArrayList<List<BigInteger>>()
val jsonHandler = JsonHandler()
val jsonQuery = JsonQuery(jsonHandler)

fun main() {
    //(12..maxRange).forEach { println(it.toString() + ": " + multOfDigits(it.toBigInteger())) }

    val firstCands = findFirstCandidates()
    candidateList.add(firstCands)
    firstCands.printSolutions()

    for (i in 2..14) {
        val candidates = candidateList.last()
        val nextCandidates = candidates.getNextCandidates()
        val solutionCount = candidates.noOfPermutations()
        println()
        println("multiplicative persistence level $i:")
        println("solutions : LEN $solutionCount smallest: ${candidates.smallestInNextLayer()}")
        println()
        if (nextCandidates.isEmpty())
            break
        val nextSolutionCount = nextCandidates.noOfPermutations(true)
        if (nextSolutionCount == ZERO) {
            println("all candidates in next level are cached")
        } else {
            println("remaining permutations to iterate in next level: $nextSolutionCount")
            println(nextSolutionCount.computationDuration(ops))
        }
        candidateList.add(nextCandidates)
        println()
    }
}

fun Collection<BigInteger>.noOfPermutations(onlyUncomputed: Boolean = false) =
        this.map { it.noOfPermutations(onlyUncomputed = onlyUncomputed) }.sum()



fun Collection<BigInteger>.getAllSolutionsInNextLevel(): Collection<BigInteger> {
    return parallelStream().flatMap {
        it.findCollectMaps().parallelStream().flatMap {
            it.permuteToCandidates().parallelStream()
        }
    }.distinct().sorted().toList()
}

fun Collection<BigInteger>.getNextCandidates(doPrintProgress: Boolean = false): ArrayList<BigInteger> {
    totalC = this.size
    countC = 0
    countNextCandidatesSoFar = 0
    val result: ArrayList<BigInteger> = ArrayList()
    val candidateList = jsonHandler.jsonData.cache
    forEach {
        if (it !in candidateList.map { it.candidate }) {
            candidateList.add(Candidate(it, it.level))
        }
        val candidate = candidateList.find { elem -> elem.candidate == it } ?: Candidate(it, it.level)
        if (candidate.nextCandidates.isNotEmpty()) {
            result.addAll(candidate.nextCandidates)
        } else {
            countPermsPC = ZERO
            totalPermsPC = it.noOfPermutations(true)
            collectMaps = ArrayList(it.findCollectMaps(sort = true))
            countCM = 0
            val nextCandidates: ArrayList<BigInteger> = ArrayList()
            collectMaps.forEach {
                val cPermsExists = candidate.candidatePerms.any { elem -> elem.collectMap == it }
                if (!cPermsExists) {
                    val candidates = it.permuteToCandidates(true, doPrintProgress)
                    candidate.candidatePerms.add(CandidatePerms(it, true, candidates))
                    nextCandidates.addAll(candidates)
                    jsonHandler.writeToFile()
                }
                countNextCandidatesSoFar = nextCandidates.size
                countCM++
                if (doPrintProgress) printProgress()
            }
            candidate.nextCandidates = nextCandidates
            jsonHandler.writeToFile()
            result.addAll(nextCandidates)
        }
        countC++
    }
    println()
    return result
}

fun printProgress() {
    print("\rCandidates: %${totalC.toString().length}d:$totalC,  CollectMaps: %${collectMaps.size.toString().length}d:${collectMaps.size}, CMProgress: %4s |%-25s|, TProgress: %4s |%-25s|, foundNextCandidates: $countNextCandidatesSoFar"
            .format(countC,
                    countCM,
                    NumberFormat.getPercentInstance(Locale.US).format(CMProgress / 100),
                    "▓".repeat((CMProgress / 4).toInt()),
                    NumberFormat.getPercentInstance(Locale.US).format(CProgress / 100),
                    "▓".repeat((CProgress / 4).toInt())
            )
    )
}

fun <T : Comparable<T>> List<T>.printSolutions() {
    val list = this.sorted()
    if (this.size > 40)
        println(list.subList(0, 10).toString() + "........" + list.subList(this.size - 10, this.size).toString())
    else
        println(this)
}

fun HashSet<BigInteger>.printAllFactorLists() {
    this.toList().sorted().forEach { println("   $it: ${it.findPrimeFactors()}") }
}

fun List<BigInteger>.smallestInNextLayer(): BigInteger {
    return parallelStream().map {
        val primeFactors = it.findPrimeFactors()
        if (primeFactors.onlyPrimesUnderTen) {
            val primes = primeFactors.primeCounts
            val result = StringBuilder()
            val twos = primes.getOrDefault(2, 0)
            val threes = primes.getOrDefault(3, 0)
            val fives = primes.getOrDefault(5, 0)
            val sevens = primes.getOrDefault(7, 0)
            if (twos % 3 == 1 && threes % 2 == 0)
                result.append("2")
            if (twos % 3 == 2 && threes % 2 == 1)
                result.append("2")
            if (twos % 3 == 0 && threes % 2 == 1)
                result.append("3")
            if (twos % 3 == 2 && threes % 2 == 0)
                result.append("4")
            result.append("5".repeat(fives))
            if (twos % 3 > 0 && threes % 2 == 1)
                result.append("6")
            result.append("7".repeat(sevens))
            result.append("8".repeat(twos / 3))
            result.append("9".repeat(threes / 2))
            return@map result.toString().toBigInteger()
        }
        return@map Long.MAX_VALUE.toBigInteger()
    }.min(BigInteger::compareTo)
            .orElse(ZERO)
}

var resultsOfCM: ArrayList<BigInteger> = ArrayList()
var countPermsPCM: BigInteger = ZERO
var totalPermsPCM: BigInteger = ZERO
var lastTimeStamp = System.currentTimeMillis()

fun HashMap<Int, Int>.permuteToCandidates(onlyCorrectNumbers: Boolean = false, doPrintProgress: Boolean = false): ArrayList<BigInteger> {
    resultsOfCM = ArrayList()
    countPermsPCM = ZERO
    totalPermsPCM = this.noOfPermutations()
    lastTimeStamp = System.currentTimeMillis()
    permute(this, "", onlyCorrectNumbers, doPrintProgress)
    return resultsOfCM
}

fun permute(map: HashMap<Int, Int>, str: String = "", onlyCorrectNumbers: Boolean, doPrintProgress: Boolean) {
    if (map.values.sum() <= 0) {
        countPermsPCM++
        countPermsPC++
        if (countPermsPCM % 25000.toBigInteger() == ZERO) {
//            println("    ${count / 1000000L} mil, results: ${results.size}")
//            val curTime = System.currentTimeMillis()
//            println("    10000000 operations took ${(curTime - lastTimeStamp) / 1000f} seconds")
//            lastTimeStamp = curTime
            CMProgress = countPermsPCM percentage totalPermsPCM
            CProgress = countPermsPC percentage totalPermsPC
            if (doPrintProgress) printProgress()
        }
        if (str.isNotEmpty() && str.length >= 2) {
            val value = str.toBigInteger()
            if (!onlyCorrectNumbers || value.findPrimeFactors().onlyPrimesUnderTen)
                resultsOfCM.add(value)
        }
        return
    }
    (1..9).forEach {
        modifyMapPermutate(map, str, it, onlyCorrectNumbers)
    }
}

infix fun BigInteger.percentage(b: BigInteger): Double {
    return min((this * 1000.toBigInteger() / b).toInt(), 1000) / 10.0
}

fun modifyMapPermutate(map: HashMap<Int, Int>, str: String, key: Int, onlyCorrectNumbers: Boolean) {
    if (map.getOrDefault(key, 0) > 0) {
        map.modify(key, -1)
        permute(map, str + key, onlyCorrectNumbers, true)
        map.modify(key, 1)
    }
}

fun <T> HashMap<T, Int>.modify(key: T, change: Int) {
    this.merge(key, change, Int::plus)
}

fun multOfDigits(n: BigInteger): BigInteger {
    return n.toString().toCharArray().map { (it.toInt() - 48).toBigInteger() }.mult()
}

fun findFirstCandidates(range: Int = maxRange): List<BigInteger> {
    return (12..range).map(Int::toBigInteger).filter { multOfDigits(it) < TEN }.filter { it.findPrimeFactors().onlyPrimesUnderTen }.toList()
}

fun BigInteger.findCollectMaps(sort: Boolean = false) : ArrayList<HashMap<Int, Int>> {
    return findPrimeFactors().primeCounts.findCollectMaps(sort) as ArrayList<HashMap<Int, Int>>
}

fun BigInteger.noOfPermutations(onlyUncomputed: Boolean = false): BigInteger {
    val collectMaps = findCollectMaps()
    if (onlyUncomputed) {
        if (jsonQuery.hasCandidate(this)) {
            val computedCollectMaps = jsonQuery.candidate(this).candidatePerms.map { it.collectMap }
            return collectMaps
                    .filter { it !in computedCollectMaps }
                    .map { it.noOfPermutations() }
                    .sum()
        }
    }
    return collectMaps
            .map { it.noOfPermutations() }
            .sum()
}

fun HashMap<Int, Int>.noOfPermutations(): BigInteger {
    return this.values.sum().toBigInteger().factorial() /
            this.values.filter { it != 0 }.map { it.toBigInteger().factorial() }.mult()
}

val primesUnderTen = intArrayOf(2, 3, 5, 7)

fun BigInteger.findPrimeFactors(): PrimeFactors {
    var mutN = this
    val primeFactors = HashMap<Int, Int>()
    primesUnderTen.forEach {
        primeFactors[it] = 0
        while (mutN % it.toBigInteger() == ZERO) {
            mutN /= it.toBigInteger()
            primeFactors.merge(it, 1, Int::plus)
        }
    }
    return PrimeFactors(primeFactors, mutN == ONE)
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

val BigInteger.level : Int
    get() = multiplicativeList().size - 1

fun Collection<BigInteger>.sum(): BigInteger = this.fold(ZERO) { a, b -> a + b }

fun Collection<BigInteger>.mult(): BigInteger = this.fold(ONE) { a, b -> a * b }

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
        listOfCollectMaps += collectMap.deepClone()
        listOfCollectMaps.addAll((1..onesPerCollectMap).map {
            val newCM = collectMap.deepClone()
            newCM[1] = it
            newCM
        })
    }
}

fun HashMap<Int, Int>.deepClone() : HashMap<Int, Int> {
    val result = HashMap<Int, Int>()
    toMap(result)
    return result
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

class PrimeFactors(val primeCounts: HashMap<Int, Int>, val onlyPrimesUnderTen: Boolean)
