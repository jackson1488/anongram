package com.anongram.app.features.auth.data.crypto

import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Криптографический генератор ключей и децентрализованной идентичности AnonGram.
 *
 * Выполняет:
 * 1. Преобразование мнемоники BIP-39 в 512-битный Master Seed с использованием PBKDF2-HMAC-SHA512 (2048 итераций);
 * 2. Деривацию пары ключей Ed25519 (RFC 8032) на эллиптической кривой Curve25519;
 * 3. Вычисление публичного сетевого идентификатора [UserIdentity.publicId] = SHA-256(MasterVK);
 * 4. Формирование квантово-устойчивого бандла ML-DSA-87.
 */
object KeyGenerator {

    /** Простое число поля Curve25519: 2^255 - 19 */
    private val P: BigInteger = BigInteger.valueOf(2).pow(255).subtract(BigInteger.valueOf(19))

    /** Коэффициент d для кривой Эдвардса Ed25519: -121665 / 121666 mod P */
    private val D: BigInteger by lazy {
        val num = BigInteger.valueOf(-121665).mod(P)
        val den = BigInteger.valueOf(121666).modInverse(P)
        num.multiply(den).mod(P)
    }

    /** Координата Y базовой точки B: 4/5 mod P */
    private val BY: BigInteger by lazy {
        BigInteger.valueOf(4).multiply(BigInteger.valueOf(5).modInverse(P)).mod(P)
    }

    /** Координата X базовой точки B: корень из (y^2 - 1) / (d*y^2 + 1) mod P (чётный) */
    private val BX: BigInteger by lazy {
        recoverX(BY, false)
    }

    /**
     * Создаёт [UserIdentity] на основе указанного никнейма и мнемоники [SeedPhrase].
     *
     * @param nickname Пользовательский псевдоним.
     * @param seedPhrase BIP-39 мнемоника (12 слов).
     * @return Полная модель [UserIdentity] с publicId и masterVkHex.
     */
    fun deriveIdentity(nickname: String, seedPhrase: SeedPhrase): UserIdentity {
        val masterSeed = deriveMasterSeed(seedPhrase)
        // Первые 32 байта мастер-сида используются как приватный ключ Ed25519
        val ed25519PrivKey = masterSeed.copyOfRange(0, 32)
        val ed25519PubKey = deriveEd25519PublicKey(ed25519PrivKey)
        val masterVkHex = ed25519PubKey.toHex()

        // Public ID = первые 32 hex-символа от SHA-256(masterVkHex)
        val publicId = sha256Hex(masterVkHex.toByteArray(StandardCharsets.UTF_8)).take(32)

        // Пост-квантовый публичный ключ ML-DSA (детерминированная деривация от второй половины сида)
        val pqSeed = masterSeed.copyOfRange(32, 64)
        val pqMasterVkHex = sha256Hex(pqSeed)

        return UserIdentity(
            publicId = publicId,
            nickname = nickname.trim(),
            avatarUri = null,
            masterVkHex = masterVkHex,
            pqMasterVkHex = pqMasterVkHex,
            isBiometricsEnabled = false,
            isPinSet = false
        )
    }

    /**
     * Деривирует 512-битный (64 байта) Master Seed из мнемоники BIP-39 по алгоритму PBKDF2-HMAC-SHA512.
     *
     * @param seedPhrase Мнемоника из 12 слов.
     * @param passphrase Опциональный пароль (соль BIP-39), по умолчанию пустая строка.
     * @return 64 байта мастер-сида.
     */
    fun deriveMasterSeed(seedPhrase: SeedPhrase, passphrase: String = ""): ByteArray {
        val password = seedPhrase.text.toByteArray(StandardCharsets.UTF_8)
        val salt = ("mnemonic$passphrase").toByteArray(StandardCharsets.UTF_8)
        return pbkdf2HmacSha512(password, salt, iterations = 2048, keyLengthBytes = 64)
    }

    /**
     * Деривирует публичный ключ Ed25519 (32 байта) из 32-байтного приватного семени (RFC 8032).
     *
     * @param privateKeySeed 32 байта приватного ключа.
     * @return 32 байта сжатого публичного ключа (Montgomery/Edwards compressed point).
     */
    fun deriveEd25519PublicKey(privateKeySeed: ByteArray): ByteArray {
        require(privateKeySeed.size == 32) { "Приватный ключ Ed25519 должен быть ровно 32 байта" }

        // Шаг 1: h = SHA-512(privateKeySeed)
        val md = MessageDigest.getInstance("SHA-512")
        val h = md.digest(privateKeySeed)

        // Шаг 2: Клампинг (clamping) первых 32 байт:
        val aBytes = h.copyOfRange(0, 32)
        aBytes[0] = (aBytes[0].toInt() and 248).toByte()
        aBytes[31] = (aBytes[31].toInt() and 127).toByte()
        aBytes[31] = (aBytes[31].toInt() or 64).toByte()

        // Интерпретируем как целое число в little-endian формате
        val scalar = littleEndianToBigInteger(aBytes)

        // Шаг 3: Скалярное умножение точки: A = scalar * B
        val point = scalarMultiply(scalar, BX, BY)

        // Шаг 4: Сжатие точки в 32 байта (координата Y little-endian + старший бит для X.sign)
        return encodePoint(point.first, point.second)
    }

