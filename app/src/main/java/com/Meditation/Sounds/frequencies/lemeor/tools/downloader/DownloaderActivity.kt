package com.Meditation.Sounds.frequencies.lemeor.tools.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.Meditation.Sounds.frequencies.QApplication
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.api.RetrofitBuilder
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import com.Meditation.Sounds.frequencies.lemeor.data.remote.ApiHelper
import com.Meditation.Sounds.frequencies.lemeor.data.utils.ViewModelFactory
import com.Meditation.Sounds.frequencies.lemeor.tools.downloader.DownloadService.Companion.DOWNLOAD_FINISH
import kotlinx.android.synthetic.main.activity_downloader.*
import kotlinx.android.synthetic.main.activity_navigation.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DownloaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIEW_DOWNLOAD = "extra_view_download"
        private const val STORAGE_PERMISSION_CODE = 1001

        fun newIntent(
            context: Context?,
            isViewDownload: Boolean = false
        ): Intent {
            val intent = Intent(context, DownloaderActivity::class.java)
            intent.putExtra(EXTRA_VIEW_DOWNLOAD, isViewDownload)
            return intent
        }

        fun startDownload(
            activity: Activity,
            tracks: ArrayList<Track>,
        ) {
            if (tracks.isNotEmpty()) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    DownloadService.startService(context = activity, tracks)
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION_CODE
                    )
                }
            }
        }
    }

    private lateinit var mViewModel: DownloaderViewModel

    private val mDownloaderAdapter = DownloaderAdapter()
    private var bound = false
    private var downloadService: DownloadService? = null

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: Any?) {
        if (event?.javaClass == DownloadInfo::class.java) {
            val download = event as DownloadInfo

            updateUI(download)
        }

        if (event is DownloadErrorEvent && QApplication.isActivityDownloadStarted) {
            mDownloaderAdapter.notifyDataSetChanged()
            AlertDialog.Builder(this@DownloaderActivity)
                .setTitle(R.string.download_error)
                .setMessage(getString(R.string.download_error_message))
                .setPositiveButton(R.string.txt_ok,
                    DialogInterface.OnClickListener { p0, p1 -> this@DownloaderActivity.finish() })
                .show()
        }

        if (event == DOWNLOAD_FINISH) {
            finish()
        }

        if (event is DownloadTrackErrorEvent) {
            mDownloaderAdapter.notifyDataSetChanged()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as DownloadService.DownloadServiceBinder
            downloadService = binder.getService()
            bound = true
            mDownloaderAdapter.data = downloadService!!.tracks
            mDownloaderAdapter.fileProgressMap = downloadService!!.fileProgressMap
            mDownloaderAdapter.downloadErrorTracks = downloadService!!.downloadErrorTracks

            downloader_tv_quantity.text =
                getString(
                    R.string.downloader_quantity,
                    downloadService!!.getCompletedFileCount(),
                    mDownloaderAdapter.itemCount
                )
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloader)
        QApplication.isActivityDownloadStarted = true
        EventBus.getDefault().register(this)
        bindService(Intent(this, DownloadService::class.java), connection, BIND_AUTO_CREATE)

        downloader_tv_quantity.text = ""

        initUI()

//        checkStoragePermission()
    }

    private fun initUI() {
        mViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                ApiHelper(RetrofitBuilder(applicationContext).apiService),
                DataBase.getInstance(applicationContext)
            )
        ).get(DownloaderViewModel::class.java)

        downloader_btn_back.setOnClickListener { onBackPressed() }

        downloader_recycler_view.adapter = mDownloaderAdapter
        downloader_recycler_view.setHasFixedSize(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        QApplication.isActivityDownloadStarted = false
        EventBus.getDefault().unregister(this)
        if (bound) {
            unbindService(connection)
        }
    }

    private fun updateUI(download: DownloadInfo) {
        downloader_tv_quantity.text =
            getString(
                R.string.downloader_quantity,
                (download.completed).coerceAtMost(download.total),
                download.total
            )

        mDownloaderAdapter.data.firstOrNull {
            download.tag == it.id.toString()
        }?.let {
            mDownloaderAdapter.updateProgress(
                downloader_recycler_view,
                mDownloaderAdapter.data.indexOf(it),
                download.progress
            )
        }

    }

//    private fun checkStoragePermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(
//                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                STORAGE_PERMISSION_CODE
//            )
//        } else {
//            DownloadService.startService(this, mDownloaderAdapter.data)
//        }
//    }
//
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == STORAGE_PERMISSION_CODE || grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            DownloadService.startService(this, mDownloaderAdapter)
//        } else {
//            Toast.makeText(
//                applicationContext,
//                "Need permission to download tracks.",
//                Toast.LENGTH_SHORT
//            ).show()
//            checkStoragePermission()
//        }
//    }
}