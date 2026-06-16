package com.oncewind.sms2Email.util

import android.util.Base64

object ConfigEncryptor {

    private const val KEY = "sms_forward_key_2024"

    fun encrypt(plainText: String): String {
        val bytes = plainText.toByteArray()
        val keyBytes = KEY.toByteArray()
        val encrypted = ByteArray(bytes.size)
        
        for (i in bytes.indices) {
            encrypted[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        return try {
            val encrypted = Base64.decode(cipherText, Base64.NO_WRAP)
            val keyBytes = KEY.toByteArray()
            val decrypted = ByteArray(encrypted.size)
            
            for (i in encrypted.indices) {
                decrypted[i] = (encrypted[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            
            String(decrypted)
        } catch (e: Exception) {
            ""
        }
    }
}