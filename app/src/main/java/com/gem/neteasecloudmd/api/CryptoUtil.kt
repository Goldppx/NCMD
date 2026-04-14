package com.gem.neteasecloudmd.api

import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.random.Random

object CryptoUtil {
    private const val AES_KEY = "0CoJUm6Qyw8W8jud"
    private const val IV = "0102030405060708"
    private const val BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    
    private val PUBLIC_KEY_PEM = """
        -----BEGIN PUBLIC KEY-----
        MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgtQn2JZ34ZC28NWYpAUd98iZ37BUrX/aKzmFbt7clFSs6sXqHauqKWqdtLkF2KexO40H1YTX8z2lSgBBOAxLsvaklV8k4cBFK9snQXE9/DDaFt6Rr7iVZMldczhC0JNgTz+SHXT6CBHuX3e9SdB1Ua44oncaTWz7OBGLbCiK45wIDAQAB
        -----END PUBLIC KEY-----
    """.trimIndent()

    private val publicKey: RSAPublicKey by lazy {
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(
            PUBLIC_KEY_PEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
        ))
        KeyFactory.getInstance("RSA").generatePublic(keySpec) as RSAPublicKey
    }

    fun weapi(jsonData: String): Map<String, String> {
        val secretKey = generateSecretKey(16)
        
        val firstEncrypted = aesEncrypt(jsonData, AES_KEY)
        val secondEncrypted = aesEncrypt(firstEncrypted, secretKey)
        
        val reversedKey = secretKey.reversed()
        val encSecKey = rsaEncrypt(reversedKey)

        return mapOf(
            "params" to secondEncrypted,
            "encSecKey" to encSecKey
        )
    }

    private fun generateSecretKey(length: Int): String {
        return (1..length).map { BASE62[Random.nextInt(BASE62.length)] }.joinToString("")
    }

    private fun aesEncrypt(text: String, key: String): String {
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "AES")
        val ivSpec = javax.crypto.spec.IvParameterSpec(IV.toByteArray())
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(text.toByteArray())
        return Base64.getEncoder().encodeToString(encrypted)
    }

    private fun rsaEncrypt(text: String): String {
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(text.toByteArray())
        return bytesToHex(encrypted)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
