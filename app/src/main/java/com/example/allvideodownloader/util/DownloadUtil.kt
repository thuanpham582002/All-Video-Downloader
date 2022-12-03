package com.example.allvideodownloader.util

import android.os.Build
import android.util.Log
import com.example.allvideodownloader.util.DatabaseUtil
import com.example.allvideodownloader.App
import com.example.allvideodownloader.App.Companion.audioDownloadDir
import com.example.allvideodownloader.App.Companion.context
import com.example.allvideodownloader.App.Companion.videoDownloadDir
import com.example.allvideodownloader.R
import com.example.allvideodownloader.database.DownloadedVideoInfo
import com.example.allvideodownloader.util.FileUtil.getConfigFile
import com.example.allvideodownloader.util.FileUtil.getCookiesFile
import com.example.allvideodownloader.util.FileUtil.getTempDir
import com.example.allvideodownloader.util.PreferenceUtil.ARIA2C
import com.example.allvideodownloader.util.PreferenceUtil.COOKIES
import com.example.allvideodownloader.util.PreferenceUtil.CROP_ARTWORK
import com.example.allvideodownloader.util.PreferenceUtil.CUSTOM_PATH
import com.example.allvideodownloader.util.PreferenceUtil.MAX_FILE_SIZE
import com.example.allvideodownloader.util.PreferenceUtil.PRIVATE_DIRECTORY
import com.example.allvideodownloader.util.PreferenceUtil.PRIVATE_MODE
import com.example.allvideodownloader.util.PreferenceUtil.RATE_LIMIT
import com.example.allvideodownloader.util.PreferenceUtil.SPONSORBLOCK
import com.example.allvideodownloader.util.PreferenceUtil.SUBDIRECTORY
import com.example.allvideodownloader.util.PreferenceUtil.SUBTITLE
import com.example.allvideodownloader.util.TextUtil.isNumberInRange
import com.example.allvideodownloader.util.TextUtil.toHttpsUrl
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

