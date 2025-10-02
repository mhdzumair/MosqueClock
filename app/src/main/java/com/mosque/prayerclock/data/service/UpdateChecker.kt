package com.mosque.prayerclock.data.service

import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("prerelease") val prerelease: Boolean,
    @SerializedName("assets") val assets: List<GitHubReleaseAsset>,
)

interface GitHubApi {
    @GET("repos/mhdzumair/MosqueClock/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>
}

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

@Singleton
class UpdateChecker
    @Inject
    constructor() {
        private val githubApi: GitHubApi by lazy {
            Retrofit
                .Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApi::class.java)
        }

        // TODO: Replace with actual GitHub repo owner and name
        private val repoOwner = "mhdzumair" // Replace with your GitHub username
        private val repoName = "MosqueClock" // Replace with your repository name

        suspend fun checkForUpdates(currentVersion: String): UpdateInfo? {
            return try {
                Log.d("UpdateChecker", "Checking for updates. Current version: $currentVersion")
                val response = githubApi.getLatestRelease()

                if (response.isSuccessful && response.body() != null) {
                    val release = response.body()!!
                    val latestVersion = release.tagName.removePrefix("v")

                    Log.d("UpdateChecker", "Latest version from GitHub: $latestVersion")

                    val hasUpdate = compareVersions(latestVersion, currentVersion) > 0

                    // Find the APK asset (MosqueClock-v*.apk)
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    val downloadUrl = apkAsset?.browserDownloadUrl ?: release.htmlUrl

                    Log.d("UpdateChecker", "APK download URL: $downloadUrl")

                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = latestVersion,
                        currentVersion = currentVersion,
                        downloadUrl = downloadUrl,
                        releaseNotes = release.body,
                    )
                } else {
                    Log.e("UpdateChecker", "Failed to check for updates: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Error checking for updates", e)
                null
            }
        }

        /**
         * Compare two version strings
         * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
         */
        private fun compareVersions(
            v1: String,
            v2: String,
        ): Int {
            val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLength) {
                val part1 = parts1.getOrNull(i) ?: 0
                val part2 = parts2.getOrNull(i) ?: 0

                when {
                    part1 > part2 -> return 1
                    part1 < part2 -> return -1
                }
            }

            return 0
        }
    }

