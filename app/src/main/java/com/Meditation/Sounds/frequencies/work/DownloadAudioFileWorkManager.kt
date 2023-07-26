package com.Meditation.Sounds.frequencies.work


import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import com.Meditation.Sounds.frequencies.lemeor.getSaveDir
import com.Meditation.Sounds.frequencies.lemeor.getTempFile
import com.Meditation.Sounds.frequencies.lemeor.getTrackUrl
import com.Meditation.Sounds.frequencies.util.FileDownloader
import java.io.File

class DownLoadCourseAudioWorkManager(
    val context: Context,
    workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams) {


    override suspend fun doWork(): Result {
        val url = inputData.getString(URL)
        val albumName = inputData.getString(ALBUM_NAME) ?: ""
        val fileName = inputData.getString(FILE_NAME) ?: ""
        val trackId = inputData.getInt(TRACK_ID, 0)
        val tmpFile = File(getTempFile(context, fileName, albumName))
        return try {
            if (url != null) {
                val targetFile = File(getSaveDir(context, fileName, albumName))
                var percentage = 0L
                if (!targetFile.exists()) {
                    FileDownloader.download(url, tmpFile) { downloaded, total ->
                        if ((downloaded * 100 / total) - percentage >= 1 || (downloaded * 100 / total) == 100L) {
                            percentage = downloaded * 100 / total
                            setProgressAsync(
                                workDataOf(
                                    TRACK_ID to trackId,
                                    DOWNLOADED to downloaded,
                                    TOTAL to total,
                                )
                            )
                        }

                    }

                    tmpFile.renameTo(targetFile)
                    val outputData = workDataOf(
                        TRACK_ID to trackId
                    )
                    return Result.success(outputData)
                } else {
                    return Result.success(
                        workDataOf(
                            TRACK_ID to trackId
                        )
                    )
                }

            } else {
                tmpFile.delete()
                return Result.failure(
                    workDataOf(
                        TRACK_ID to trackId
                    )
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Result.failure(
                workDataOf(
                    ERROR to e,
                    TRACK_ID to trackId
                )
            )
        }

    }


    companion object {
        const val URL = "url"
        const val TRACK_ID = "track_id"
        const val ALBUM_NAME = "album_name"
        const val FILE_NAME = "file_name"
        const val DOWNLOADED = "downloaded"
        const val TOTAL = "total"
        const val ERROR = "error"
        const val TAG = "DownLoadCourseAudioWorkManager"

        fun getTag(trackId: Int) = "track_$trackId"

        fun start(context: Context, track: Track, album: Album?): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putInt(TRACK_ID, track.id)
                .putString(ALBUM_NAME, album?.audio_folder ?: "")
                .putString(FILE_NAME, track.filename)
                .putString(URL, getTrackUrl(album, track.filename))

            val oneTimeWorkRequest =
                OneTimeWorkRequestBuilder<DownLoadCourseAudioWorkManager>()
                    .setInputData(inputData.build())
                    .addTag(getTag(trackId = track.id))
                    .addTag(TAG)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()

            WorkManager
                .getInstance(context)
                .enqueue(oneTimeWorkRequest)

            return oneTimeWorkRequest
        }
    }
}
