package org.aponcet.authserver

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


data class EncodedPasswordAndSalt(val encodedPassword: ByteArray, val salt: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncodedPasswordAndSalt

        if (!encodedPassword.contentEquals(other.encodedPassword)) return false
        if (!salt.contentEquals(other.salt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encodedPassword.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        return result
    }
}

data class PasswordManagerException(override val message: String, val e: Throwable) : Exception(message, e)

class PasswordManager {
    companion object {
        private val RANDOM = SecureRandom()
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH = 256

        fun generateEncodedPassword(password : String): EncodedPasswordAndSalt {
            val salt = nextSalt()
            return EncodedPasswordAndSalt(hash(password, salt), salt)
        }

        fun isPasswordOk(password: String, salt: ByteArray, encodedPassword: ByteArray) : Boolean {
            val encoded = hash(password, salt)
            return encoded.contentEquals(encodedPassword)
        }

        /**
         * Generate salt
         */
        private fun nextSalt() : ByteArray {
            val salt = ByteArray(16)
            RANDOM.nextBytes(salt)
            return salt
        }

        /**
         * Hash password with a given salt
         * @return encoded password
         */
        fun hash(password : String, salt: ByteArray): ByteArray {
            val passwordChar = password.toCharArray()
            val spec = PBEKeySpec(passwordChar, salt, ITERATIONS, KEY_LENGTH)
            Arrays.fill(passwordChar, Char.MIN_VALUE)
            try {
                val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                return skf.generateSecret(spec).encoded
            } catch (e: NoSuchAlgorithmException) {
                throw PasswordManagerException("No such algorithm", e)
            } catch (e: InvalidKeySpecException) {
                throw PasswordManagerException("Invalid Key spec algorithm", e)
            } finally {
                spec.clearPassword()
            }
        }
    }
}