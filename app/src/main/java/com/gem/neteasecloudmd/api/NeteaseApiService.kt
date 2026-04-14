package com.gem.neteasecloudmd.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable
data class LoginResult(
    val code: Int,
    val msg: String? = null,
    val message: String? = null,
    val profile: UserProfile? = null,
    val cookie: List<String>? = null
)

@Serializable
data class CaptchaSentResult(
    val code: Int,
    val msg: String? = null
)

@Serializable
data class CaptchaVerifyResult(
    val code: Int,
    val msg: String? = null,
    val message: String? = null
)

@Serializable
data class PlaylistItem(
    val id: Long,
    val name: String,
    val coverImgUrl: String? = null,
    val trackCount: Int = 0,
    val creator: UserCreator? = null
)

@Serializable
data class UserCreator(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null
)

@Serializable
data class PlaylistResponse(
    val code: Int,
    val playlist: List<PlaylistItem>? = null,
    val message: String? = null
)

@Serializable
data class LoginResponse(
    val code: Int,
    val profile: UserProfile? = null,
    val cookie: String? = null,
    val msg: String? = null
)

@Serializable
data class UserProfile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null
)

class NeteaseApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "NeteaseApi"
        private const val BASE_URL = "https://music.163.com"
    }

    suspend fun sendCaptcha(phone: String): Result<CaptchaSentResult> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "ctcode" to "86",
                "secrete" to "music_middleuser_pclogin",
                "cellphone" to phone
            )

            val jsonParams = Json.encodeToString(params)
            val encryptedParams = CryptoUtil.weapi(jsonParams)

            val encodedParams = encryptedParams["params"]
                ?.replace("/", "%2F")
                ?.replace("+", "%2B")
                ?.replace("=", "%3D")

            val requestBody = "params=$encodedParams&encSecKey=${encryptedParams["encSecKey"]}"

            val request = Request.Builder()
                .url("$BASE_URL/weapi/sms/captcha/sent")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Sending captcha to: ${request.url}")

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "Captcha sent response: $body")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<CaptchaSentResult>(body)
            if (result.code == 200) {
                Result.success(result)
            } else {
                Result.failure(Exception(result.msg ?: "Failed to send captcha"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyCaptchaAndLogin(phone: String, captcha: String): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            val md5Password = md5(captcha)
            
            val params = mapOf(
                "type" to "1",
                "https" to "true",
                "phone" to phone,
                "countrycode" to "86",
                "captcha" to captcha,
                "remember" to "true"
            )

            val jsonParams = Json.encodeToString(params)
            val encryptedParams = CryptoUtil.weapi(jsonParams)

            val encodedParams = encryptedParams["params"]
                ?.replace("/", "%2F")
                ?.replace("+", "%2B")
                ?.replace("=", "%3D")

            val requestBody = "params=$encodedParams&encSecKey=${encryptedParams["encSecKey"]}"

            val request = Request.Builder()
                .url("$BASE_URL/weapi/w/login/cellphone")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Login with captcha to: ${request.url}")

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "Login response: $body")
            
            val cookies = response.headers("Set-Cookie")
            Log.d(TAG, "Cookies from headers: ${cookies.joinToString()}")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<LoginResult>(body)
            if (result.code == 200) {
                val loginResult = result.copy(cookie = cookies.ifEmpty { result.cookie })
                Result.success(loginResult)
            } else {
                Result.failure(Exception(result.msg ?: result.message ?: "Login failed with code ${result.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(phone: String, password: String): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            val md5Password = md5(password)
            
            val params = mapOf(
                "type" to "1",
                "https" to "true",
                "phone" to phone,
                "countrycode" to "86",
                "password" to md5Password,
                "remember" to "true"
            )

            val jsonParams = Json.encodeToString(params)
            Log.d(TAG, "JSON params: $jsonParams")
            
            val encryptedParams = CryptoUtil.weapi(jsonParams)

            val encodedParams = encryptedParams["params"]
                ?.replace("/", "%2F")
                ?.replace("+", "%2B")
                ?.replace("=", "%3D")
            
            val requestBody = "params=$encodedParams&encSecKey=${encryptedParams["encSecKey"]}"

            val request = Request.Builder()
                .url("$BASE_URL/weapi/w/login/cellphone")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Sending request to: ${request.url}")
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response body: $body")
            
            val cookies = response.headers("Set-Cookie")
            Log.d(TAG, "Cookies from headers: ${cookies.joinToString()}")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response. Code: ${response.code}"))
            }

            val result = json.decodeFromString<LoginResult>(body)
            if (result.code == 200) {
                val loginResult = result.copy(cookie = cookies.ifEmpty { result.cookie })
                Result.success(loginResult)
            } else {
                Result.failure(Exception(result.msg ?: result.message ?: "Login failed with code ${result.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun loginWithCookie(cookie: String): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/weapi/w/login/cellphone")
                .post("".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            Log.d(TAG, "Cookie login response: $body")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<LoginResult>(body)
            if (result.code == 200) {
                Result.success(result)
            } else {
                Result.failure(Exception(result.msg ?: result.message ?: "Login failed with code ${result.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun getUserPlaylists(uid: Long, cookie: String): Result<PlaylistResponse> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "uid" to uid.toString(),
                "limit" to "30",
                "offset" to "0"
            )

            val jsonParams = Json.encodeToString(params)
            val encryptedParams = CryptoUtil.weapi(jsonParams)

            val encodedParams = encryptedParams["params"]
                ?.replace("/", "%2F")
                ?.replace("+", "%2B")
                ?.replace("=", "%3D")

            val requestBody = "params=$encodedParams&encSecKey=${encryptedParams["encSecKey"]}"

            val request = Request.Builder()
                .url("$BASE_URL/weapi/user/playlist")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            Log.d(TAG, "Get playlists for uid: $uid")
            Log.d(TAG, "Cookie length: ${cookie.length}")
            Log.d(TAG, "Cookie preview: ${cookie.take(100)}")

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "Playlists response code: ${response.code}")
            Log.d(TAG, "Playlists response: $body")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<PlaylistResponse>(body)
            if (result.code == 200) {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "Failed to get playlists"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPlaylistDetail(id: Long, cookie: String, offset: Int = 0, limit: Int = 20): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "id" to id.toString(),
                "n" to "100000",
                "s" to "8"
            )

            val jsonParams = Json.encodeToString(params)
            val encryptedParams = CryptoUtil.weapi(jsonParams)

            val encodedParams = encryptedParams["params"]
                ?.replace("/", "%2F")
                ?.replace("+", "%2B")
                ?.replace("=", "%3D")

            val requestBody = "params=$encodedParams&encSecKey=${encryptedParams["encSecKey"]}"

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v6/playlist/detail")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            Log.d(TAG, "Get playlist detail for id: $id")

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "Playlist detail response: ${body.take(500)}")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<PlaylistDetailResponse>(body)
            if (result.code == 200) {
                val tracks = result.playlist?.tracks?.map { track ->
                    TrackItem(
                        id = track.id,
                        name = track.name ?: "未知",
                        artists = track.ar?.joinToString(", ") { it.name ?: "" } ?: "未知艺术家",
                        albumName = track.al?.name ?: track.album?.name ?: "未知专辑",
                        albumPicUrl = track.al?.picUrl ?: track.album?.picUrl,
                        duration = track.dt ?: 0
                    )
                } ?: emptyList()
                Result.success(tracks)
            } else {
                Result.failure(Exception(result.message ?: "Failed to get playlist detail"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getSongUrl(id: Long, cookie: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "ids" to "[$id]",
                "level" to "standard",
                "encodeType" to "aac",
                "csrf_token" to ""
            )

            val jsonParams = Json.encodeToString(params)
            val encryptedParams = CryptoUtil.weapi(jsonParams)

            val encodedParams = encryptedParams["params"]
                ?.replace("/", "%2F")
                ?.replace("+", "%2B")
                ?.replace("=", "%3D")

            val requestBody = "params=$encodedParams&encSecKey=${encryptedParams["encSecKey"]}"

            val request = Request.Builder()
                .url("$BASE_URL/weapi/song/enhance/player/url/v1")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            Log.d(TAG, "Get song url for id: $id")

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "Song url response: $body")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<SongUrlResponse>(body)
            if (result.code == 200) {
                val url = result.data?.firstOrNull()?.url
                if (url != null) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("No URL found for song"))
                }
            } else {
                Result.failure(Exception("Failed to get song url"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

@Serializable
data class ArtistResponse(
    val id: Long,
    val name: String? = null
)

@Serializable
data class AlbumResponse(
    val id: Long,
    val name: String? = null,
    @SerialName("picUrl")
    val picUrl: String? = null,
    @SerialName("pic")
    val pic: Long? = null
)

@Serializable
data class SongUrlResponse(
    val code: Int,
    val data: List<SongUrlData>? = null
)

@Serializable
data class SongUrlData(
    val id: Long,
    val url: String? = null,
    val type: String? = null,
    val level: String? = null,
    val size: Long? = null
)

@Serializable
data class PlaylistDetailResponse(
    val code: Int,
    val playlist: PlaylistWithTracks? = null,
    val message: String? = null
)

@Serializable
data class PlaylistWithTracks(
    val id: Long,
    val name: String,
    val tracks: List<TrackResponse>? = null
)

@Serializable
data class TrackResponse(
    val id: Long,
    val name: String? = null,
    val ar: List<ArtistResponse>? = null,
    val album: AlbumResponse? = null,
    val al: AlbumResponse? = null,
    val dt: Int? = null
)

@Serializable
data class SongDetailResponse(
    val code: Int,
    val songs: List<SongDetail>? = null
)

@Serializable
data class SongDetail(
    val id: Long,
    val name: String? = null,
    val ar: List<SongArtist>? = null,
    val al: SongAlbum? = null,
    val dt: Int? = null
)

@Serializable
data class SongArtist(
    val id: Long,
    val name: String? = null
)

@Serializable
data class SongAlbum(
    val id: Long,
    val name: String? = null,
    val picUrl: String? = null
)
