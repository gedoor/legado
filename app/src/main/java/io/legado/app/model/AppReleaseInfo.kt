package io.legado.app.model

import com.google.gson.annotations.SerializedName
import java.time.Instant

data class AppReleaseInfo(
    val appVariant: AppVariant,
    val createdAt: Long,
    val note: String,
    val name: String,
    val downloadUrl: String,
    val assetUrl: String
) {
    val versionName: String = name.split("_").getOrNull(2)?.removeSuffix(".apk") ?: ""
}

enum class AppVariant {
    OFFICIAL,
    BETA_RELEASEA,
    BETA_RELEASE,
    UNKNOWN
}

data class GithubRelease(
    val assets: List<Asset>,
    val body: String,
    @SerializedName("prerelease")
    val isPreLease: Boolean,
) {
    fun gitReleaseToAppReleaseInfo(): List<AppReleaseInfo> {
        return assets
            .filter { it.isValid }
            .map { it.assetToAppReleaseInfo(isPreLease, body) }
    }
}

data class Asset(
    @SerializedName("browser_download_url")
    val apkUrl: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("download_count")
    val downloadCount: Int,
    val id: Int,
    val name: String,
    val state: String,
    val url: String
) {
    val isValid: Boolean
        get() = (contentType == "application/vnd.android.package-archive") && (state == "uploaded")

    fun assetToAppReleaseInfo(preRelease: Boolean, note: String): AppReleaseInfo {
        val instant = Instant.parse(createdAt)
        val timestamp: Long = instant.toEpochMilli()

        return when {
            preRelease && name.contains("releaseA") ->
                AppReleaseInfo(
                    AppVariant.BETA_RELEASEA,
                    timestamp,
                    note,
                    name,
                    apkUrl,
                    url
                )

            preRelease && name.contains("release") ->
                AppReleaseInfo(
                    AppVariant.BETA_RELEASE,
                    timestamp,
                    note,
                    name,
                    apkUrl,
                    url
                )

            else ->
                AppReleaseInfo(
                    AppVariant.OFFICIAL,
                    timestamp,
                    note,
                    name,
                    apkUrl,
                    url
                )

        }
    }
}
