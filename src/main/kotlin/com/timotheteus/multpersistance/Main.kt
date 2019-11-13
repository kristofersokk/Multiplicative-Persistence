package com.timotheteus.multpersistance

import java.math.BigInteger
import java.math.BigInteger.TEN
import java.math.BigInteger.ZERO
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val maxRange = 20
const val ops = 700000
val candidateList = ArrayList<List<BigInteger>>()
val jsonHandler = JsonHandler()

fun main() {
    //(12..maxRange).forEach { println(it.toString() + ": " + multOfDigits(it.toBigInteger())) }

    val firstCands = findFirstCandidates()
    candidateList.add(firstCands)
    firstCands.printSolutions()

    for (i in 2..14) {
        val candidates = candidateList.last()
        val nextCandidates = candidates.getNextCandidates()
        if (nextCandidates.isEmpty()) {

            return
        }
        val solutionCount = candidates.noOfPermutations()
        val nextSolutionCount = nextCandidates.noOfPermutations(true)
        println()
        println("multiplicative persistence level $i:")
        println("solutions : LEN $solutionCount smallest: ${candidates.smallestInNextLayer()}")
        println()
        println("remaining permutations to iterate in next level: $nextSolutionCount")
        println(nextSolutionCount.computationDuration(ops))
        candidateList.add(nextCandidates)
        println()
    }
}

fun Collection<BigInteger>.noOfPermutations(onlyUncomputed: Boolean = false) = this.map { it.noOfPermutations(onlyUncomputed = onlyUncomputed) }.sum()

var totalC = 0
var totalPermsPC: BigInteger = ZERO
var collectMaps = ArrayList<HashMap<Int, Int>>()
var countC = 0
var countPermsPC: BigInteger = ZERO
var countCM = 0
var CMProgress = 0.0
var CProgress = 0.0


fun Collection<BigInteger>.getNextCandidates(): ArrayList<BigInteger> {
    totalC = this.size
    countC = 0
    val result: ArrayList<BigInteger> = ArrayList()
    val candidateList = jsonHandler.jsonData.cache
    forEach {
        if (it !in candidateList.map { it.candidate }) {
            candidateList.add(Candidate(it))
        }
        val candidate = candidateList.find { elem -> elem.candidate == it } ?: Candidate(it)
        if (candidate.nextCandidates.isNotEmpty()) {
            result.addAll(candidate.nextCandidates)
        } else {
            countPermsPC = ZERO
            totalPermsPC = it.noOfPermutations(true)
            collectMaps = ArrayList(it.findPrimeFactors().primeCounts.findCollectMaps(sort = true))
            countCM = 0
            val nextCandidates: ArrayList<BigInteger> = ArrayList()
            collectMaps.forEach {
                val cPermsExists = candidate.candidatePerms.any { elem -> elem.collectMap == it }
                if (!cPermsExists) {
                    val candidates = it.permuteToCandidates(true)
                    candidate.candidatePerms.add(CandidatePerms(it, candidates))
                    nextCandidates.addAll(candidates)
                    jsonHandler.writeToFile()
                }
                countCM++
                printProgress()
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
    print("\rCandidates: %${totalC.toString().length}d:$totalC CollectMaps: %${collectMaps.size.toString().length}d:${collectMaps.size} CollectMapProgress: %4s |%-25s|, TotalProgress: %4s |%-25s|"
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

fun HashMap<Int, Int>.permuteToCandidates(onlyCorrectNumbers: Boolean = false): ArrayList<BigInteger> {
    resultsOfCM = ArrayList()
    countPermsPCM = ZERO
    totalPermsPCM = this.noOfPermutations()
    lastTimeStamp = System.currentTimeMillis()
    permute(this, "", onlyCorrectNumbers)
    return resultsOfCM
}

fun permute(map: HashMap<Int, Int>, str: String = "", onlyCorrectNumbers: Boolean) {
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
            printProgress()
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
    return kotlin.math.min((this * 1000.toBigInteger() / b).toInt(), 1000) / 10.0
}

fun modifyMapPermutate(map: HashMap<Int, Int>, str: String, key: Int, onlyCorrectNumbers: Boolean) {
    if (map.getOrDefault(key, 0) >= 1) {
        map.modify(key, -1)
        permute(map, str + key, onlyCorrectNumbers)
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

fun BigInteger.noOfPermutations(onlyUncomputed: Boolean = false): BigInteger {
    val collectMaps = findPrimeFactors().primeCounts.findCollectMaps()
    if (onlyUncomputed) {
        val candidateIsCached = jsonHandler.jsonData.cache.any { it.candidate == this }
        if (candidateIsCached) {
            val computedCollectMaps = jsonHandler.jsonData.cache.first { it.candidate == this }.candidatePerms.map { it.collectMap }
            return collectMaps
                    .filter { !computedCollectMaps.contains(it) }
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
    var canBeExpanded = true
    primesUnderTen.forEach {
        primeFactors[it] = 0
        while (mutN.mod(it.toBigInteger()) == BigInteger.ZERO) {
            mutN = mutN.divide(it.toBigInteger())
            primeFactors.merge(it, 1, Int::plus)
        }
    }
    if (mutN > BigInteger.ONE) {
        primeFactors[0] = 1
        canBeExpanded = false
    }
    return PrimeFactors(primeFactors, canBeExpanded)
}

class PrimeFactors(val primeCounts: HashMap<Int, Int>, val onlyPrimesUnderTen: Boolean)
