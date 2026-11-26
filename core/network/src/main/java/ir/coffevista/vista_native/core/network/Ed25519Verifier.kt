package ir.coffevista.vista_native.core.network

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Arrays

/**
 * Pure RFC 8032 Ed25519 signature verifier using constant-structure modular arithmetic
 * over twisted Edwards curve edwards25519 (p = 2^255 - 19). Works consistently on
 * Android API 24+ and local JVM unit tests without provider/API-level differences.
 */
object Ed25519Verifier {
    private val P: BigInteger = BigInteger.valueOf(2).pow(255).subtract(BigInteger.valueOf(19))
    private val L: BigInteger = BigInteger.valueOf(2).pow(252)
        .add(BigInteger("27742317777372353535851937790883648493"))
    private val D: BigInteger = BigInteger("-121665")
        .multiply(BigInteger("121666").modInverse(P))
        .mod(P)
    private val TWO: BigInteger = BigInteger.valueOf(2)
    private val SQRT_M1: BigInteger = TWO.modPow(P.subtract(BigInteger.ONE).shiftRight(2), P)
    private val B_X: BigInteger = BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202")
    private val B_Y: BigInteger = BigInteger.valueOf(4).multiply(BigInteger.valueOf(5).modInverse(P)).mod(P)

    private data class Point(
        val x: BigInteger,
        val y: BigInteger,
        val z: BigInteger,
        val t: BigInteger,
    ) {
        companion object {
            val IDENTITY = Point(BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO)
        }
    }

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size != 32 || signature.size != 64) return false
        return runCatching {
            val aPoint = decodePoint(publicKey) ?: return false
            val rBytes = signature.copyOfRange(0, 32)
            val rPoint = decodePoint(rBytes) ?: return false
            val s = decodeLittleEndian(signature.copyOfRange(32, 64))
            if (s < BigInteger.ZERO || s >= L) return false

            val digest = MessageDigest.getInstance("SHA-512").apply {
                update(rBytes)
                update(publicKey)
                update(message)
            }.digest()
            val h = decodeLittleEndian(digest).mod(L)

            val lhs = scalarMult(Point(B_X, B_Y, BigInteger.ONE, B_X.multiply(B_Y).mod(P)), s)
            val rhs = pointAdd(rPoint, scalarMult(aPoint, h))
            Arrays.equals(encodePoint(lhs), encodePoint(rhs))
        }.getOrDefault(false)
    }

    private fun decodePoint(enc: ByteArray): Point? {
        if (enc.size != 32) return null
        val xSign = (enc[31].toInt() ushr 7) and 1
        val yBytes = enc.copyOf()
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()
        val y = decodeLittleEndian(yBytes)
        if (y >= P) return null

        val y2 = y.multiply(y).mod(P)
        val u = y2.subtract(BigInteger.ONE).mod(P)
        val v = D.multiply(y2).add(BigInteger.ONE).mod(P)
        if (v == BigInteger.ZERO) return null

        // x = (u * v^3) * (u * v^7)^((p - 5) / 8) mod p
        val v3 = v.multiply(v).mod(P).multiply(v).mod(P)
        val v7 = v3.multiply(v3).mod(P).multiply(v).mod(P)
        val exp = P.subtract(BigInteger.valueOf(5)).shiftRight(3)
        var x = u.multiply(v3).mod(P)
            .multiply(u.multiply(v7).mod(P).modPow(exp, P))
            .mod(P)

        val check = v.multiply(x).mod(P).multiply(x).mod(P)
        if (check != u) {
            if (check == u.negate().mod(P)) {
                x = x.multiply(SQRT_M1).mod(P)
            } else {
                return null
            }
        }
        if (x == BigInteger.ZERO && xSign == 1) return null
        if (x.and(BigInteger.ONE).toInt() != xSign) {
            x = P.subtract(x).mod(P)
        }
        return Point(x, y, BigInteger.ONE, x.multiply(y).mod(P))
    }

    private fun encodePoint(pt: Point): ByteArray {
        val zInv = pt.z.modInverse(P)
        val x = pt.x.multiply(zInv).mod(P)
        val y = pt.y.multiply(zInv).mod(P)
        val out = encodeLittleEndian(y)
        if (x.testBit(0)) {
            out[31] = (out[31].toInt() or 0x80).toByte()
        }
        return out
    }

    private fun pointAdd(p: Point, q: Point): Point {
        val a = p.y.subtract(p.x).mod(P).multiply(q.y.subtract(q.x).mod(P)).mod(P)
        val b = p.y.add(p.x).mod(P).multiply(q.y.add(q.x).mod(P)).mod(P)
        val c = p.t.multiply(TWO).mod(P).multiply(D).mod(P).multiply(q.t).mod(P)
        val d = p.z.multiply(TWO).mod(P).multiply(q.z).mod(P)
        val e = b.subtract(a).mod(P)
        val f = d.subtract(c).mod(P)
        val g = d.add(c).mod(P)
        val h = b.add(a).mod(P)
        return Point(
            x = e.multiply(f).mod(P),
            y = g.multiply(h).mod(P),
            z = f.multiply(g).mod(P),
            t = e.multiply(h).mod(P),
        )
    }

    private fun scalarMult(base: Point, scalar: BigInteger): Point {
        var acc = Point.IDENTITY
        var cur = base
        for (bit in 0 until scalar.bitLength()) {
            if (scalar.testBit(bit)) {
                acc = pointAdd(acc, cur)
            }
            cur = pointAdd(cur, cur)
        }
        return acc
    }

    private fun decodeLittleEndian(bytes: ByteArray): BigInteger {
        val rev = ByteArray(bytes.size + 1)
        for (i in bytes.indices) {
            rev[bytes.size - i] = bytes[i]
        }
        return BigInteger(rev)
    }

    private fun encodeLittleEndian(v: BigInteger): ByteArray {
        val raw = v.toByteArray()
        val out = ByteArray(32)
        var rawIdx = raw.size - 1
        var outIdx = 0
        while (rawIdx >= 0 && outIdx < 32) {
            out[outIdx++] = raw[rawIdx--]
        }
        return out
    }
}
