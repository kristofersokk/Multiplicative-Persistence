package com.timotheteus.multpersistance

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigInteger
import java.math.BigInteger.TEN
import java.math.BigInteger.ZERO
import kotlin.concurrent.thread
import kotlin.system.exitProcess

const val maxRange = 100
const val ops = 700000
val sqlPort = 3306
var interrupt = false
val candidateList = ArrayList<List<BigInteger>>()

fun main() {
    //(12..maxRange).forEach { println(it.toString() + ": " + multOfDigits(it.toBigInteger())) }

    thread(start = true) {
        run()
    }

    Database.connect("jdbc:mysql://localhost:3306/",
            driver = "com.mysql.jdbc.Driver",
            user = "kristofersokk",
            password = "MySQLHoldsSecrets")

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Candidates, NextCandidates, CollectMaps, MapCandidates)
    }

    val firstCands = findFirstCandidates()
    candidateList.add(firstCands)
    firstCands.printSolutions()

    for (i in 2..14) {
        val candidates = candidateList.last()
        val nextCandidates = candidates.getNextCandidates()
        println(candidates)
        val solutionCount = candidates.map { it.noOfPermutations() }.sum()
        val nextSolutionCount = nextCandidates.map { it.noOfPermutations() }.sum()
        println()
        println("multiplicative persistence level $i:")
        println("solutions : LEN $solutionCount smallest: ${candidates.smallestInNextLayer()}")
        println()
        println("solutions in next level: $nextSolutionCount")
        println(nextSolutionCount.computationDuration(ops))
        candidateList.add(nextCandidates)
        println()
    }
}

fun Collection<BigInteger>.getNextCandidates() : ArrayList<BigInteger> {
    val result: ArrayList<BigInteger> = ArrayList()

    forEach {
        var nextCandidates: List<BigInteger> = ArrayList()
        var candidateId: EntityID<Int> = EntityID(0, Candidates)
        transaction {
            val candidateExists = Candidates.select {Candidates.value eq it.toString()}.firstOrNull()
            candidateId = if (candidateExists == null) {
                Candidates.insertAndGetId { table -> table[value] = it.toString() }
            } else {
                candidateExists[Candidates.id]
            }
            nextCandidates = NextCandidates.select { NextCandidates.candidate_id eq candidateId }
                    .map { it[NextCandidates.value].toBigInteger() }
        }

        if (nextCandidates is Query && nextCandidates.isNotEmpty()) {
            result.addAll(nextCandidates)
        } else {

            //Candidate is not in database, computing collectMaps and nextCandidates
            val collectMaps = it.findPrimeFactors().primeCounts.findCollectMaps(sort = true)
            val nextCandidates: HashSet<BigInteger> = HashSet()
            collectMaps.forEach {
                if (interrupt) {
                    exitProcess(-1)
                }
                val candidates = it.permuteToCandidates(true)
                transaction {
                    //add collectMap and candidates to db
                    val mapId = CollectMaps.insertAndGetId { row ->
                        row[map] = it.toString()
                        row[candidate_id] = candidateId
                    }
                    candidates.forEach { candidate ->
                        MapCandidates.insert { row ->
                            row[map_id] = mapId
                            row[value] = candidate.toString()
                        }
                    }
                }
                nextCandidates.addAll(candidates)
            }
            result.addAll(nextCandidates)
            transaction {
                //add all nextCandidates to db
                nextCandidates.forEach {
                    NextCandidates.insert { row ->
                        row[candidate_id] = candidateId
                        row[value] = it.toString()
                    }
                }
            }
        }
    }
    return result
}

fun run() {
    if (!readLine().isNullOrEmpty()) {
        interrupt = true
        return
    }
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

var results: ArrayList<BigInteger> = ArrayList()
var count: Long = 0
var lastTimeStamp = System.currentTimeMillis()

fun HashMap<Int, Int>.permuteToCandidates(onlyCorrectNumbers: Boolean = false): ArrayList<BigInteger> {
    results = ArrayList()
    count = 0
    permute(this, "", onlyCorrectNumbers)
    return results
}

fun permute(map: HashMap<Int, Int>, str: String = "", onlyCorrectNumbers: Boolean) {
    if (map.values.sum() <= 0) {
//        count++
//        if (count % 10000000 == 0L) {
//            println("    ${count / 1000000L} mil, results: ${results.size}")
//            val curTime = System.currentTimeMillis()
//            println("    10000000 operations took ${(curTime - lastTimeStamp) / 1000f} seconds")
//            lastTimeStamp = curTime
//        }
        if (str.isNotEmpty() && str.length >= 2) {
            val value = str.toBigInteger()
            if (!onlyCorrectNumbers || value.findPrimeFactors().onlyPrimesUnderTen)
                results.add(value)
        }
        return
    }
    (1..9).forEach {
        modifyMapPermutate(map, str, it, onlyCorrectNumbers)
    }
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

data class PrimeFactors(val primeCounts: HashMap<Int, Int>, val onlyPrimesUnderTen: Boolean)
