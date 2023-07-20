package com.Meditation.Sounds.frequencies.lemeor.tools.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.TrackDao
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import com.Meditation.Sounds.frequencies.lemeor.getSaveDir
import com.Meditation.Sounds.frequencies.lemeor.getTrackUrl
import com.Meditation.Sounds.frequencies.work.DownLoadCourseAudioWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import kotlin.collections.set


class DownloadService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "DownloadService"
        private const val EXTRA_TRACKS = "extra_tracks"
        const val DOWNLOAD_FINISH = "download_finish"

        fun startService(context: Context, tracks: ArrayList<Track>) {
            val startIntent = Intent(context, DownloadService::class.java)
            startIntent.putParcelableArrayListExtra(EXTRA_TRACKS, tracks)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, DownloadService::class.java)
            context.stopService(stopIntent)
        }
    }

    var tracks = ArrayList<Track>()
        private set

    var downloadErrorTracks = ArrayList<Int>()
        private set

    val fileProgressMap: HashMap<Int, Int> = HashMap()
    private var trackDao: TrackDao? = null

    // Binder given to clients.
    private val binder = DownloadServiceBinder()

    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(application).cancelAllWorkByTag(DownLoadCourseAudioWorkManager.TAG)
        trackDao = DataBase.getInstance(applicationContext).trackDao()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading files...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setNotificationSilent()
            .build()
        startForeground(1, notification)

        var tracks: List<Track> = intent?.getParcelableArrayListExtra(EXTRA_TRACKS)!!
        tracks = tracks.filter {
            !this.tracks.any { track ->
                track.id == it.id && it.albumId == track.albumId
            }
        }
        if (tracks.isNotEmpty()) {
            if (getCompletedFileCount() == this.tracks.size) {
                this.tracks.clear()
                fileProgressMap.clear()
            }
            this.tracks.addAll(tracks)
            val dao = DataBase.getInstance(applicationContext).albumDao()
            GlobalScope.launch {
                tracks.forEach { t ->
                    val album = dao.getAlbumById(t.albumId)
                    t.album = album
                }

                CoroutineScope(Dispatchers.Main).launch {
                    enqueueFiles(tracks)

                    EventBus.getDefault().post(
                        DownloadInfo(
                            "",
                            0,
                            getCompletedFileCount(),
                            tracks.size
                        )
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Download Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }


    override fun onDestroy() {
        downloadErrorTracks.clear()
        WorkManager.getInstance(application).cancelAllWorkByTag(DownLoadCourseAudioWorkManager.TAG)
        super.onDestroy()
    }

    private fun updateUIWithProgress() {
        val totalFiles: Int = fileProgressMap.size
        val completedFiles: Int = getCompletedFileCount()

        if (completedFiles == totalFiles) {
            Toast.makeText(
                applicationContext,
                getString(R.string.tst_download_successful),
                Toast.LENGTH_SHORT
            ).show()

            EventBus.getDefault().post(DOWNLOAD_FINISH)

            stopSelf()
        }
    }

    private fun checkDoneDownloaded() {
        val totalFiles: Int = fileProgressMap.size
        val completedFiles: Int = getCompletedFileCount()
        if ((completedFiles + downloadErrorTracks.size) == totalFiles && downloadErrorTracks.size > 0) {
            EventBus.getDefault().post(DownloadErrorEvent(downloadErrorTracks.size))
        }
    }

    fun getCompletedFileCount(): Int {
        return tracks.filter {
            File(getSaveDir(applicationContext, it.filename, it.album?.audio_folder ?: "")).exists()
        }.size
    }

    private fun enqueueFiles(tracks: List<Track>) {
        tracks.forEach {
            if (File(
                    getSaveDir(
                        applicationContext,
                        it.filename,
                        it.album?.audio_folder ?: ""
                    )
                ).exists()
            ) {
                fileProgressMap[it.id] = 100
            } else {
                val request = DownLoadCourseAudioWorkManager.start(applicationContext, it, it.album)
                observeWorker(request)
            }
        }
    }

    private fun observeWorker(request: OneTimeWorkRequest) {
        WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(request.id)
            .observe(this) { workInfo ->
                CoroutineScope(Dispatchers.Main).launch {
                    val wasSuccess = workInfo.state == WorkInfo.State.SUCCEEDED

                    if (wasSuccess) {
                        val trackId =
                            workInfo.outputData.getInt(DownLoadCourseAudioWorkManager.TRACK_ID, 0)
                        fileProgressMap[trackId] = 100
                        downloadErrorTracks.remove(trackId)
                        checkDoneDownloaded()
                        updateUIWithProgress()
                        EventBus.getDefault().post(
                            DownloadInfo(
                                "$trackId",
                                100,
                                getCompletedFileCount(),
                                tracks.size
                            )
                        )
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        val trackId =
                            workInfo.outputData.getInt(DownLoadCourseAudioWorkManager.TRACK_ID, 0)
                        if (!downloadErrorTracks.any { trackId == it }) {
                            tracks.firstOrNull { trackId == it.id }?.let {
                                downloadErrorTracks.add(it.id)
                                EventBus.getDefault()
                                    .post(
                                        DownloadTrackErrorEvent(
                                            trackId,
                                            getTrackUrl(it.album, it.filename)
                                        )
                                    )
                            }
                        }
                        checkDoneDownloaded()
                    } else if (workInfo != null) {
                        val total =
                            workInfo.progress.getLong(DownLoadCourseAudioWorkManager.TOTAL, 0L)
                        val downloaded =
                            workInfo.progress.getLong(DownLoadCourseAudioWorkManager.DOWNLOADED, 0L)
                        if (total > 0) {
                            val trackId =
                                workInfo.progress.getInt(DownLoadCourseAudioWorkManager.TRACK_ID, 0)
                            val progress = (downloaded * 100 / total).toInt();
                            fileProgressMap[trackId] = progress
                            EventBus.getDefault().post(
                                DownloadInfo(
                                    "$trackId",
                                    progress,
                                    getCompletedFileCount(),
                                    tracks.size
                                )
                            )
                        }
                    }
                }
            }
    }

    inner class DownloadServiceBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
}
