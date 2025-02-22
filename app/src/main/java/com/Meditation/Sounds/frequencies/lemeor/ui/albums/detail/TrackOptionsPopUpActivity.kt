package com.Meditation.Sounds.frequencies.lemeor.ui.albums.detail

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.*
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.TrackDao
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import com.Meditation.Sounds.frequencies.lemeor.tools.downloader.DownloadService
import com.Meditation.Sounds.frequencies.lemeor.tools.downloader.DownloaderActivity
import com.Meditation.Sounds.frequencies.utils.Utils
import kotlinx.android.synthetic.main.activity_pop_up_track_options.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class TrackOptionsPopUpActivity : AppCompatActivity() {

    private var track: Track? = null
    private var duration: Long = 0
    private var db: DataBase? = null
    private var trackDao: TrackDao? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: Any?) {
        if (event == DownloadService.DOWNLOAD_FINISH) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pop_up_track_options)

        db = DataBase.getInstance(applicationContext)
        trackDao = db?.trackDao()

        initUI()

        setUI()
    }

    private fun initUI() {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        val width = dm.widthPixels
        val height = dm.heightPixels

        if (resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.setLayout((width * .3).toInt(), (height * .55).toInt())
        } else if (resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.setLayout((width * .55).toInt(), (height * .3).toInt())
        }

        val params = window.attributes
        params.gravity = Gravity.CENTER
        params.x = 0
        params.y = -20

        window.attributes = params

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setUI() {
        val trackId = intent.getIntExtra(EXTRA_TRACK_ID, -1)
        if (trackId == -1) {
            Toast.makeText(applicationContext, "Track error", Toast.LENGTH_SHORT).show()
            return
        }

        GlobalScope.launch {
            track = trackDao?.getTrackById(trackId)

            if(track==null)
            {
                finish()
            }

            if (track!=null && track?.isFavorite!!) {
                track_add_favorites.text = getString(R.string.tv_remove_from_favorite)
            } else {
                track_add_favorites.text = getString(R.string.tv_add_to_favorites)
            }


            val dur = track?.duration!!
            duration = if (dur > 0) {
                dur
            } else {
                300000
            }
            tv_duration.text = getConvertedTime(duration)
        }

        track_add_program.setOnClickListener {
            trackIdForProgram = track?.id

            val intent = Intent()
            setResult(RESULT_OK, intent)
            finish()
        }

        track_add_favorites.setOnClickListener {
            val programDao = db?.programDao()

            if (track?.isFavorite!!) {
                Toast.makeText(applicationContext, "Removed from Favorites", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Added to Favorites", Toast.LENGTH_SHORT).show()
            }

            GlobalScope.launch {
                val program = programDao?.getProgramByName(FAVORITES)
                if (track?.isFavorite!!) {
                    program?.records?.remove(track?.id!!)
                } else {
                    program?.records?.add(track?.id!!)
                }
                program?.let { it1 -> programDao.updateProgram(it1) }

                trackDao?.isTrackFavorite(!track?.isFavorite!!, track?.id!!)
            }
            finish()
        }

        track_redownload.setOnClickListener {
            if (!Utils.isConnectedToNetwork(applicationContext)) {
                Toast.makeText(applicationContext, getString(R.string.err_network_available), Toast.LENGTH_SHORT).show()
            }
            finish()
            val tracks = ArrayList<Track>()

            GlobalScope.launch {

                // val track = trackDao.getTrackById(track.id)

                //for preloaded tracks
                try {

                    val file = File(getSaveDir(applicationContext, track!!, NewAlbumDetailFragment.album!!))
                    val preloaded = File(getPreloadedSaveDir(applicationContext, track!!, NewAlbumDetailFragment.album!!))

                    if (!file.exists() && !preloaded.exists()) {
                        track?.let { tracks.add(it) }
                    }
                    else
                    {
                        file.delete()
                        preloaded.delete()
                        track?.let { tracks.add(it) }
                    }

                    downloadedTracks = tracks

                    CoroutineScope(Dispatchers.Main).launch {
                        startActivity(DownloaderActivity.newIntent(applicationContext, tracks))
                    }
                }
                catch (ex:Exception)
                {
                    ex.printStackTrace()
                }
            }
        }

        btn_minus.setOnClickListener {
            var d = duration

            if (d > 600000) {
                d -= 300000
            } else {
                d = 300000
            }

            tv_duration.text = getConvertedTime(d)

            duration = d
        }

        btn_plus.setOnClickListener {
            var d = duration

            if (d < 86400000) {
                d += 300000
            } else if (d >= 86400000) {
                d = 86400000
            }

            tv_duration.text = getConvertedTime(d)

            duration = d
        }
    }

    override fun onStop() {
        super.onStop()

        GlobalScope.launch { trackDao?.setDuration(duration, track?.id!!) }
    }

    companion object {
        const val EXTRA_TRACK_ID = "extra_track_id"

        fun newIntent(context: Context?, id: Int): Intent {
            val intent = Intent(context, TrackOptionsPopUpActivity::class.java)
            intent.putExtra(EXTRA_TRACK_ID, id)
            return intent
        }
    }
}