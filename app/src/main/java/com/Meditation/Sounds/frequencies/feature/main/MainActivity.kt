@file:Suppress("DEPRECATION")

package com.Meditation.Sounds.frequencies.feature.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.Meditation.Sounds.frequencies.BuildConfig
import com.Meditation.Sounds.frequencies.FileEncyptUtil
import com.Meditation.Sounds.frequencies.MusicService
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.api.ApiListener
import com.Meditation.Sounds.frequencies.api.models.GetAPKsNewVersionOutput
import com.Meditation.Sounds.frequencies.api.models.GetFlashSaleOutput
import com.Meditation.Sounds.frequencies.api.models.GetProfileOutput
import com.Meditation.Sounds.frequencies.db.QFDatabase
import com.Meditation.Sounds.frequencies.feature.album.AlbumsFragment
import com.Meditation.Sounds.frequencies.feature.base.BaseActivity
import com.Meditation.Sounds.frequencies.feature.download.DownloadMusicManager
import com.Meditation.Sounds.frequencies.feature.options.OptionFragment
import com.Meditation.Sounds.frequencies.feature.playlist.PlaylistGroupFragment
import com.Meditation.Sounds.frequencies.feature.profile.ProfileFragment
import com.Meditation.Sounds.frequencies.feature.video.VideoFragment
import com.Meditation.Sounds.frequencies.models.Album
import com.Meditation.Sounds.frequencies.models.Playlist
import com.Meditation.Sounds.frequencies.models.PlaylistItem
import com.Meditation.Sounds.frequencies.models.PlaylistItemSongAndSong
import com.Meditation.Sounds.frequencies.tasks.*
import com.Meditation.Sounds.frequencies.utils.*
import com.Meditation.Sounds.frequencies.views.CustomSeekBar
import com.Meditation.Sounds.frequencies.views.DisclaimerDialog
import com.Meditation.Sounds.frequencies.views.EncryptingProgressDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity(), MusicService.Callback, ApiListener<Any>,
    SyncMusicTask.IOnUpdateProgressListener, View.OnTouchListener {

    companion object {
        @JvmField
        var ourMainRunning = false
        val CACHE_APK_FOLDER = File(FilesUtils.getSdcardStore(), Constants.DEFAULT_APKS_FOLDER)
    }

    private var mLocalApkPath: String? = null
    var musicService: MusicService? = null
        private set
    var isGetNewAlbumFromServer = true
    private var isBounded = false

    private lateinit var mAudioManager: AudioManager

    private var mViewGroupCurrent: View? = null

    private var mSettingsContentObserver = SettingsContentObserver(Handler())

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MusicService.MusicBinder
            musicService = binder.service
            isBounded = true
            musicService?.addCallback(this@MainActivity)
            showPlayerController(musicService!!.getCurrentItems() != null && musicService!!.getCurrentItems()!!.size > 0)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBounded = false
        }
    }
    private var mDownloadMusicManager: DownloadMusicManager? = null
    private var _xDelta = 0F
    private var _yDelta = 0F
    private var mDisclaimerDialog: DisclaimerDialog? = null
    private var mCurrentFragment: Fragment? = null
    private lateinit var mLoadingDialog: ProgressDialog
    private var needToDownloadData = false
    private var isDownloadingData = false
    private lateinit var mViewModel: MainActivityViewModel

    private var downloadManager: DownloadManager? = null
    private var refid: Long = 0
    private var Download_Uri: Uri? = null
    private var mTotalMp3FileCount: Int = 0
    private var mEncryptingProgressDialog: EncryptingProgressDialog? = null
    private var mUpgrateMp3FileDialog: AlertDialog? = null
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (refid == referenceId) {
                val mBuilder = NotificationCompat.Builder(this@MainActivity)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("[New APK] " + this@MainActivity.getString(R.string.app_name))
                    .setContentText("Download completed")
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(455, mBuilder.build())
                autoInstallNewAPK()
            }
        }
    }
    private var isCheckLogin = false

    var mIsFromBroadcastReceiverPurchase = false
    private val broadcastReceiverPurchase = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)
                || SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_ADVANCED)
                || SharedPreferenceHelper.getInstance()
                    .getBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE)
                || SharedPreferenceHelper.getInstance()
                    .getBool(Constants.KEY_PURCHASED_HIGH_QUANTUM)
            ) {
                if (mDisclaimerDialog == null || !mDisclaimerDialog!!.isShowing) {
                    if (mEncryptingProgressDialog == null || !mEncryptingProgressDialog!!.isShowing) {
                        if (mUpgrateMp3FileDialog == null || !mUpgrateMp3FileDialog!!.isShowing) {
                            isGetNewAlbumFromServer = true
                            mIsFromBroadcastReceiverPurchase = true
                            checkPermissionAndSyncData()
                        }
                    }
                }
            }
        }
    }

    private val broadCastReceiverPlaylistController = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            if (intent != null && intent.getBooleanExtra(
                    Constants.EXTRAX_HIDDEN_CONTROLLER,
                    false
                )
            ) {
                showPlayerController(false)
            }
        }
    }

    private val broadCastReDownloadMp3Files = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            if (mUpgrateMp3FileDialog == null || !mUpgrateMp3FileDialog!!.isShowing) {
                val upgrateMp3FileDialog = AlertDialog.Builder(this@MainActivity)
                    .setMessage(R.string.txt_warning_frequencies_frequencies)
                    .setCancelable(false)
                    .setPositiveButton(R.string.txt_ok) { _, _ ->
                        DeleteAllTrackSync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                mUpgrateMp3FileDialog = upgrateMp3FileDialog.show()
            }
        }
    }

    private var mCountDownTimer: CountDownTimer? = null

    private fun setCountdownTimer(totalTime: Long) {
        if (mCountDownTimer != null) {
            mCountDownTimer!!.cancel()
        }
        mCountDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(l: Long) {
                val totalSeconds = (l / 1000).toInt()
                val days = totalSeconds / (24 * 3600)
                var remainder = totalSeconds - (days * 24 * 3600)
                val hours = remainder / 3600
                remainder -= (hours * 3600)
                val mins = remainder / 60
                remainder -= mins * 60
                val secs = remainder
                val hour: String = if (hours > 9) "" + hours else "0$hours"
                val min: String = if (mins > 9) "" + mins else "0$mins"
                val second: String = if (secs > 9) "" + secs else "0$secs"
//                if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)
//                    && SharedPreferenceHelper.getInstance()
//                        .getBool(Constants.KEY_PURCHASED_ADVANCED)
//                    && SharedPreferenceHelper.getInstance()
//                        .getBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE)
//                    && SharedPreferenceHelper.getInstance()
//                        .getBool(Constants.KEY_PURCHASED_HIGH_QUANTUM)
//                ) {
////                    viewCountdown.visibility = View.GONE
//                } else {
////                    viewCountdown.visibility = View.VISIBLE
//                }
//                mTvDurationCountDown.text = "$hour:$min:$second"
                mTvTimeCountDownHour.text = hour
                mTvTimeCountDownMin.text = min
                mTvTimeCountDownSecond.text = second
            }

            override fun onFinish() {
//                viewCountdown.visibility = View.GONE
//                initComponents()
            }
        }
        mCountDownTimer!!.start()
    }

    private var mCallbackManager: CallbackManager? = null

    override fun updateTitlePlayer(title: String, albumId: Long) {//, songId: Long
        item_track_name.text = title
        val albumDao = QFDatabase.getDatabase(this@MainActivity).albumDAO()
        val data = albumDao.getAll()
        if (data.isNotEmpty()) {
            for (item in data) {
                if (albumId == item.id) {
                    val imageAlbum = item.albumArt
                    if (!TextUtils.isEmpty(imageAlbum)) {
                        if (imageAlbum!!.startsWith("http")) {
                            Glide.with(this)
                                .applyDefaultRequestOptions(RequestOptions().placeholder(R.drawable.ic_album_placeholder))
                                .load(imageAlbum)
                                .into(imgAlbum!!)
                        } else {
                            Glide.with(this)
                                .applyDefaultRequestOptions(RequestOptions().placeholder(R.drawable.ic_album_placeholder))
                                .load(imageAlbum)
                                .into(imgAlbum!!)
                        }
                    } else {
                        imgAlbum?.setBackgroundResource(R.drawable.ic_album_default_small)
                    }
                    break
                }
            }
        }
    }

    override fun initLayout(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.createKeyHash(this)
        registerReceiver(
            broadcastReceiverPurchase,
            IntentFilter(Constants.BROADCAST_ACTION_PURCHASED)
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels

        if (SharedPreferenceHelper.getInstance()
                .getLong(Constants.ETRAX_FIRST_INSTALLER_APP_TIME) == 0L
        ) {
            val calendar = Calendar.getInstance()
            SharedPreferenceHelper.getInstance()
                .setLong(Constants.ETRAX_FIRST_INSTALLER_APP_TIME, calendar.timeInMillis)
            val dateFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
            Log.d("MENDATE", "0 -" + dateFormat.format(calendar.time))
        }
        loadCountdownTime()

        GetFlashSaleTask(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @SuppressLint("HardwareIds")
    override fun initComponents() {
        //Save Device ID
        var deviceId = SharedPreferenceHelper.getInstance()[Constants.KEY_DEVICE_ID]
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            if (deviceId == null || deviceId.isEmpty()) {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(this@MainActivity)
                deviceId = adInfo.id
            }
            if (deviceId != null) {
                if (deviceId.length >= 16) {
                    deviceId = deviceId.substring(0, 16)
                } else {
                    for (i in deviceId.length + 1..16) {
                        deviceId += "1"
                    }
                }
            } else {
                deviceId = "testKey:12345678"
            }
            SharedPreferenceHelper.getInstance().set(
                Constants.KEY_DEVICE_ID,
                deviceId
            )
        }

        mCallbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance()
            .registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d("FACEBOOK", "Login onSuccess")
                }

                override fun onCancel() {
                    Log.d("FACEBOOK", "Login onCancel")
                }

                override fun onError(error: FacebookException) {
                    Log.d("FACEBOOK", "Login onError")
                }
            })

        mLoadingDialog = ProgressDialog(this)
        mLoadingDialog.setMessage(getString(R.string.msg_loading))
        mLoadingDialog.setCancelable(false)
        mViewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            mSettingsContentObserver
        )

        mViewGroupCurrent = viewGroupAlbums
        if (mViewGroupCurrent != null) {
            mViewGroupCurrent?.isSelected = true
        }
