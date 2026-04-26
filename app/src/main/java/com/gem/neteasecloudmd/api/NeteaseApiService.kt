package com.gem.neteasecloudmd.api

import android.util.Log
import com.gem.neteasecloudmd.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
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

@Serializable
data class LyricResponse(
    val code: Int,
    val lrc: LyricData? = null,
    val tlyric: LyricData? = null,
    val romalytic: LyricData? = null
)

@Serializable
data class LyricData(
    val lyric: String? = null,
    val version: Int = 0
)

class NeteaseApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun Request.executeWithUse(): String {
        return client.newCall(this).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    private fun buildWeapiBody(jsonParams: String): Result<String> {
        val encryptedParams = CryptoUtil.weapi(jsonParams)
        val raw = encryptedParams["params"] ?: return Result.failure(Exception("Encryption failed"))
        val encoded = raw.replace("/", "%2F").replace("+", "%2B").replace("=", "%3D")
        return Result.success("params=$encoded&encSecKey=${encryptedParams["encSecKey"]}")
    }

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "NeteaseApi"
        private const val BASE_URL = "https://music.163.com"
    }

    private fun str(id: Int): String = when (id) {
        R.string.api_unknown -> "Unknown"
        R.string.api_unknown_artist -> "Unknown Artist"
        R.string.api_unknown_album -> "Unknown Album"
        R.string.api_unknown_playlist -> "Unknown Playlist"
        else -> ""
    }

    suspend fun sendCaptcha(phone: String): Result<CaptchaSentResult> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "ctcode" to "86",
                "secrete" to "music_middleuser_pclogin",
                "cellphone" to phone
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/sms/captcha/sent")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Sending captcha to: ${request.url}")

            val body = request.executeWithUse()

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
            val params = mapOf(
                "type" to "1",
                "https" to "true",
                "phone" to phone,
                "countrycode" to "86",
                "captcha" to captcha,
                "remember" to "true"
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/w/login/cellphone")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            Log.d(TAG, "Login with captcha to: ${request.url}")

            val (body, cookies) = client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (body.isEmpty()) {
                    return@withContext Result.failure(Exception("Empty response"))
                }
                Pair(body, response.headers("Set-Cookie"))
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
            
            val (body, cookies, responseCode) = client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                Triple(body, response.headers("Set-Cookie"), response.code)
            }
            
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response. Code: $responseCode"))
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

            val body = request.executeWithUse()
            
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
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/user/playlist")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            Log.d(TAG, "Get playlists for uid: $uid")
            Log.d(TAG, "Cookie length: ${cookie.length}")

            val body = request.executeWithUse()

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
    
    suspend fun getPlaylistAllTrackIds(id: Long, cookie: String): Result<List<Long>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "id" to id.toString(),
                "n" to "100000",
                "s" to "8"
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v6/playlist/detail")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<PlaylistDetailResponse>(body)
            if (result.code == 200) {
                val trackIds = result.playlist?.trackIds?.map { it.id } ?: emptyList()
                Result.success(trackIds)
            } else {
                Result.failure(Exception(result.message ?: "Failed to get playlist detail"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSongsDetails(ids: List<Long>, cookie: String): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            if (ids.isEmpty()) return@withContext Result.success(emptyList())

            val cList = ids.map { mapOf("id" to it) }
            val params = mapOf(
                "c" to Json.encodeToString(cList)
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v3/song/detail")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<SongDetailResponse>(body)
            if (result.code == 200) {
                val tracks = result.songs?.map { song ->
                    TrackItem(
                        id = song.id,
                        name = song.name ?: str(R.string.api_unknown),
                        artists = song.ar?.joinToString(", ") { it.name ?: "" } ?: str(R.string.api_unknown_artist),
                        albumName = song.al?.name ?: str(R.string.api_unknown_album),
                        albumPicUrl = song.al?.picUrl,
                        duration = song.dt ?: 0
                    )
                } ?: emptyList()
                // Keep the order matching the input ids
                val trackMap = tracks.associateBy { it.id }
                val orderedTracks = ids.mapNotNull { trackMap[it] }
                Result.success(orderedTracks)
            } else {
                Result.failure(Exception("Failed to get song details"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPlaylistTrackIds(id: Long, cookie: String): Result<List<Long>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "id" to id.toString(),
                "n" to "100000",
                "s" to "8"
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v6/playlist/detail")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<PlaylistDetailResponse>(body)
            if (result.code == 200) {
                val trackIds = result.playlist?.trackIds?.map { it.id } ?: emptyList()
                Result.success(trackIds)
            } else {
                Result.failure(Exception(result.message ?: "Failed to get playlist detail"))
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
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v6/playlist/detail")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            Log.d(TAG, "Get playlist detail for id: $id")

            val body = request.executeWithUse()

            Log.d(TAG, "Playlist detail response: ${body.take(500)}")

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<PlaylistDetailResponse>(body)
            if (result.code == 200) {
                val tracks = result.playlist?.tracks?.map { track ->
                    TrackItem(
                        id = track.id,
                        name = track.name ?: str(R.string.api_unknown),
                        artists = track.ar?.joinToString(", ") { it.name ?: "" } ?: str(R.string.api_unknown_artist),
                        albumName = track.al?.name ?: track.album?.name ?: str(R.string.api_unknown_album),
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
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/song/enhance/player/url/v1")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            Log.d(TAG, "Get song url for id: $id")

            val body = request.executeWithUse()

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

    suspend fun getLyric(id: Long, cookie: String): Result<LyricResponse> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "id" to id.toString(),
                "lv" to "-1",
                "kv" to "-1",
                "tv" to "-1"
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/song/lyric")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()

            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<LyricResponse>(body)
            if (result.code == 200) {
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to get lyrics with code ${result.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lyric Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserPlayRecord(uid: Long, cookie: String, limit: Int = 100): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "uid" to uid.toString(),
                "type" to "0",
                "limit" to limit.toString(),
                "offset" to "0"
            )

            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v1/play/record")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<UserPlayRecordResponse>(body)
            if (result.code != 200) {
                return@withContext Result.failure(Exception(result.message ?: "Failed to get user play record"))
            }

            val tracksFromWeapi = (result.allData ?: result.weekData ?: emptyList())
                .mapNotNull { it.song }
                .map { song ->
                    TrackItem(
                        id = song.id,
                        name = song.name ?: str(R.string.api_unknown),
                        artists = song.ar?.joinToString(", ") { it.name ?: "" } ?: str(R.string.api_unknown_artist),
                        albumName = song.al?.name ?: str(R.string.api_unknown_album),
                        albumPicUrl = song.al?.picUrl,
                        duration = song.dt ?: 0
                    )
                }

            if (tracksFromWeapi.isNotEmpty()) {
                return@withContext Result.success(tracksFromWeapi)
            }

            val fallbackRequest = Request.Builder()
                .url("$BASE_URL/api/v1/play/record?uid=$uid&type=0")
                .get()
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val fallbackBody = fallbackRequest.executeWithUse()
            if (fallbackBody.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val fallbackResult = json.decodeFromString<UserPlayRecordResponse>(fallbackBody)
            val fallbackTracks = (fallbackResult.allData ?: fallbackResult.weekData ?: emptyList())
                .mapNotNull { it.song }
                .map { song ->
                    TrackItem(
                        id = song.id,
                        name = song.name ?: str(R.string.api_unknown),
                        artists = song.ar?.joinToString(", ") { it.name ?: "" } ?: str(R.string.api_unknown_artist),
                        albumName = song.al?.name ?: str(R.string.api_unknown_album),
                        albumPicUrl = song.al?.picUrl,
                        duration = song.dt ?: 0
                    )
                }

            Result.success(fallbackTracks)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPersonalFm(cookie: String, limit: Int = 6): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("csrf_token" to "")
            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/v1/radio/get")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<PersonalFmResponse>(body)
            if (result.code != 200) {
                return@withContext Result.failure(Exception("Failed to get personal FM"))
            }

            val tracks = result.data
                ?.take(limit)
                ?.map { track ->
                    TrackItem(
                        id = track.id,
                        name = track.name ?: str(R.string.api_unknown),
                        artists = track.artists?.joinToString(", ") { it.name ?: "" }
                            ?: track.ar?.joinToString(", ") { it.name ?: "" }
                            ?: str(R.string.api_unknown_artist),
                        albumName = track.album?.name ?: track.al?.name ?: str(R.string.api_unknown_album),
                        albumPicUrl = track.album?.picUrl ?: track.al?.picUrl,
                        duration = track.duration ?: track.dt ?: 0
                    )
                }
                ?: emptyList()

            Result.success(tracks)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRecentPlaySongs(cookie: String, limit: Int = 100): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/record/recent/song")
                .get()
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<RecentPlaySongsResponse>(body)
            if (result.code != 200) {
                return@withContext Result.failure(Exception("Failed to get recent play songs"))
            }

            val tracks = result.data?.list
                ?.take(limit)
                ?.mapNotNull { it.data }
                ?.map { song ->
                    TrackItem(
                        id = song.id,
                        name = song.name ?: str(R.string.api_unknown),
                        artists = song.ar?.joinToString(", ") { it.name ?: "" } ?: str(R.string.api_unknown_artist),
                        albumName = song.al?.name ?: str(R.string.api_unknown_album),
                        albumPicUrl = song.al?.picUrl,
                        duration = song.dt ?: 0
                    )
                }
                ?: emptyList()

            Result.success(tracks)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to get recent play songs"))
        }
    }

    suspend fun getLikedSongIds(uid: Long, cookie: String): Result<Set<Long>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("uid" to uid.toString())
            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/song/like/get")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = json.decodeFromString<LikedSongIdsResponse>(body)
            if (result.code != 200) {
                return@withContext Result.failure(Exception("Failed to get liked songs"))
            }

            Result.success((result.ids ?: emptyList()).toSet())
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Serializable
    private data class SongLikeParams(
        val trackId: Long,
        val userid: Long,
        val like: Boolean
    )

    suspend fun setSongLiked(songId: Long, like: Boolean, cookie: String, userId: Long = 0L): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId <= 0L) return@withContext Result.failure(Exception("Invalid userId"))

            val params = SongLikeParams(songId, userId, like)
            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val request = Request.Builder()
                .url("$BASE_URL/weapi/song/like")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = runCatching { json.decodeFromString<SimpleCodeResponse>(body) }.getOrNull()
                ?: return@withContext Result.failure(Exception("Failed to parse response"))

            if (result.code == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception(result.msg ?: result.message ?: "Like operation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "setSongLiked failed", e)
            Result.failure(e)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long, cookie: String): Result<Boolean> =
        manipulatePlaylistTracks(playlistId, listOf(trackId), "add", cookie)

    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>, cookie: String): Result<Boolean> =
        manipulatePlaylistTracks(playlistId, trackIds, "del", cookie)

    private suspend fun manipulatePlaylistTracks(
        playlistId: Long,
        trackIds: List<Long>,
        operation: String,
        cookie: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (playlistId <= 0L || trackIds.isEmpty()) {
                return@withContext Result.failure(Exception("Invalid playlist or tracks"))
            }

            // Build form body matching the reference implementation
            val trackStrings = trackIds.map { it.toString() }
            val formBody = mapOf(
                "op" to operation,
                "pid" to playlistId.toString(),
                "trackIds" to Json.encodeToString(trackStrings),
                "imme" to "true"
            )
            val encodedBody = formBody.entries.joinToString("&") { (key, value) ->
                "${key}=${java.net.URLEncoder.encode(value, "UTF-8")}"
            }

            val request = Request.Builder()
                .url("$BASE_URL/api/playlist/manipulate/tracks")
                .post(encodedBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = runCatching { json.decodeFromString<SimpleCodeResponse>(body) }.getOrNull()
                ?: return@withContext Result.failure(Exception("Failed to parse response"))

            if (result.code == 200) {
                Result.success(true)
            } else if (result.code == 512) {
                // Retry with duplicated tracks (known Netease bug workaround)
                val retryParams = mapOf(
                    "op" to operation,
                    "pid" to playlistId.toString(),
                    "trackIds" to Json.encodeToString(trackStrings + trackStrings),
                    "imme" to "true"
                )
                val retryEncoded = retryParams.entries.joinToString("&") { (key, value) ->
                    "${key}=${java.net.URLEncoder.encode(value, "UTF-8")}"
                }
                val retryReq = Request.Builder()
                    .url("$BASE_URL/api/playlist/manipulate/tracks")
                    .post(retryEncoded.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .header("Cookie", cookie)
                    .build()
                val retryResponseBody = retryReq.executeWithUse()
                if (retryResponseBody.isNotEmpty()) {
                    val retryResult = runCatching { json.decodeFromString<SimpleCodeResponse>(retryResponseBody) }.getOrNull()
                    if (retryResult?.code == 200) return@withContext Result.success(true)
                }
                Result.failure(Exception(result.msg ?: result.message ?: "Playlist operation failed"))
            } else {
                Result.failure(Exception(result.msg ?: result.message ?: "Playlist operation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun audioMatch(
        durationMs: Int,
        audioFingerprint: String,
        cookie: String
    ): Result<Long?> = withContext(Dispatchers.IO) {
        try {
            if (durationMs <= 0 || audioFingerprint.isBlank()) {
                return@withContext Result.failure(Exception("Invalid audio match params"))
            }

            val url = "$BASE_URL/audio/match?duration=$durationMs&audioFP=$audioFingerprint"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Cookie", cookie)
                .build()

            val body = request.executeWithUse()
            if (body.isBlank()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val root = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
            val code = root["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            if (code != 200) {
                return@withContext Result.failure(Exception("Audio match failed: $code"))
            }

            val resultObj = root["result"]?.jsonObject
            val songId = resultObj?.get("song")
                ?.jsonObject
                ?.get("id")
                ?.jsonPrimitive
                ?.content
                ?.toLongOrNull()
            Result.success(songId)
        } catch (e: Exception) {
            Log.e(TAG, "audioMatch exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchSongs(keywords: String, limit: Int = 30): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            val urls = listOf(
                "$BASE_URL/api/search",
                "$BASE_URL/api/search/get/web",
                "$BASE_URL/search"
            )

            for (base in urls) {
                val httpUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("keywords", keywords)
                    .addQueryParameter("s", keywords)
                    .addQueryParameter("type", "1")
                    .addQueryParameter("limit", limit.toString())
                    .build()

                val request = Request.Builder()
                    .url(httpUrl)
                    .get()
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()

                val body = request.executeWithUse()
                if (body.isEmpty()) continue

                val result = runCatching { json.decodeFromString<SearchSongResponse>(body) }.getOrNull() ?: continue
                val tracks = result.result?.songs?.map { song ->
                    TrackItem(
                        id = song.id,
                        name = song.name ?: str(R.string.api_unknown),
                        artists = (song.artists ?: song.ar)?.joinToString(", ") { it.name ?: "" } ?: str(R.string.api_unknown_artist),
                        albumName = (song.album ?: song.al)?.name ?: str(R.string.api_unknown_album),
                        albumPicUrl = (song.album ?: song.al)?.picUrl,
                        duration = song.duration ?: song.dt ?: 0
                    )
                } ?: emptyList()

                if (tracks.isNotEmpty()) return@withContext Result.success(tracks)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPlaylists(keywords: String, limit: Int = 30): Result<List<PlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val urls = listOf(
                "$BASE_URL/api/search",
                "$BASE_URL/api/search/get/web",
                "$BASE_URL/search"
            )

            for (base in urls) {
                val httpUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("keywords", keywords)
                    .addQueryParameter("s", keywords)
                    .addQueryParameter("type", "1000")
                    .addQueryParameter("limit", limit.toString())
                    .build()

                val request = Request.Builder()
                    .url(httpUrl)
                    .get()
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()

                val body = request.executeWithUse()
                if (body.isEmpty()) continue

                val result = runCatching { json.decodeFromString<SearchPlaylistResponse>(body) }.getOrNull() ?: continue
                val playlists = result.result?.playlists?.map { playlist ->
                    PlaylistItem(
                        id = playlist.id,
                        name = playlist.name ?: str(R.string.api_unknown_playlist),
                        coverImgUrl = playlist.coverImgUrl,
                        trackCount = playlist.trackCount ?: 0
                    )
                } ?: emptyList()

                if (playlists.isNotEmpty()) return@withContext Result.success(playlists)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAlbums(keywords: String, limit: Int = 30): Result<List<SearchAlbumItem>> = withContext(Dispatchers.IO) {
        try {
            val urls = listOf(
                "$BASE_URL/api/search",
                "$BASE_URL/api/search/get/web",
                "$BASE_URL/search"
            )

            for (base in urls) {
                val httpUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("keywords", keywords)
                    .addQueryParameter("s", keywords)
                    .addQueryParameter("type", "10")
                    .addQueryParameter("limit", limit.toString())
                    .build()

                val request = Request.Builder()
                    .url(httpUrl)
                    .get()
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()

                val body = request.executeWithUse()
                if (body.isEmpty()) continue

                val result = runCatching { json.decodeFromString<SearchAlbumResponse>(body) }.getOrNull() ?: continue
                val albums = result.result?.albums?.map { album ->
                    SearchAlbumItem(
                        id = album.id,
                        name = album.name ?: str(R.string.api_unknown_album),
                        artist = (album.artist ?: album.artists?.firstOrNull())?.name ?: str(R.string.api_unknown_artist),
                        picUrl = album.picUrl,
                        size = album.size ?: 0
                    )
                } ?: emptyList()

                if (albums.isNotEmpty()) return@withContext Result.success(albums)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSuggest(keywords: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val urls = listOf(
                "$BASE_URL/api/search/suggest",
                "$BASE_URL/search/suggest"
            )

            for (base in urls) {
                val httpUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("keywords", keywords)
                    .addQueryParameter("type", "mobile")
                    .build()

                val request = Request.Builder()
                    .url(httpUrl)
                    .get()
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()

                val body = request.executeWithUse()
                if (body.isEmpty()) continue

                val suggestions = parseSuggestKeywords(body)
                if (suggestions.isNotEmpty()) return@withContext Result.success(suggestions)
            }

            val params = mapOf(
                "s" to keywords,
                "csrf_token" to ""
            )
            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val weapiRequest = Request.Builder()
                .url("$BASE_URL/weapi/search/suggest/web")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            val weapiBody = weapiRequest.executeWithUse()
            val weapiSuggestions = parseSuggestKeywords(weapiBody)
            if (weapiSuggestions.isNotEmpty()) {
                return@withContext Result.success(weapiSuggestions)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHotSearchList(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val urls = listOf(
                "$BASE_URL/api/search/hot/detail",
                "$BASE_URL/search/hot/detail",
                "$BASE_URL/api/search/hot"
            )

            for (url in urls) {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()

                val body = request.executeWithUse()
                if (body.isEmpty()) continue

                val words = parseHotWords(body)
                if (words.isNotEmpty()) return@withContext Result.success(words)
            }

            val params = mapOf("csrf_token" to "")
            val jsonParams = Json.encodeToString(params)
            val requestBody = buildWeapiBody(jsonParams).getOrElse { return@withContext Result.failure(it) }

            val weapiRequest = Request.Builder()
                .url("$BASE_URL/weapi/search/hot/detail")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Referer", "https://music.163.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            val weapiBody = weapiRequest.executeWithUse()
            val weapiWords = parseHotWords(weapiBody)
            if (weapiWords.isNotEmpty()) {
                return@withContext Result.success(weapiWords)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbumTracks(albumId: Long, cookie: String = ""): Result<List<TrackItem>> = withContext(Dispatchers.IO) {
        try {
            val urls = listOf(
                "$BASE_URL/api/album?id=$albumId",
                "$BASE_URL/album?id=$albumId",
                "$BASE_URL/api/v1/album/$albumId"
            )

            for (url in urls) {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Referer", "https://music.163.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .apply {
                        if (cookie.isNotBlank()) header("Cookie", cookie)
                    }
                    .build()

                val body = request.executeWithUse()
                if (body.isEmpty()) continue

                val tracks = parseAlbumTracks(body)
                if (tracks.isNotEmpty()) return@withContext Result.success(tracks)
            }

            Result.failure(Exception("Failed to get album tracks"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAlbumTracks(body: String): List<TrackItem> {
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val songs = root["songs"]?.jsonArray ?: return emptyList()
            songs.mapNotNull { songElement ->
                val songObj = songElement.jsonObject
                val id = songObj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                val name = songObj["name"]?.jsonPrimitive?.content ?: str(R.string.api_unknown)
                val duration = songObj["dt"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val artists = songObj["ar"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                    ?.joinToString(", ")
                    ?: str(R.string.api_unknown_artist)
                val albumObj = songObj["al"]?.jsonObject
                val albumName = albumObj?.get("name")?.jsonPrimitive?.content ?: str(R.string.api_unknown_album)
                val albumPicUrl = albumObj?.get("picUrl")?.jsonPrimitive?.content

                TrackItem(
                    id = id,
                    name = name,
                    artists = artists,
                    albumName = albumName,
                    albumPicUrl = albumPicUrl,
                    duration = duration
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun parseSuggestKeywords(body: String): List<String> {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val result = root["result"]?.jsonObject ?: return emptyList()

            val allMatch = result["allMatch"]?.jsonArray?.mapNotNull { item ->
                item.jsonObject["keyword"]?.jsonPrimitive?.content
            } ?: emptyList()
            if (allMatch.isNotEmpty()) return allMatch

            val songs = result["songs"]?.jsonArray?.mapNotNull { item ->
                item.jsonObject["name"]?.jsonPrimitive?.content
            } ?: emptyList()
            if (songs.isNotEmpty()) return songs

            val albums = result["albums"]?.jsonArray?.mapNotNull { item ->
                item.jsonObject["name"]?.jsonPrimitive?.content
            } ?: emptyList()
            if (albums.isNotEmpty()) return albums

            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseHotWords(body: String): List<String> {
        return try {
            val root = json.parseToJsonElement(body).jsonObject

            val dataWords = root["data"]?.jsonArray?.mapNotNull { item ->
                item.jsonObject["searchWord"]?.jsonPrimitive?.content
            } ?: emptyList()
            if (dataWords.isNotEmpty()) return dataWords

            val hotWords = root["result"]
                ?.jsonObject
                ?.get("hots")
                ?.jsonArray
                ?.mapNotNull { item -> item.jsonObject["first"]?.jsonPrimitive?.content }
                ?: emptyList()
            if (hotWords.isNotEmpty()) return hotWords

            emptyList()
        } catch (_: Exception) {
            emptyList()
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
    val tracks: List<TrackResponse>? = null,
    val trackIds: List<TrackIdItem>? = null
)

@Serializable
data class TrackIdItem(
    val id: Long
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

@Serializable
data class UserPlayRecordResponse(
    val code: Int,
    val allData: List<UserPlayRecordItem>? = null,
    val weekData: List<UserPlayRecordItem>? = null,
    val message: String? = null
)

@Serializable
data class UserPlayRecordItem(
    val song: SongDetail? = null,
    val playCount: Int? = null,
    val score: Int? = null
)

@Serializable
data class PersonalFmResponse(
    val code: Int,
    val data: List<PersonalFmTrack>? = null
)

@Serializable
data class PersonalFmTrack(
    val id: Long,
    val name: String? = null,
    val artists: List<SongArtist>? = null,
    val ar: List<SongArtist>? = null,
    val album: SongAlbum? = null,
    val al: SongAlbum? = null,
    val duration: Int? = null,
    val dt: Int? = null
)

@Serializable
data class LikedSongIdsResponse(
    val code: Int,
    val ids: List<Long>? = null
)

@Serializable
data class SimpleCodeResponse(
    val code: Int,
    val msg: String? = null,
    val message: String? = null
)

@Serializable
data class RecentPlaySongsResponse(
    val code: Int,
    val data: RecentPlaySongsData? = null
)

@Serializable
data class RecentPlaySongsData(
    val list: List<RecentPlaySongItem>? = null
)

@Serializable
data class RecentPlaySongItem(
    val data: SongDetail? = null
)

@Serializable
data class SearchSongResponse(
    val code: Int,
    val result: SearchSongResult? = null
)

@Serializable
data class SearchSongResult(
    val songs: List<SearchSongItem>? = null
)

@Serializable
data class SearchSongItem(
    val id: Long,
    val name: String? = null,
    @SerialName("artists")
    val artists: List<SongArtist>? = null,
    @SerialName("ar")
    val ar: List<SongArtist>? = null,
    @SerialName("album")
    val album: SongAlbum? = null,
    @SerialName("al")
    val al: SongAlbum? = null,
    @SerialName("duration")
    val duration: Int? = null,
    @SerialName("dt")
    val dt: Int? = null
)

@Serializable
data class SearchPlaylistResponse(
    val code: Int,
    val result: SearchPlaylistResult? = null
)

@Serializable
data class SearchPlaylistResult(
    val playlists: List<SearchPlaylistItem>? = null
)

@Serializable
data class SearchPlaylistItem(
    val id: Long,
    val name: String? = null,
    val coverImgUrl: String? = null,
    val trackCount: Int? = null
)

@Serializable
data class SearchAlbumResponse(
    val code: Int,
    val result: SearchAlbumResult? = null
)

@Serializable
data class SearchAlbumResult(
    val albums: List<SearchAlbumRaw>? = null
)

@Serializable
data class SearchAlbumRaw(
    val id: Long,
    val name: String? = null,
    val picUrl: String? = null,
    val size: Int? = null,
    val artist: SongArtist? = null,
    val artists: List<SongArtist>? = null
)

data class SearchAlbumItem(
    val id: Long,
    val name: String,
    val artist: String,
    val picUrl: String?,
    val size: Int
)

@Serializable
data class SearchSuggestResponse(
    val code: Int,
    val result: SearchSuggestResult? = null
)

@Serializable
data class SearchSuggestResult(
    val allMatch: List<SearchSuggestItem>? = null
)

@Serializable
data class SearchSuggestItem(
    val keyword: String? = null
)

@Serializable
data class HotSearchResponse(
    val code: Int,
    val data: List<HotSearchItem>? = null
)

@Serializable
data class HotSearchItem(
    val searchWord: String? = null
)

@Serializable
data class AlbumDetailResponse(
    val code: Int,
    val songs: List<SongDetail>? = null
)