object DownloadUtil {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
    }

    class Result(val resultCode: ResultCode, val filePath: List<String>?) {
        companion object {
            fun failure(): Result {
                return Result(ResultCode.EXCEPTION, null)
            }

            fun success(filePaths: List<String>?): Result {
                return Result(ResultCode.SUCCESS, filePaths)
            }
        }
    }


    enum class ResultCode {
        SUCCESS, EXCEPTION
    }

    private const val TAG = "DownloadUtil"
    private const val OUTPUT_TEMPLATE = "%(title).100s [%(id)s].%(ext)s"
    private const val AUDIO_REGEX = "(mp3)|(aac)|(opus)|(m4a)"
    private const val CROP_ARTWORK_COMMAND =
        """--ppa "ffmpeg: -c:v png -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"""""


    data class PlaylistInfo(
        val url: String = "",
        val size: Int = 0,
        val title: String = ""
    )


    fun getPlaylistInfo(playlistURL: String): PlaylistResult {
        TextUtil.makeToastSuspend(context.getString(R.string.fetching_playlist_info))
        val request = YoutubeDLRequest(playlistURL)
        with(request) {
            addOption("--flat-playlist")
            addOption("-J")
            addOption("-R", "1")
            addOption("--socket-timeout", "5")
        }
        for (s in request.buildCommand()) Log.d(TAG, s)
        val resp: YoutubeDLResponse = YoutubeDL.getInstance().execute(request, null)
        val res = jsonFormat.decodeFromString<PlaylistResult>(resp.out)
        Log.d(TAG, "getPlaylistInfo: " + Json.encodeToString(res))
        if (res.type != "playlist") {
            return PlaylistResult(playlistCount = 1)
        }
        return res
    }


    private fun getVideoInfo(request: YoutubeDLRequest): VideoInfo {
        request.addOption("--dump-json")
        val videoInfo: VideoInfo
        try {
            Log.d(TAG, "getVideoInfo: Start")
            val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(request, null, null)
            Log.d(TAG, "getVideoInfo: Reading response")
            videoInfo = jsonFormat.decodeFromString(response.out)
            Log.d(TAG, videoInfo.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        return videoInfo
    }

    fun fetchVideoInfo(
        url: String,
        playlistItem: Int = 0,
        preferences: DownloadPreferences = DownloadPreferences()
    ): VideoInfo {
        val videoInfo: VideoInfo = getVideoInfo(YoutubeDLRequest(url).apply {
            preferences.run {
                if (extractAudio) {
                    addOption("-x")
                } else {
                    addOption("-S", toVideoFormatSorter())
                }
            }
            addOption("-R", "1")
            if (playlistItem != 0)
                addOption("--playlist-items", playlistItem)
            addOption("--socket-timeout", "5")
        })
        with(videoInfo) {
            if (title.isEmpty() or ext.isEmpty()) {
                throw Exception(context.getString(R.string.fetch_info_error_msg))
            }
        }
        return videoInfo
    }


    data class DownloadPreferences(
        val extractAudio: Boolean = PreferenceUtil.getValue(PreferenceUtil.EXTRACT_AUDIO),
        val createThumbnail: Boolean = PreferenceUtil.getValue(PreferenceUtil.THUMBNAIL),
        val downloadPlaylist: Boolean = PreferenceUtil.getValue(PreferenceUtil.PLAYLIST),
        val subdirectory: Boolean = PreferenceUtil.getValue(SUBDIRECTORY),
        val customPath: Boolean = PreferenceUtil.getValue(CUSTOM_PATH),
        val outputPathTemplate: String = PreferenceUtil.getOutputPathTemplate(),
        val embedSubtitle: Boolean = PreferenceUtil.getValue(SUBTITLE),
        val concurrentFragments: Float = PreferenceUtil.getConcurrentFragments(),
        val maxFileSize: String = PreferenceUtil.getString(MAX_FILE_SIZE, ""),
        val sponsorBlock: Boolean = PreferenceUtil.getValue(SPONSORBLOCK),
        val sponsorBlockCategory: String = PreferenceUtil.getSponsorBlockCategories(),
        val cookies: Boolean = PreferenceUtil.getValue(COOKIES),
        val cookiesContent: String = PreferenceUtil.getCookies(),
        val aria2c: Boolean = PreferenceUtil.getValue(ARIA2C),
        val audioFormat: Int = PreferenceUtil.getAudioFormat(),
        val videoFormat: Int = PreferenceUtil.getVideoFormat(),
        val videoResolution: Int = PreferenceUtil.getVideoResolution(),
        val privateMode: Boolean = PreferenceUtil.getValue(PRIVATE_MODE),
        val rateLimit: Boolean = PreferenceUtil.getValue(RATE_LIMIT),
        val maxDownloadRate: String = PreferenceUtil.getMaxDownloadRate(),
        val privateDirectory: Boolean = PreferenceUtil.getValue(PRIVATE_DIRECTORY),
        val cropArtwork: Boolean = PreferenceUtil.getValue(CROP_ARTWORK),
        val customCommandTemplate: String = ""
    )

    private fun YoutubeDLRequest.addOptionsForVideoDownloads(
        downloadPreferences: DownloadPreferences,
    ): YoutubeDLRequest {
        return this.apply {
            downloadPreferences.run {
                addOption("-S", toVideoFormatSorter())
                if (embedSubtitle) {
                    addOption("--remux-video", "mkv")
                    addOption("--embed-subs")
                    addOption("--sub-lang", "all,-live_chat")
                }
            }
        }
    }

    private fun DownloadPreferences.toVideoFormatSorter(): String {
        val sorter = StringBuilder()
        if (maxFileSize.isNumberInRange(1, 4096)) {
            sorter.append("size:${maxFileSize}M,")
        }
        when (videoFormat) {
            1 -> sorter.append("ext,")
            2 -> sorter.append("vcodec:vp9.2,")
            3 -> sorter.append("vcodec:av01,")
        }
        when (videoResolution) {
            1 -> sorter.append("res:2160")
            2 -> sorter.append("res:1440")
            3 -> sorter.append("res:1080")
            4 -> sorter.append("res:720")
            5 -> sorter.append("res:480")
            6 -> sorter.append("res:360")
            7 -> sorter.append("+size,+br,+res,+fps")
            else -> sorter.append("res")
        }
        return sorter.toString()
    }

    private fun YoutubeDLRequest.addOptionsForAudioDownloads(
        id: String,
        downloadPreferences: DownloadPreferences,
        playlistUrl: String
    ): YoutubeDLRequest {
        return this.apply {
            with(downloadPreferences) {
                addOption("-x")
                when (audioFormat) {
                    1 -> {
                        addOption("--audio-format", "mp3")
                        addOption("--audio-quality", "0")
                    }

                    2 -> {
                        addOption("--audio-format", "m4a")
                        addOption("--audio-quality", "0")
                    }
                }
                addOption("--embed-metadata")
                addOption("--embed-thumbnail")
                addOption("--convert-thumbnails", "png")

                if (cropArtwork) {
                    val configFile = context.getConfigFile(id)
                    FileUtil.writeContentToFile(CROP_ARTWORK_COMMAND, configFile)
                    addOption("--config", configFile.absolutePath)
                }

                if (playlistUrl.isNotEmpty()) {
                    addOption("--parse-metadata", "%(album,playlist,title)s:%(meta_album)s")
                    addOption(
                        "--parse-metadata",
                        "%(track_number,playlist_index)d:%(meta_track)s"
                    )
                } else {
                    addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
                }
            }
        }
    }

    private fun scanVideoIntoDownloadHistory(
        videoInfo: VideoInfo,
        downloadPath: String,
    ): List<String> {
        val filePaths = FileUtil.scanFileToMediaLibrary(
            title = videoInfo.id,
            downloadDir = downloadPath
        )
        for (path in filePaths) {
            DatabaseUtil.insertInfo(
                DownloadedVideoInfo(
                    id = 0,
                    videoTitle = videoInfo.title,
                    videoAuthor = videoInfo.uploader ?: videoInfo.channel.toString(),
                    videoUrl = videoInfo.webpageUrl ?: videoInfo.originalUrl ?: "null",
                    thumbnailUrl = videoInfo.thumbnail.toHttpsUrl(),
                    videoPath = path,
                    extractor = videoInfo.extractorKey
                )
            )
        }
        return filePaths
    }

    fun downloadVideo(
        videoInfo: VideoInfo? = null,
        playlistUrl: String = "",
        playlistItem: Int = 0,
        downloadPreferences: DownloadPreferences = DownloadPreferences(),
        progressCallback: ((Float, Long, String) -> Unit)?
    ): Result {
        if (videoInfo == null)
            return Result.failure()

        with(downloadPreferences) {
            // TODO: Move custom template configurations here
//            if (customCommandTemplate.isEmpty()) { }
            val url = playlistUrl.ifEmpty {
                videoInfo.webpageUrl ?: return Result.failure()
            }
            val request = YoutubeDLRequest(url)
            val pathBuilder = StringBuilder()

            with(request) {
                addOption("--no-mtime")
                if (cookies) {
                    val cookiesFile = context.getCookiesFile(videoInfo.id)
                    FileUtil.writeContentToFile(cookiesContent, cookiesFile)
                    addOption("--cookies", cookiesFile.absolutePath)
                }

                if (rateLimit && maxDownloadRate.isNumberInRange(1, 1000000)) {
                    addOption("-r", "${maxDownloadRate}K")
                }

                if (playlistItem != 0 && downloadPlaylist)
                    addOption("--playlist-items", playlistItem)

                if (aria2c) {
                    addOption("--downloader", "libaria2c.so")
                    addOption("--external-downloader-args", "aria2c:\"--summary-interval=1\"")
                } else if (concurrentFragments > 0f) {
                    addOption("--concurrent-fragments", (concurrentFragments * 16).roundToInt())
                }

                if (extractAudio or (videoInfo.vcodec == "none")) {
                    if (privateDirectory)
                        pathBuilder.append(App.getPrivateDownloadDirectory())
                    else
                        pathBuilder.append(audioDownloadDir)
                    addOptionsForAudioDownloads(
                        id = videoInfo.id,
                        downloadPreferences = downloadPreferences,
                        playlistUrl = playlistUrl
                    )
                } else {
                    if (privateDirectory)
                        pathBuilder.append(App.getPrivateDownloadDirectory())
                    else
                        pathBuilder.append(videoDownloadDir)
                    addOptionsForVideoDownloads(downloadPreferences)
                }
                if (sponsorBlock) {
                    addOption("--sponsorblock-remove", sponsorBlockCategory)
                }

                if (createThumbnail) {
                    addOption("--write-thumbnail")
                    addOption("--convert-thumbnails", "png")
                }
                if (!downloadPlaylist) {
                    addOption("--no-playlist")
                }
                if (subdirectory) {
                    pathBuilder.append("/${videoInfo.extractorKey}")
                }
                addOption("-P", pathBuilder.toString())
                if (Build.VERSION.SDK_INT > 23)
                    addOption("-P", "temp:" + context.getTempDir())
                if (customPath)
                    addOption("-o", outputPathTemplate + OUTPUT_TEMPLATE)
                else
                    addOption("-o", OUTPUT_TEMPLATE)

                for (s in request.buildCommand())
                    Log.d(TAG, s)
            }
            kotlin.runCatching {
                YoutubeDL.getInstance().execute(request, videoInfo.id, progressCallback)
            }.onFailure { th ->
                if (sponsorBlock && th.message?.contains("Unable to communicate with SponsorBlock API") == true) {
                    th.printStackTrace()
                } else throw th
            }
            if (privateMode) {
                return Result.success(null)
            }
            val filePaths = scanVideoIntoDownloadHistory(
                videoInfo = videoInfo,
                downloadPath = pathBuilder.toString(),
            )
            return Result.success(filePaths)
        }
    }


}