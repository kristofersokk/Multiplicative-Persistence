package com.timotheteus.multpersistance

import java.math.BigInteger
import java.util.function.Predicate

class JsonQuery(private val jsonHandler: JsonHandler) {

    val cache = jsonHandler.jsonData.cache

    var totalC = 0
    var totalPermsPC: BigInteger = BigInteger.ZERO
    var collectMaps = ArrayList<HashMap<Int, Int>>()
    var countC = 0
    var countPermsPC: BigInteger = BigInteger.ZERO
    var countCM = 0
    var countNextCandidatesSoFar = 0
    var CMProgress = 0.0
    var CProgress = 0.0

    fun calculateLevel(level: Int) {
        getCandidatesWithLevel(level - 1).forEach { candidate ->

        }
    }

    fun addCandidate(bigInteger: BigInteger) {
        if (!hasCandidate(bigInteger)) {
            cache.addAll(Candidate)
        }
    }

    fun getCandidatesWithLevel(level: Int) : Collection<Candidate> {
        return cache.filter { it.level == level }
    }

    fun hasCandidate(cand: BigInteger) : Boolean {
        return cache.any { it.candidate == cand }
    }

    fun candidate(cand: BigInteger) : Candidate {
        return cache.first { it.candidate == cand }
    }

    fun invalidateCandidate(predicate: Predicate<BigInteger>) {
        cache.forEach { candidate ->
            if (predicate.test(candidate.candidate)) {
                candidate.candidatePerms.forEach { CM ->
                    CM.calculated = false
                }
            }
        }
        jsonHandler.writeToFile()
    }

    fun clearNextCandidates(predicate: Predicate<BigInteger>) {
        cache.forEach {
            if (predicate.test(it.candidate)) {
                it.nextCandidates = ArrayList()
            }
        }
        jsonHandler.writeToFile()
    }

    fun completeCandidate(predicate: Predicate<BigInteger>) {
        cache.forEach { candidate ->
            if (predicate.test(candidate.candidate)) {
                val nextCandidates = ArrayList<BigInteger>()
                candidate.candidatePerms.forEach {
                    if (!it.calculated) {
                        it.candidates = it.collectMap.permuteToCandidates(onlyCorrectNumbers = true, doPrintProgress = false)
                        it.calculated = true
                    }
                    nextCandidates.addAll(it.candidates)
                }
                candidate.nextCandidates = nextCandidates
            }
        }
        jsonHandler.writeToFile()
    }

    fun addMissingCollectMaps(predicate: Predicate<BigInteger>) : Boolean {
        var addedNewCollectMaps = false
        cache.forEach { candidate ->
            if (predicate.test(candidate.candidate)) {
                candidate.candidate.findCollectMaps().forEach { CM ->
                    if (CM !in candidate.candidatePerms.map { it.collectMap }) {
                        candidate.candidatePerms.add(CandidatePerms(CM, false, ArrayList()))
                        addedNewCollectMaps = true
                    }
                }
            }
        }
        jsonHandler.writeToFile()
        return addedNewCollectMaps
    }

}