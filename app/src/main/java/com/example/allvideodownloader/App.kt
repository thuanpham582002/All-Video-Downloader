package com.example.allvideodownloader

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.allvideodownloader.util.FileUtil.createEmptyFile
import com.example.allvideodownloader.util.PreferenceUtil
import com.example.allvideodownloader.util.PreferenceUtil.AUDIO_DIRECTORY
import com.example.allvideodownloader.util.PreferenceUtil.VIDEO_DIRECTORY
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        configureRxJavaErrorHandler()
        Completable.fromAction { this.initLibraries() }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    // it worked
                }

                override fun onError(e: Throwable) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "failed to initialize youtubedl-android", e)
                    Toast.makeText(
                        applicationContext,
                        "initialization failed: " + e.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            if (e is UndeliverableException) {
                e.printStackTrace()
            } else {
                Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(
                    Thread.currentThread(),
                    e
                )
            }
            Log.e(TAG, "Undeliverable exception received, not sure what to do", e)
        }
    }

    @Throws(YoutubeDLException::class)
    private fun initLibraries() {
        YoutubeDL.getInstance().init(this)
        FFmpeg.getInstance().init(this)
        Aria2c.getInstance().init(this)
    }


    //
    companion object {
        private const val PRIVATE_DIRECTORY_SUFFIX = ".Seal"
        private const val TAG = "App"
        lateinit var clipboard: ClipboardManager
        lateinit var videoDownloadDir: String
        lateinit var audioDownloadDir: String
        var ytdlpVersion = ""
        lateinit var applicationScope: CoroutineScope
        lateinit var connectivityManager: ConnectivityManager

        fun getPrivateDownloadDirectory(): String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).resolve(
                PRIVATE_DIRECTORY_SUFFIX
            ).run {
                createEmptyFile(".nomedia")
                absolutePath
            }


        fun updateDownloadDir(path: String, isAudio: Boolean = false) {
            if (isAudio) {
                audioDownloadDir = path
                PreferenceUtil.updateString(AUDIO_DIRECTORY, path)
            } else {
                videoDownloadDir = path
                PreferenceUtil.updateString(VIDEO_DIRECTORY, path)
            }
        }

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}
