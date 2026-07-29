package com.gem.neteasecloudmd.api

import com.gem.neteasecloudmd.BuildConfig
import com.gem.neteasecloudmd.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed interface UpdateCheckResult {
    data class UpToDate(
        val releaseTag: String,
        val releaseSha: String,
        val releaseUrl: String
    ) : UpdateCheckResult

    data class UpdateAvailable(
        val releaseTag: String,
        val releaseSha: String,
        val releaseUrl: String
    ) : UpdateCheckResult

    data class DevelopmentBuild(
        val releaseTag: String,
        val releaseSha: String,
        val releaseUrl: String
    ) : UpdateCheckResult

    data class DifferentHistory(
        val releaseTag: String,
        val releaseSha: String,
        val releaseUrl: String
    ) : UpdateCheckResult

    data class Failure(val cause: Throwable) : UpdateCheckResult
}

/** Queries the public GitHub API and compares the release commit with this exact build. */
object GitHubUpdateChecker {
    private const val REPOSITORY = "Goldppx/NCMD"
    private const val API_BASE_URL = "https://api.github.com/repos/$REPOSITORY"
    private const val TAG = "UpdateChecker"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val currentSha = BuildConfig.GIT_SHA
            require(currentSha != "unknown") { "This build does not contain a Git commit hash." }

            val release = requestJson("$API_BASE_URL/releases/latest")
            val tag = release.string("tag_name")
            val releaseUrl = release.string("html_url")
            val releaseSha = resolveTagCommit(tag)

            if (currentSha.equals(releaseSha, ignoreCase = true)) {
                return@withContext UpdateCheckResult.UpToDate(tag, releaseSha, releaseUrl)
            }

            when (requestJson("$API_BASE_URL/compare/$currentSha...$releaseSha").string("status")) {
                "ahead" -> UpdateCheckResult.UpdateAvailable(tag, releaseSha, releaseUrl)
                "behind" -> UpdateCheckResult.DevelopmentBuild(tag, releaseSha, releaseUrl)
                "identical" -> UpdateCheckResult.UpToDate(tag, releaseSha, releaseUrl)
                else -> UpdateCheckResult.DifferentHistory(tag, releaseSha, releaseUrl)
            }
        } catch (error: Exception) {
            Logger.w(TAG, "Update check failed: ${error.message}")
            UpdateCheckResult.Failure(error)
        }
    }

    private fun resolveTagCommit(tag: String): String {
        val reference = requestJson("$API_BASE_URL/git/ref/tags/$tag")
        val tagObject = reference["object"]?.jsonObject
            ?: error("GitHub tag reference did not contain an object.")
        val objectType = tagObject.string("type")
        val objectSha = tagObject.string("sha")
        return if (objectType == "commit") {
            objectSha
        } else {
            requestJson("$API_BASE_URL/git/tags/$objectSha")["object"]
                ?.jsonObject
                ?.string("sha")
                ?: error("Annotated GitHub tag did not contain a commit.")
        }
    }

    private fun requestJson(url: String) = client.newCall(
        Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NCMD-Android")
            .build()
    ).execute().use { response ->
        check(response.isSuccessful) { "GitHub request failed with HTTP ${response.code}." }
        val body = response.body?.string().orEmpty()
        check(body.isNotBlank()) { "GitHub returned an empty response." }
        json.parseToJsonElement(body).jsonObject
    }

    private fun Map<String, kotlinx.serialization.json.JsonElement>.string(name: String): String =
        get(name)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: error("GitHub response did not contain '$name'.")
}
