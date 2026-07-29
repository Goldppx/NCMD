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
    private const val WEB_BASE_URL = "https://github.com/$REPOSITORY"
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

            val release = runCatching { latestReleaseFromApi() }
                .getOrElse { apiError ->
                    Logger.w(TAG, "GitHub API unavailable (${apiError.message}); falling back to GitHub web.")
                    latestReleaseFromWeb()
                }

            if (currentSha.equals(release.sha, ignoreCase = true)) {
                return@withContext UpdateCheckResult.UpToDate(release.tag, release.sha, release.url)
            }

            when (runCatching {
                requestJson("$API_BASE_URL/compare/$currentSha...${release.sha}").string("status")
            }.getOrElse { compareError ->
                Logger.w(TAG, "GitHub comparison unavailable: ${compareError.message}")
                "unknown"
            }) {
                "ahead" -> UpdateCheckResult.UpdateAvailable(release.tag, release.sha, release.url)
                "behind" -> UpdateCheckResult.DevelopmentBuild(release.tag, release.sha, release.url)
                "identical" -> UpdateCheckResult.UpToDate(release.tag, release.sha, release.url)
                else -> UpdateCheckResult.DifferentHistory(release.tag, release.sha, release.url)
            }
        } catch (error: Exception) {
            Logger.w(TAG, "Update check failed: ${error.message}")
            UpdateCheckResult.Failure(error)
        }
    }

    private fun latestReleaseFromApi(): LatestRelease {
        val release = requestJson("$API_BASE_URL/releases/latest")
        val tag = release.string("tag_name")
        return LatestRelease(
            tag = tag,
            sha = resolveTagCommit(tag),
            url = release.string("html_url")
        )
    }

    /**
     * GitHub REST has a small unauthenticated, IP-shared quota. The normal GitHub
     * release and tag pages remain publicly available when that quota is exhausted.
     */
    private fun latestReleaseFromWeb(): LatestRelease {
        val latestPage = requestPage("$WEB_BASE_URL/releases/latest")
        val tag = latestPage.url.substringAfter("/releases/tag/", missingDelimiterValue = "")
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
            ?: error("GitHub did not redirect to a release tag.")
        val tagPage = requestPage("$WEB_BASE_URL/tree/$tag")
        val sha = CURRENT_OID_PATTERN.find(tagPage.body)?.groupValues?.get(1)
            ?: error("GitHub tag page did not contain a commit hash.")
        return LatestRelease(tag, sha, "$WEB_BASE_URL/releases/tag/$tag")
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

    private fun requestPage(url: String): GitHubPage = client.newCall(
        Request.Builder()
            .url(url)
            .header("User-Agent", "NCMD-Android")
            .build()
    ).execute().use { response ->
        check(response.isSuccessful) { "GitHub page request failed with HTTP ${response.code}." }
        GitHubPage(
            url = response.request.url.toString(),
            body = response.body?.string().orEmpty()
        )
    }

    private fun Map<String, kotlinx.serialization.json.JsonElement>.string(name: String): String =
        get(name)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: error("GitHub response did not contain '$name'.")

    private data class LatestRelease(val tag: String, val sha: String, val url: String)

    private data class GitHubPage(val url: String, val body: String)

    private val CURRENT_OID_PATTERN = Regex("\\\"currentOid\\\":\\\"([0-9a-f]{40})\\\"")
}