//        mViewGroupCurrent?.let {
//            it.isSelected = true
//        }
        mCurrentFragment = AlbumsFragment()
        setNewPage(mCurrentFragment as AlbumsFragment)

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!Utils.isTablet(this)) {
                loadData()
            }
        } else {
            if (Utils.isTablet(this)) {
                loadData()
            }
        }

        registerReceiver(
            broadCastReceiverPlaylistController,
            IntentFilter(Constants.BROADCAST_PLAY_PLAYLIST)
        )
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        registerReceiver(
            broadCastReDownloadMp3Files,
            IntentFilter(Constants.ACTION_RE_DOWNLOAD_MP3)
        )

//        if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)
//            && SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_ADVANCED)
//            && SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE)
//            && SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_HIGH_QUANTUM)
//        ) {
////            viewCountdown.visibility = View.GONE
//        } else {
////            viewCountdown.visibility = View.VISIBLE
//        }
    }

    private fun loadData() {
        val currentVersion = SharedPreferenceHelper.getInstance().get(Constants.PREF_VERSION_APP)
        if (!SharedPreferenceHelper.getInstance().isShowDisclaimer || currentVersion == null
            || (!currentVersion.equals(
                BuildConfig.VERSION_NAME,
                ignoreCase = true
            ))
        ) {
            if (currentVersion == null || (!currentVersion.equals(
                    BuildConfig.VERSION_NAME,
                    ignoreCase = true
                ))
            ) {
                SharedPreferenceHelper.getInstance().isShowDisclaimer = false
            }
            SharedPreferenceHelper.getInstance()
                .set(Constants.PREF_VERSION_APP, BuildConfig.VERSION_NAME)
            mDisclaimerDialog = DisclaimerDialog(
                this@MainActivity,
                true,
                object : DisclaimerDialog.IOnSubmitListener {
                    override fun submit(isCheck: Boolean) {
                        if (isCheck) {
                            SharedPreferenceHelper.getInstance().isShowDisclaimer = true
                        }
                        checkPermissionAndSyncData()
                    }
                })
            mDisclaimerDialog!!.show()
        } else {
            checkPermissionAndSyncData()
        }
    }

    private fun checkPermissionAndSyncData() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
