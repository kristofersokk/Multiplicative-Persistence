@file:UseSerializers(BigIntegerSerializer::class)

package com.timotheteus.multpersistance

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.math.BigInteger

/**
 * {
 *     "cache" : [
 *         {"number": "68889",
 *          "collectMaps" : [
 *             {"collectMap" : {2: 3, 3: 2, 5: 2, 7: 0},
 *              "candidates" : ["", "", ]
 *             }
 *          ],
 *          "nextCandidates" : ["..", "..", ".."]
 *         }
 *     ]
 * }
 */

@Serializable
data class JsonData(
        val cache: ArrayList<Candidate> = ArrayList()
)

@Serializable
data class Candidate @JvmOverloads constructor(
        val candidate: BigInteger,
        val candidatePerms: ArrayList<CandidatePerms> = ArrayList(),
        var nextCandidates: ArrayList<BigInteger> = ArrayList()
)

@Serializable
data class CandidatePerms(
        val collectMap: HashMap<Int, Int>,
        var candidates: ArrayList<BigInteger> = ArrayList()
)

@Serializer(forClass = BigInteger::class)
object BigIntegerSerializer : KSerializer<BigInteger> {

    override val descriptor: SerialDescriptor =
            StringDescriptor.withName("WithCustomDefault")

    override fun serialize(encoder: Encoder, obj: BigInteger) {
        encoder.encodeString(obj.toString())
    }

    override fun deserialize(decoder: Decoder): BigInteger {
        return decoder.decodeString().toBigInteger()
    }
}

//val bigIntegerConverter = object: Converter {
//    override fun canConvert(cls: Class<*>)
//            = cls == BigInteger::class.java
//
//    override fun fromJson(jv: JsonValue) =
//            if (jv.string != null) {
//                BigInteger(jv.string)
//            } else {
//                println(jv.string)
//                throw KlaxonException("Couldn't parse bigInteger: ${jv.string}")
//            }
//
//    override fun toJson(value: Any) = "\"$value\""
//}
//
//@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
//annotation class TypeBigInteger