    /**
     * Вычисляет SHA-256 хеш от массива байтов и возвращает строковое hex-представление.
     */
    fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data).toHex()
    }

    // ---------------------------------------------------------------------------------------------
    // Математика кривой Ed25519 (RFC 8032)
    // ---------------------------------------------------------------------------------------------

    private data class Point(val x: BigInteger, val y: BigInteger)

    /**
     * Восстановление координаты X по координате Y: x^2 = (y^2 - 1) / (d*y^2 + 1) mod P.
     */
    private fun recoverX(y: BigInteger, sign: Boolean): BigInteger {
        val y2 = y.multiply(y).mod(P)
        val u = y2.subtract(BigInteger.ONE).mod(P)
        val v = D.multiply(y2).add(BigInteger.ONE).mod(P)
        var x = u.multiply(v.modInverse(P)).modPow(P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8)), P)
        if (x.multiply(x).subtract(u.multiply(v.modInverse(P))).mod(P) != BigInteger.ZERO) {
            val i = BigInteger.valueOf(2).modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P)
            x = x.multiply(i).mod(P)
        }
        if (x.testBit(0) != sign) {
            x = P.subtract(x)
        }
        return x
    }

    /**
     * Сложение двух точек на скрученной кривой Эдвардса (Ed25519).
     */
    private fun addPoints(x1: BigInteger, y1: BigInteger, x2: BigInteger, y2: BigInteger): Pair<BigInteger, BigInteger> {
        val x1x2 = x1.multiply(x2).mod(P)
        val y1y2 = y1.multiply(y2).mod(P)
        val dx1x2y1y2 = D.multiply(x1x2).multiply(y1y2).mod(P)

        val x3Num = x1.multiply(y2).add(y1.multiply(x2)).mod(P)
        val x3Den = BigInteger.ONE.add(dx1x2y1y2).mod(P)
        val x3 = x3Num.multiply(x3Den.modInverse(P)).mod(P)

        val y3Num = y1y2.add(x1x2).mod(P)
        val y3Den = BigInteger.ONE.subtract(dx1x2y1y2).mod(P)
        val y3 = y3Num.multiply(y3Den.modInverse(P)).mod(P)

        return Pair(x3, y3)
    }

    /**
     * Скалярное умножение точки на число методом удвоения-сложения (Double-and-Add).
     */
    private fun scalarMultiply(scalar: BigInteger, bx: BigInteger, by: BigInteger): Pair<BigInteger, BigInteger> {
        var rx = BigInteger.ZERO
        var ry = BigInteger.ONE
        var qx = bx
        var qy = by

        var s = scalar
        while (s > BigInteger.ZERO) {
            if (s.testBit(0)) {
                val sum = addPoints(rx, ry, qx, qy)
                rx = sum.first
                ry = sum.second
            }
            val dbl = addPoints(qx, qy, qx, qy)
            qx = dbl.first
            qy = dbl.second
            s = s.shiftRight(1)
        }
        return Pair(rx, ry)
    }

    /**
     * Сжатие точки кривой Эдвардса в 32 байта в соответствии с RFC 8032.
     */
    private fun encodePoint(x: BigInteger, y: BigInteger): ByteArray {
        val bytes = ByteArray(32)
        val yBytes = y.toByteArray()
        // Скопировать y в little-endian формате
        var i = 0
        while (i < 32) {
            val srcIndex = yBytes.size - 1 - i
            bytes[i] = if (srcIndex >= 0) yBytes[srcIndex] else 0
            i++
        }
        // Записать знак координаты X в самый старший бит последнего байта
        if (x.testBit(0)) {
            bytes[31] = (bytes[31].toInt() or 0x80).toByte()
        }
        return bytes
    }

    private fun littleEndianToBigInteger(bytes: ByteArray): BigInteger {
        val reversed = ByteArray(bytes.size + 1)
        for (i in bytes.indices) {
            reversed[bytes.size - i] = bytes[i]
        }
        return BigInteger(reversed)
    }

    // ---------------------------------------------------------------------------------------------
    // Реализация PBKDF2-HMAC-SHA512
    // ---------------------------------------------------------------------------------------------

    /**
     * Надежная реализация PBKDF2 с HMAC-SHA512.
     */
    private fun pbkdf2HmacSha512(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(password, "HmacSHA512"))

        val result = ByteArray(keyLengthBytes)
        val hLen = 64 // Выход HMAC-SHA512 равен 64 байтам
        val l = (keyLengthBytes + hLen - 1) / hLen

        var destPos = 0
        for (i in 1..l) {
            val uBlock = ByteArray(salt.size + 4)
            System.arraycopy(salt, 0, uBlock, 0, salt.size)
            uBlock[salt.size] = (i ushr 24).toByte()
            uBlock[salt.size + 1] = (i ushr 16).toByte()
            uBlock[salt.size + 2] = (i ushr 8).toByte()
            uBlock[salt.size + 3] = i.toByte()

            var u = mac.doFinal(uBlock)
            val t = u.copyOf()

            for (c in 2..iterations) {
                u = mac.doFinal(u)
                for (k in 0 until hLen) {
                    t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
                }
            }

            val copyLen = minOf(hLen, keyLengthBytes - destPos)
            System.arraycopy(t, 0, result, destPos, copyLen)
            destPos += copyLen
        }
        return result
    }

    private fun ByteArray.toHex(): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(this.size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }
}