//            if (File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER).exists()) {
//                FilesUtils.deleteRecursive(File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER))
//            }
//            if (File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ADVANCED_FOLDER).exists()) {
//                FilesUtils.deleteRecursive(File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ADVANCED_FOLDER))
//            }
//            if (File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ABUNDANCE_FOLDER).exists()) {
//                FilesUtils.deleteRecursive(File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ABUNDANCE_FOLDER))
//            }
//            if (File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_HIGHER_QUANTUM_FOLDER).exists()) {
//                FilesUtils.deleteRecursive(File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_HIGHER_QUANTUM_FOLDER))
//            }
//
//            val database = QFDatabase.getDatabase(this)
//            val albums = database.albumDAO().getAll()
//            val songs = database.songDAO().getByAlbumId(albums.get(6).id)
//            if(songs.size>0){
//                val song = songs.get(0)
//                val ext = StringsUtils.getFileExtension(song.path)
//                var pathWithoutExtension = StringsUtils.getFileNameWithoutExtension(song.path)
//                var fromFile = File(song.path)
//                var toFile = File(pathWithoutExtension + "123." + ext)
//                fromFile.renameTo(toFile)
//                database.songDAO().updateEncryptPathFromId(song.id, toFile.path)
//            }

            checkingNewData()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                10
            )
        }
    }

    private fun checkingNewData() {
        val dataFolder = File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER)
        /*val dataFolderAdvanced = File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ADVANCED_FOLDER)
        val dataFolderHigherAbundance = File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ABUNDANCE_FOLDER)
        val dataFolderHigherQuantum = File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_HIGHER_QUANTUM_FOLDER)


                ||dataFolderAdvanced.exists() && dataFolderAdvanced.listFiles().isNotEmpty()
                ||dataFolderHigherAbundance.exists() && dataFolderHigherAbundance.listFiles().isNotEmpty()
                ||dataFolderHigherQuantum.exists() && dataFolderHigherQuantum.listFiles().isNotEmpty()*/
        if (dataFolder.exists() && dataFolder.listFiles()?.isNotEmpty() == true) {
            requestData()
        } else {
            val upgrateMp3FileDialog = AlertDialog.Builder(this)
                .setMessage(R.string.txt_warning_frequencies_frequencies)
                .setCancelable(false)
                .setPositiveButton(R.string.txt_ok) { _, _ ->
                    DeleteAllTrackSync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            mUpgrateMp3FileDialog = upgrateMp3FileDialog.show()
        }
    }

    @SuppressLint("StaticFieldLeak")
    internal inner class DeleteAllTrackSync : AsyncTask<Void, Void, Void>() {

        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()
            setMessageLoadingDialog(getString(R.string.txt_clear_data))
            showLoading(true)
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg p0: Void?): Void? {
            //Delete old folder
            for (item in Constants.DEFAULT_DATA_FOLDER_OLDS) {
                if (File(FilesUtils.getSdcardStore(), item).exists()) {
                    FilesUtils.deleteRecursive(File(FilesUtils.getSdcardStore(), item))
                }
            }

            //Delete folder current
            if (File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER).exists()) {
                FilesUtils.deleteRecursive(
                    File(
                        FilesUtils.getSdcardStore(),
                        Constants.DEFAULT_DATA_FOLDER
                    )
                )
            }
            if (File(
                    FilesUtils.getSdcardStore(),
                    Constants.DEFAULT_DATA_ADVANCED_FOLDER
                ).exists()
            ) {
                FilesUtils.deleteRecursive(
                    File(
                        FilesUtils.getSdcardStore(),
                        Constants.DEFAULT_DATA_ADVANCED_FOLDER
                    )
                )
            }
            if (File(
                    FilesUtils.getSdcardStore(),
                    Constants.DEFAULT_DATA_ABUNDANCE_FOLDER
                ).exists()
            ) {
                FilesUtils.deleteRecursive(
                    File(
                        FilesUtils.getSdcardStore(),
                        Constants.DEFAULT_DATA_ABUNDANCE_FOLDER
                    )
                )
            }
            if (File(
                    FilesUtils.getSdcardStore(),
                    Constants.DEFAULT_DATA_HIGHER_QUANTUM_FOLDER
                ).exists()
            ) {
                FilesUtils.deleteRecursive(
                    File(
                        FilesUtils.getSdcardStore(),
                        Constants.DEFAULT_DATA_HIGHER_QUANTUM_FOLDER
                    )
                )
            }
            return null
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            setMessageLoadingDialog(getString(R.string.txt_waiting))
            showLoading(false)
            requestData()
        }
    }

    private fun requestData() {
        mEncryptingProgressDialog = EncryptingProgressDialog(this@MainActivity)
        var isLoadData = false
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Utils.isTablet(this)) {
                isLoadData = true
            }
        } else {
            if (!Utils.isTablet(this)) {
                isLoadData = true
            }
        }
        if (isLoadData) {
            if (!isCheckLogin) {
                if (Utils.isConnectedToNetwork(this@MainActivity)) {
                    isCheckLogin = true
                    val file = File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER)
                    val fileAdvanced =
                        File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ADVANCED_FOLDER)
                    val fileHigherAbundance =
                        File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_ABUNDANCE_FOLDER)
                    val fileHigherQuantum = File(
                        FilesUtils.getSdcardStore(),
                        Constants.DEFAULT_DATA_HIGHER_QUANTUM_FOLDER
                    )
                    needToDownloadData = !file.exists() || file.listFiles()?.isEmpty() ?: true
                            || !fileAdvanced.exists() || fileAdvanced.listFiles()?.isEmpty() ?: true
                            || !fileHigherAbundance.exists() || fileHigherAbundance.listFiles()
                        ?.isEmpty() ?: true
                            || !fileHigherQuantum.exists() || fileHigherQuantum.listFiles()
                        ?.isEmpty() ?: true
                    val userJson = SharedPreferenceHelper.getInstance().get(Constants.PREF_PROFILE)
                    if (userJson != null) {
                        GetProfileTask(this@MainActivity, this@MainActivity).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR
                        )
                    } else {
                        SyncMusicTask(
                            this@MainActivity,
                            this@MainActivity,
                            this@MainActivity
                        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                } else {
                    SyncMusicTask(
                        this@MainActivity,
                        this@MainActivity,
                        this@MainActivity
                    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            } else {
                SyncMusicTask(
                    this@MainActivity,
                    this@MainActivity,
                    this@MainActivity
                ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mCallbackManager!!.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        ourMainRunning = true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Utils.isTablet(this)) {
                loadData()
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!Utils.isTablet(this)) {
                loadData()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mConnection)
        isBounded = false
        unregisterReceiver(onDownloadComplete)
        unregisterReceiver(broadCastReceiverPlaylistController)
        unregisterReceiver(broadcastReceiverPurchase)
        unregisterReceiver(broadCastReDownloadMp3Files)
        ourMainRunning = false
    }

    fun loadCountdownTime() {
        val flashSaleRemainTimeGloble = Utils.getFlashSaleRemainTime()
        if (flashSaleRemainTimeGloble > 0) {
            setCountdownTimer(flashSaleRemainTimeGloble)
        } else {
//            viewCountdown.visibility = View.GONE
        }
    }

    private fun random(): Int {
        val random = Random()
        return random.nextInt(3) + 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            checkPermissionAndSyncData()
        }
    }

    private var mDownloadPressCountDown: CountDownTimer? = object : CountDownTimer(350, 350) {

        override fun onTick(p0: Long) {

        }

        override fun onFinish() {
            if (mDownloadMusicManager != null) {
                mDownloadMusicManager?.showDialog()
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (view == viewGroupDownload) {
            var newX: Float
            var newY: Float
            when (event?.action?.and(MotionEvent.ACTION_MASK)) {
                MotionEvent.ACTION_DOWN -> {
                    if (mDownloadPressCountDown != null) {
                        mDownloadPressCountDown?.cancel()
                    }
                    mDownloadPressCountDown?.start()
                    _xDelta = view?.x?.minus(event.rawX) ?: 0F
                    _yDelta = view?.y?.minus(event.rawY) ?: 0F
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mDownloadPressCountDown != null) {
                        mDownloadPressCountDown?.cancel()
                    }
                    newX = event.rawX + _xDelta
                    newY = event.rawY + _yDelta
                    if (newX <= 0) {
                        newX = 0F
                    }
                    if (newX >= screenWidth - view?.width!!) {
                        newX = (screenWidth - view.width).toFloat()
                    }
                    if (newY <= 0) {
                        newY = 0F
                    }
                    if (newY >= screenHeight - view.height) {
                        newY = (screenHeight - view.height).toFloat()
                    }
                    view.animate()?.x(newX)
                        ?.y(newY)
                        ?.setDuration(0)
                        ?.start()
                }
            }
            drawer_layout.invalidate()
        }
        return true
    }

    override fun addListener() {

        viewGroupDownload.setOnTouchListener(this)

        viewCountdown.setOnClickListener {
            //            if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)) {
//                showDialogSubscriptionFS(true)
//            } else if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_ADVANCED)) {
//                showDialogSubscriptionFS(false)
//            } else {
//                if (mCurrentFragment != null && mCurrentFragment is AlbumsFragment) {
//                    if ((mCurrentFragment as AlbumsFragment).getTabAlbum() == 0) {
//                        /*tab album basic*/
//                        showDialogSubscriptionFS(false)
//                    } else if ((mCurrentFragment as AlbumsFragment).getTabAlbum() == 1) {
//                        showDialogSubscriptionFS(true)
//                    }
//                } else {
//                    if (random() == 1) {
//                        showDialogSubscriptionFS(false)
//                    } else {
//                        showDialogSubscriptionFS(true)
//                    }
//                }
//            }
            if (mCurrentFragment != null && mCurrentFragment is AlbumsFragment) {
                when {
                    (mCurrentFragment as AlbumsFragment).getTabAlbum() == 0 -> showDialogSubscriptionFS(
                        0
                    )/*tab album basic*/
                    (mCurrentFragment as AlbumsFragment).getTabAlbum() == 1 -> showDialogSubscriptionFS(
                        1
                    )

                    (mCurrentFragment as AlbumsFragment).getTabAlbum() == 2 -> showDialogSubscriptionFS(
                        2
                    )
                }
            } else {
                when {
                    random() == 1 -> showDialogSubscriptionFS(0)
                    random() == 2 -> showDialogSubscriptionFS(1)
                    else -> showDialogSubscriptionFS(2)
                }
            }
        }

        sbDurationMain.setOnProgressChangedListener(object :
            CustomSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(progress: Int, max: Int, isFromUser: Boolean) {
                if (isFromUser && musicService != null) {
                    val playlistPlaying = musicService!!.getCurrentItems()
                    if (playlistPlaying != null && playlistPlaying.size > 0) {
                        musicService!!.isPlaying = true
                        onPlaylistItemStart(PlaylistItem())
                    }
                    musicService!!.changeDurationFromMain(progress)
                }
            }

            override fun onProgressDone() {

            }
        })
        viewGroupAlbums!!.setOnClickListener {
            if (mViewGroupCurrent != null) {
                mViewGroupCurrent!!.isSelected = false
            }
            mViewGroupCurrent = viewGroupAlbums
            mViewGroupCurrent!!.isSelected = true
            mCurrentFragment = AlbumsFragment()
            sendBroadcast(Intent("BROADCAST_DISMISS_POPUP_PROGRAM"))
            setNewPage(mCurrentFragment as AlbumsFragment)
//            LoginManager.getInstance().logInWithReadPermissions(this@MainActivity, Arrays.asList("public_profile", "user_friends"));
        }
        viewGroupPlaylists!!.setOnClickListener {
            if (mViewGroupCurrent != null) {
                mViewGroupCurrent!!.isSelected = false
            }
            mViewGroupCurrent = viewGroupPlaylists
            mViewGroupCurrent!!.isSelected = true
            mCurrentFragment = PlaylistGroupFragment()
            sendBroadcast(Intent("BROADCAST_DISMISS_POPUP_PROGRAM"))
            setNewPage(mCurrentFragment as PlaylistGroupFragment)
        }
        viewGroupVideos.setOnClickListener {
            if (mViewGroupCurrent != null) {
                mViewGroupCurrent!!.isSelected = false
            }
            mViewGroupCurrent = viewGroupVideos
            mViewGroupCurrent?.isSelected = true
            mCurrentFragment = VideoFragment()
            sendBroadcast(Intent("BROADCAST_DISMISS_POPUP_PROGRAM"))
            setNewPage(mCurrentFragment as VideoFragment)
        }
        viewGroupProfile.setOnClickListener {
            if (mViewGroupCurrent != null) {
                mViewGroupCurrent!!.isSelected = false
            }
            mViewGroupCurrent = viewGroupProfile
            mViewGroupCurrent?.isSelected = true
            mCurrentFragment = ProfileFragment()
            sendBroadcast(Intent("BROADCAST_DISMISS_POPUP_PROGRAM"))
            setNewPage(mCurrentFragment as ProfileFragment)
        }
        viewGroupOption.setOnClickListener {
            if (mViewGroupCurrent != null) {
                mViewGroupCurrent!!.isSelected = false
            }
            mViewGroupCurrent = viewGroupOption
            mViewGroupCurrent?.isSelected = true
            mCurrentFragment = OptionFragment.newInstance()
            sendBroadcast(Intent("BROADCAST_DISMISS_POPUP_PROGRAM"))
            setNewPage(mCurrentFragment as OptionFragment)
        }
        btnPlay.setOnClickListener {
            val playlistPlaying = musicService!!.getCurrentItems()
            if (playlistPlaying != null && playlistPlaying.size > 0) {
                if (musicService!!.isPlaying) {
                    musicService!!.pausePlayer()
                    btnPlay.isSelected = false
                } else {
                    musicService?.resume()
                    btnPlay.isSelected = true
                }
            }
        }
        btnNext.setOnClickListener {
            musicService?.playNext(false)
        }
        btnPrevious.setOnClickListener {
            musicService?.playPrev()
        }

        btnReplay.setOnClickListener {
            if (musicService?.repeatType == MusicService.RepeatType.NONE) {
                musicService?.repeatType = MusicService.RepeatType.ALL
                btnReplay.setImageResource(R.drawable.ic_replay_selected)
            } else if (musicService?.repeatType == MusicService.RepeatType.ALL) {
                musicService?.repeatType = MusicService.RepeatType.ONE
                btnReplay.setImageResource(R.drawable.ic_replay_1)
            } else if (musicService?.repeatType == MusicService.RepeatType.ONE) {
                musicService?.repeatType = MusicService.RepeatType.NONE
                btnReplay.setImageResource(R.drawable.ic_replay)
            }
        }
        btnShuffle.setOnClickListener {
            musicService?.shuffle = !(musicService?.shuffle!!)
            btnShuffle.isSelected = musicService?.shuffle!!
        }
        if (Utils.isTablet(this)) {
            btnProfile.setOnClickListener {
                FilesUtils.showComingSoon(this@MainActivity)
            }
        }
    }

    fun showPlayerController(isShow: Boolean) {
        if (isShow) {
            viewController.visibility = View.VISIBLE
            viewTitle?.visibility = View.VISIBLE
        } else {
            viewController.visibility = View.INVISIBLE
            viewTitle?.visibility = View.GONE
        }
        btnPlay.isSelected = musicService!!.isPlaying
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
    }

    inner class SettingsContentObserver(handler: Handler) : ContentObserver(handler) {

        override fun deliverSelfNotifications(): Boolean {
            return super.deliverSelfNotifications()
        }

        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
//            if (!isUpdatingVolume) sbVolume.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
//                    mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        }
    }

    override fun onPlaylistItemStart(playlistItem: PlaylistItem) {
        btnPlay.isSelected = true
    }

    override fun onPlaylistItemPlayed(playlistItem: PlaylistItem) {
        btnPlay.isSelected = true
    }

    override fun onPausePlayback() {
        btnPlay.isSelected = false
    }

    override fun onStopPlayback() {
        btnPlay.isSelected = false
    }

    fun getCurrentPlaylistItem(): PlaylistItem? {
        if (musicService != null) {
            return musicService!!.getCurrentPlaylistItem()
        }
        return null
    }

    fun getPlayingAction(): Boolean {
        return btnPlay.isSelected
    }

    override fun updateDurationPlayer(
        duration: Int,
        currentDuration: Int,
        song: PlaylistItemSongAndSong
    ) {
        sbDurationMain.post(object : Runnable {
            override fun run() {
                sbDurationMain.setProgress(currentDuration, if (duration != 0) duration else 1)
            }

        })
        tvCurrentDurationMain.text =
            StringsUtils.toString(if (currentDuration > duration) duration.toLong() else currentDuration.toLong())
        tvDurationMain.text = StringsUtils.toString(duration.toLong() - currentDuration.toLong())

//        val title = song.song.title
//        val albumId = song.song.albumId
//        tvSongTitle.text = title
//        val albumDao = QFDatabase.getDatabase(this@MainActivity).albumDAO()
//        val data = albumDao.getAll()
//        if (data.isNotEmpty()) {
//            for (item in data) {
//                if (albumId == item.id) {
//                    val imageAlbum = item.albumArt
//                    Glide.with(this@MainActivity)
//                            .applyDefaultRequestOptions(RequestOptions().placeholder(R.drawable.ic_album_default_2))
//                            .load(imageAlbum).into(imgAlbum)
//                    break
//                }
//            }
//        }
    }

    override fun onConnectionOpen(task: BaseTask<*>?) {
        mLoadingDialog.setMessage(getString(R.string.msg_loading))
        if (needToDownloadData && task is GetNewAlbumsTask) {
            mLoadingDialog.show()
        } else if (task is SyncMusicTask) {
            mTotalMp3FileCount = FileEncyptUtil.countMp3Files()
            if (mTotalMp3FileCount > 0) {
                if (mEncryptingProgressDialog != null) {
                    mEncryptingProgressDialog?.show()
                }
            }
        }

    }

    override fun onProgress(countEncypted: Int) {
        if (mEncryptingProgressDialog != null && mTotalMp3FileCount > 0) {
            mEncryptingProgressDialog?.setProgressPercent(100 * countEncypted / mTotalMp3FileCount)
        }
    }

    override fun onConnectionError(task: BaseTask<*>?, exception: Exception?) {
        if (task is GetAPKNewVersionTask) {
            return
        }
        if (mLoadingDialog.isShowing)
            mLoadingDialog.dismiss()

        if (task is GetNewAlbumsTask) {
            if (needToDownloadData) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.msg_download_data)
                    .setPositiveButton(R.string.txt_try_again) { _, _ ->
                        GetNewAlbumsTask(this, this).execute()
                    }
                    .setNegativeButton(R.string.txt_cancel) { _, _ ->
                        getDefaultPlaylistFromServer()
                    }
                    .show()
            }
        }
        if (task is SyncMusicTask) {
            if (mEncryptingProgressDialog != null && mEncryptingProgressDialog?.isShowing!!) {
                mEncryptingProgressDialog?.dismiss()
            }
            if (isGetNewAlbumFromServer) {
                isGetNewAlbumFromServer = false
                GetNewAlbumsTask(this, this).execute()
            } else {
                UpdateDurationOfAllPlaylistTask(
                    this@MainActivity.application,
                    null
                ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
        if (task is GetDefaultPlaylistTask || task is GetProfileTask) {
            SyncMusicTask(
                this@MainActivity,
                this@MainActivity,
                this@MainActivity
            ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun getDefaultPlaylistFromServer() {
        GetDefaultPlaylistTask(
            this@MainActivity,
            this@MainActivity
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun onConnectionSuccess(task: BaseTask<*>?, data: Any?) {
        if (task is GetProfileTask) {
            val output = data as GetProfileOutput
            if (output.success && output.data != null) {
                SharedPreferenceHelper.getInstance()
                    .set(Constants.PREF_PROFILE, Gson().toJson(output.data))

                if (output.data.isMaster == 1 && output.data.isPremium == 1
                    && output.data.isHighAbundance == 1 && output.data.isHighQuantum == 1
                ) {
                    SharedPreferenceHelper.getInstance().setBool(Constants.IS_UNLOCK_ALL, true)
                } else {
                    SharedPreferenceHelper.getInstance().setBool(Constants.IS_UNLOCK_ALL, false)
                }
                if (output.data.isMaster == 1) {
                    SharedPreferenceHelper.getInstance().setBool(Constants.KEY_PURCHASED, true)
                } else {
                    SharedPreferenceHelper.getInstance().setBool(Constants.KEY_PURCHASED, false)
                }
                if (output.data.isPremium == 1) {
                    SharedPreferenceHelper.getInstance()
                        .setBool(Constants.KEY_PURCHASED_ADVANCED, true)
                } else {
                    SharedPreferenceHelper.getInstance()
                        .setBool(Constants.KEY_PURCHASED_ADVANCED, false)
                }
                if (output.data.isHighAbundance == 1) {
                    SharedPreferenceHelper.getInstance()
                        .setBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE, true)
                } else {
                    SharedPreferenceHelper.getInstance()
                        .setBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE, false)
                }
                if (output.data.isHighQuantum == 1) {
                    SharedPreferenceHelper.getInstance()
                        .setBool(Constants.KEY_PURCHASED_HIGH_QUANTUM, true)
                } else {
                    SharedPreferenceHelper.getInstance()
                        .setBool(Constants.KEY_PURCHASED_HIGH_QUANTUM, false)
                }
                val intent = Intent(Constants.BROADCAST_ACTION_PURCHASED)
                sendBroadcast(intent)
            } else {
                showAlert(getString(R.string.txt_warning_login_fail))
            }
        }
        if (task is GetDefaultPlaylistTask) {
            val result = data as GetAPKsNewVersionOutput
            if (result.code == 200 && result.apks != null && result.apks.size > 0) {
                SharedPreferenceHelper.getInstance()
                    .set(Constants.PREF_DEFAUT_PLAYLIST_JSON, result.apks.get(0))
            }
            GetAllAlbumTask(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            SyncMusicTask(
                this@MainActivity,
                this@MainActivity,
                this@MainActivity
            ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        if (task is GetNewAlbumsTask) {
            if (mLoadingDialog.isShowing)
                mLoadingDialog.dismiss()
            @Suppress("UNCHECKED_CAST")
            val newAlbums = data as ArrayList<Album>
            //Download done if size = 0
            SharedPreferenceHelper.getInstance()
                .setBool(Constants.IS_DOWNLOADED_ALL_ALBUM, newAlbums.size == 0)
            if (newAlbums.size > 0 && !isDownloadingData) {
                isDownloadingData = true
                needToDownloadData = true
                mIsFromBroadcastReceiverPurchase = false
//                mDownloadMusicDialog = DownloadMusicDialog(this@MainActivity, newAlbums, object : DownloadMusicDialog.Callback {
//                    override fun onError(throwable: Throwable) {
//                        AlertDialog.Builder(this@MainActivity)
//                                .setMessage(R.string.msg_download_data)
//                                .setPositiveButton(R.string.txt_try_again) { _, _ ->
//                                    isDownloadingData = false
//                                    GetNewAlbumsTask(this@MainActivity, this@MainActivity).execute()
//                                }
//                                .setNegativeButton(R.string.txt_cancel) { _, _ ->
//                                    getDefaultPlaylistFromServer()
//                                    isDownloadingData = false
//                                }
//                                .show()
//                    }
//
//                    override fun onSuccess() {
//                        isDownloadingData = false
//                        getDefaultPlaylistFromServer()
//                        //SyncMusicTask(this@MainActivity, this@MainActivity, this@MainActivity).execute()
//                    }
//                })
                mDownloadMusicManager = DownloadMusicManager(
                    this@MainActivity,
                    newAlbums,
                    object : DownloadMusicManager.Callback {
                        override fun onError(throwable: Throwable) {
                            AlertDialog.Builder(this@MainActivity)
                                .setMessage(R.string.msg_download_data)
                                .setPositiveButton(R.string.txt_try_again) { _, _ ->
                                    isDownloadingData = false
                                    GetNewAlbumsTask(this@MainActivity, this@MainActivity).execute()
                                    viewGroupDownload.visibility = View.GONE
                                }
                                .setNegativeButton(R.string.txt_cancel) { _, _ ->
                                    getDefaultPlaylistFromServer()
                                    isDownloadingData = false
                                    viewGroupDownload.visibility = View.GONE
                                }
                                .show()
                        }

                        override fun onPercentRatio(
                            ratio: String,
                            currentStep: Int,
                            totalStep: Int
                        ) {
                            mTvDownloadPercent.post(Runnable {
                                mTvDownloadPercent.text = ratio
                                if (currentStep == totalStep) {
                                    mDownloadMusicManager?.dismissDialog()
                                }
                            })
                        }

                        override fun onSuccess() {
                            isDownloadingData = false
                            getDefaultPlaylistFromServer()
                            viewGroupDownload.visibility = View.GONE
                            //SyncMusicTask(this@MainActivity, this@MainActivity, this@MainActivity).execute()
                        }
                    })
                mDownloadMusicManager?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                viewGroupDownload.visibility = View.VISIBLE
//                try {
//                    mDownloadMusicDialog!!.show()
//                }catch (e : WindowManager.BadTokenException){}
            } else if (isDownloadingData && mIsFromBroadcastReceiverPurchase) {
                mIsFromBroadcastReceiverPurchase = false
//                if (mDownloadMusicDialog != null) {
//                    mDownloadMusicDialog!!.additionDownloadData(newAlbums)
//                }
                if (mDownloadMusicManager != null) {
                    mDownloadMusicManager!!.additionDownloadData(newAlbums)
                    viewGroupDownload.visibility = View.VISIBLE
                }
            } else {
                //load playlist default
                getDefaultPlaylistFromServer()
            }
        }
        if (task is SyncMusicTask) {
            if (mEncryptingProgressDialog != null && mEncryptingProgressDialog!!.isShowing) {
                mEncryptingProgressDialog?.dismiss()
            }
            if (isGetNewAlbumFromServer) {
                isGetNewAlbumFromServer = false
                GetNewAlbumsTask(this, this).execute()
            } else {
                kotlin.run {
                    val playlistDao = QFDatabase.getDatabase(this@MainActivity).playlistDAO()
                    if (playlistDao.getFirstPlaylist().isEmpty()) {
                        val playlist = Playlist()
                        playlist.title = "Playlist 1"
                        playlist.fromUsers = 1
                        playlistDao.insert(playlist)
                    }
                }
                UpdateDurationOfAllPlaylistTask(
                    this@MainActivity.application,
                    null
                ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }


        }
        if (task is GetAPKNewVersionTask) {
            val output = data as GetAPKsNewVersionOutput
            if (output.code == 200) {
                if (output.apks != null && output.apks.size > 0) {
                    val currentVer = "v" + BuildConfig.VERSION_NAME
                    val apkUrl = output.apks.get(0)
                    val pathSplit = apkUrl.split("/")
                    if (pathSplit.size > 0) {
                        val fileName = pathSplit.get(pathSplit.size - 1)
                        val newVersion = fileName.replace("Quantum_", "").replace(".apk", "")
                        if (!newVersion.equals(currentVer, ignoreCase = true)) {
                            dialogConfirmUpdateApk(apkUrl, fileName)
                        } else {
                            deleteAPKFolder()
                        }
                    } else {
                        deleteAPKFolder()
                    }
                } else {
                    deleteAPKFolder()
                }
            } else {
                deleteAPKFolder()
            }
        }
        if (task is GetFlashSaleTask) {
            val jsonFlashSale = data as String
            Log.i("jsonre", "r-->$jsonFlashSale")
            if (jsonFlashSale.isNotEmpty()) {
                val jsonCurrent = Gson().fromJson(jsonFlashSale, GetFlashSaleOutput::class.java)

//                Luon hien flash sale
//                jsonCurrent.flashSale.enable = true
//                jsonFlashSale = Gson().toJson(jsonCurrent)

                var flashsaleCurrentString = ""
                if (jsonCurrent?.flashSale != null) {
                    flashsaleCurrentString = Gson().toJson(jsonCurrent.flashSale)
                }
                val jsonOrgrialString =
                    SharedPreferenceHelper.getInstance().get(Constants.PREF_FLASH_SALE)
                var flashsaleOrgrialString = ""
                if (jsonOrgrialString != null) {
                    val jsonOrgrial =
                        Gson().fromJson(jsonOrgrialString, GetFlashSaleOutput::class.java)
                    if (jsonOrgrial?.flashSale != null) {
                        flashsaleOrgrialString = Gson().toJson(jsonOrgrial.flashSale)
                    }
                }

                SharedPreferenceHelper.getInstance().set(Constants.PREF_FLASH_SALE, jsonFlashSale)

                if (!flashsaleCurrentString.equals(flashsaleOrgrialString, ignoreCase = true)) {
                    SharedPreferenceHelper.getInstance()
                        .setInt(Constants.PREF_FLASH_SALE_COUNTERED, 0)
                    loadDs()
                }

                fetchInterstitialDs()

                if (SharedPreferenceHelper.getInstance()
                        .getInt(Constants.PREF_FLASH_SALE_COUNTERED) <= jsonCurrent.flashSale.proposalsCount!!
                ) {
                    QcAlarmManager.createAlarms(this)
                } else {
                    QcAlarmManager.clearAlarms(this)
                }

                //Create reminder
                QcAlarmManager.createReminderAlarm(this)

                loadCountdownTime()
            } else {
                QcAlarmManager.clearAlarms(this)
            }
        }
    }

    private fun fetchInterstitialDs() {
//        val jsonOrgrialString = SharedPreferenceHelper.getInstance().get(Constants.PREF_FLASH_SALE)
//        if (jsonOrgrialString != null && jsonOrgrialString.isNotEmpty()) {
//            val flashSaleOutput = Gson().fromJson(
//                jsonOrgrialString,
//                GetFlashSaleOutput::class.java
//            )
////            if (flashSaleOutput?.advertisements != null && flashSaleOutput.advertisements.enable!!) {
////                if (!SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)) {
////
////                }
////            }
//        }
    }

    private fun deleteAPKFolder() {
        val apkFolder = File(CACHE_APK_FOLDER, Constants.APKS_FOLDER)
        FilesUtils.deleteFileInDir(apkFolder)
    }

    private fun dialogConfirmUpdateApk(apkUrl: String, fileName: String) {
        try {
            android.app.AlertDialog.Builder(this)
                .setTitle(R.string.txt_warning_update_newversion_title)
                .setMessage(R.string.txt_warning_update_newversion)
                .setCancelable(false)
                .setPositiveButton(R.string.txt_agree) { _, _ ->
                    downloadAPK(apkUrl, fileName)
                }.setNegativeButton(R.string.txt_disagree) { _, _ ->

                }.show()
        } catch (_: WindowManager.BadTokenException) {
        }
    }

    fun downloadAPK(apkUrl: String, fileName: String) {
        val apkFolder = File(CACHE_APK_FOLDER, Constants.APKS_FOLDER)
        val apkFile = File(apkFolder, fileName)
        mLocalApkPath = apkFile.path
        if (apkFile.exists()) {
            autoInstallNewAPK()
        } else {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.txt_downloading_dot),
                Toast.LENGTH_LONG
            ).show()

            FilesUtils.deleteFileInDir(apkFolder)
            downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            Download_Uri = Uri.parse(apkUrl)
            val request = DownloadManager.Request(Download_Uri)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setAllowedOverRoaming(false)
            request.setTitle("[New APK] " + getString(R.string.app_name))
            request.setDescription("Downloading " + fileName)
            request.setVisibleInDownloadsUi(true)
//            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/Quantum/" + "/" + "Quantum_v1.0" + ".apk")
            request.setDestinationUri(Uri.fromFile(apkFile))
            refid = downloadManager!!.enqueue(request)
        }
    }

    fun autoInstallNewAPK() {
        if (mLocalApkPath != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(
                Uri.fromFile(File(mLocalApkPath!!)),
                "application/vnd.android.package-archive"
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK; // without this flag android returned a intent error!
            startActivity(intent);
        }
    }

    private var doubleBackToExitPressedOnce = false

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        sendBroadcast(Intent("BROADCAST_DISMISS_POPUP_PROGRAM"))
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit!", Toast.LENGTH_SHORT).show()

        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }
}
