package com.Meditation.Sounds.frequencies.lemeor.ui.programs.detail

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.*
import com.Meditation.Sounds.frequencies.lemeor.data.api.RetrofitBuilder
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.ProgramDao
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import com.Meditation.Sounds.frequencies.lemeor.data.remote.ApiHelper
import com.Meditation.Sounds.frequencies.lemeor.data.utils.ViewModelFactory
import com.Meditation.Sounds.frequencies.lemeor.tools.downloader.DownloadService
import com.Meditation.Sounds.frequencies.lemeor.tools.downloader.DownloaderActivity
import com.Meditation.Sounds.frequencies.lemeor.tools.player.MusicRepository
import com.Meditation.Sounds.frequencies.lemeor.tools.player.PlayerSelected
import com.Meditation.Sounds.frequencies.lemeor.ui.albums.detail.NewAlbumDetailFragment
import com.Meditation.Sounds.frequencies.lemeor.ui.main.NavigationActivity
import com.Meditation.Sounds.frequencies.lemeor.ui.programs.NewProgramFragment
import com.Meditation.Sounds.frequencies.utils.Utils
import kotlinx.android.synthetic.main.fragment_new_album_detail.*
import kotlinx.android.synthetic.main.fragment_program_detail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ProgramDetailFragment : Fragment() {

    private val programId: Int by lazy {
        arguments?.getInt(ARG_PROGRAM_ID) ?: throw IllegalArgumentException("Must call through newInstance()")
    }

    private var mTracks: ArrayList<Track>? = null
    private var program: Program? = null
    private lateinit var mViewModel: ProgramDetailViewModel
    private var mTrackAdapter: ProgramTrackAdapter? = null
    private var isDownloaded: Boolean = true

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: Any?) {
        if (event == DownloadService.DOWNLOAD_FINISH) {
            downloadedTracks = null

            val tracks: ArrayList<Track> = ArrayList()

            val dao = DataBase.getInstance(requireContext()).albumDao()
            GlobalScope.launch {
                program?.records?.forEach {
                    mViewModel.getTrackById(it)?.let { track ->
                        tracks.add(track)
                    }
                }

                var isDownloaded = true
                tracks.forEach {
                    val album = dao.getAlbumById(it.albumId)

                    val file = File(getSaveDir(requireContext(), it, album!!))
                    val preloaded = File(getPreloadedSaveDir(requireContext(), it, album))

                    if (!file.exists() && !preloaded.exists()) {
                        isDownloaded = false
                    }
                }

                this@ProgramDetailFragment.isDownloaded = isDownloaded

                CoroutineScope(Dispatchers.Main).launch {
                    mTracks = tracks

                    if (!isDownloaded) {
                        program_play.text = getString(R.string.btn_download)
                    } else {
                        program_play.text = getString(R.string.btn_play)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        EventBus.getDefault().unregister(this)
        isTrackAdd = false
        albumIdBackProgram = -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_program_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initUI()

        mViewModel.program(programId)?.observe(viewLifecycleOwner, {
            program = it

            setUI(it)
        })

        view?.isFocusableInTouchMode = true
        view?.requestFocus()
        view?.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                onBackPressed()
                true
            } else false
        }
    }

    private fun initUI() {
        mViewModel = ViewModelProvider(this,
                ViewModelFactory(
                        ApiHelper(RetrofitBuilder(requireContext()).apiService),
                        DataBase.getInstance(requireContext()))
        ).get(ProgramDetailViewModel::class.java)
    }

    private fun onBackPressed() {
        var fragment: Fragment?

        if (isTrackAdd) {
            fragment = albumIdBackProgram?.let { NewAlbumDetailFragment.newInstance(it) }
        } else {
            fragment = selectedNaviFragment
            if (fragment == null) { fragment = NewProgramFragment() }
        }

        parentFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.trans_left_to_right_in, R.anim.trans_left_to_right_out)
                .replace(R.id.nav_host_fragment, fragment!!, fragment.javaClass.simpleName)
                .commit()
    }

    private fun setUI(program: Program) {
        val tracks: ArrayList<Track> = ArrayList()

        program_back.setOnClickListener { onBackPressed() }

        program_name.text = program.name
        program_time.text = getString(R.string.total_time, getConvertedTime((program.records.size * 300000).toLong()))

        program_play.setOnClickListener {
            if (tracks.size == 0) {
                Toast.makeText(requireContext(), "Empty List", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            playOrDownload(tracks)
        }

        mTrackAdapter = ProgramTrackAdapter(requireContext(), tracks, program.isMy)
        mTrackAdapter!!.setOnClickListener(object : ProgramTrackAdapter.Listener {
            override fun onTrackClick(track: Track, i: Int) {
                if (isDownloaded) {
                    isMultiPlay = false
                    mTrackAdapter?.setSelected(i)
                    play(tracks)
                    Handler(Looper.getMainLooper()).postDelayed({ EventBus.getDefault().post(PlayerSelected(i)) }, 200)
                }
            }

            override fun onTrackOptions(track: Track, i: Int) {
                positionFor = i

                startActivityForResult(PopActivity.newIntent(requireContext(), track.id), 1002)
            }
        })
        program_tracks_recycler.adapter = mTrackAdapter

        val dao = DataBase.getInstance(requireContext()).albumDao()
        GlobalScope.launch {
            program.records.forEach {
                mViewModel.getTrackById(it)?.let { track ->
                    tracks.add(track)
                }
            }

            var isDownloaded = true
            tracks.forEach {
                val album = dao.getAlbumById(it.albumId)
                it.album = album

                val file = File(getSaveDir(requireContext(), it, album!!))
                val preloaded = File(getPreloadedSaveDir(requireContext(), it, album))

                if (!file.exists() && !preloaded.exists()) {
                    isDownloaded = false
                }
            }

            this@ProgramDetailFragment.isDownloaded = isDownloaded

            CoroutineScope(Dispatchers.Main).launch {
                mTracks = tracks
                mTrackAdapter?.setData(tracks)

                if (!isDownloaded) {
                    program_play.text = getString(R.string.btn_download)
                } else {
                    program_play.text = getString(R.string.btn_play)
                }
            }
        }

        currentTrackIndex.observe(viewLifecycleOwner, {
            tracks.forEachIndexed { index, _ ->
                if (index == it) {
                    mTrackAdapter?.setSelected(index)
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun playOrDownload(tracks: ArrayList<Track>) {
        if (!isDownloaded) {
            if (!Utils.isConnectedToNetwork(requireContext())) {
                Toast.makeText(requireContext(), getString(R.string.err_network_available), Toast.LENGTH_SHORT).show()
                return
            }

            val list = ArrayList<Track>()

            val dao = DataBase.getInstance(requireContext()).albumDao()
            GlobalScope.launch {
                tracks.forEach { t->
                    val album = dao.getAlbumById(t.albumId)
                    t.album = album

                    val file = File(getSaveDir(requireContext(), t, album!!))
                    val preloaded = File(getPreloadedSaveDir(requireContext(), t, album))

                    if (!file.exists() && !preloaded.exists()) {
                        var isExist = false
                        list.forEach { l->
                            if (l.id == t.id) { isExist = true }
                        }
                        if (!isExist) { list.add(t) }
                    }
                }

                downloadedTracks = list

                CoroutineScope(Dispatchers.Main).launch {
                    startActivity(DownloaderActivity.newIntent(requireContext(), list))
                }
            }
        } else {
            play(tracks)
            EventBus.getDefault().post(PlayerSelected(0))
        }
    }

    fun play(tracks: ArrayList<Track>) {
        val activity = activity as NavigationActivity

        if (isPlayAlbum || playProgramId != programId) { activity.hidePlayerUI() }

        isPlayAlbum = false
        playAlbumId = -1
        isPlayProgram = true
        playProgramId = programId

        val data: ArrayList<MusicRepository.Track> = ArrayList()
        val db = DataBase.getInstance(requireContext())

        GlobalScope.launch {
            tracks.forEach { t ->
                val file = File(getSaveDir(requireContext(), t, t.album!!))
                val preloaded = File(getPreloadedSaveDir(requireContext(), t, t.album!!))

                var uri: Uri? = null

                if (file.exists()) { uri = Uri.fromFile(file) }

                if (preloaded.exists()) { uri = Uri.fromFile(preloaded) }

                val track = db.trackDao().getTrackById(t.id)
                if (track?.duration == 0.toLong()) { track.duration = getDuration(file) }
                Log.i("fileduration","d-->"+getDuration(file))
                val multiplay = track?.duration!! / 300000

                data.add(MusicRepository.Track(track.name, t.album?.name!!, t.album!!, R.drawable.launcher, uri!!, track.duration, 0, multiplay.toInt()))
            }

            trackList = data

            CoroutineScope(Dispatchers.Main).launch {
                activity.showPlayerUI()
            }
        }
    }

    private fun getDuration(file: File): Long {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(file.absolutePath)
        val durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return durationStr!!.toLong()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            val action = data.getStringExtra(PopActivity.EXTRA_ACTION)

            val db = DataBase.getInstance(requireContext())
            val programDao = db.programDao()
            val trackDao = db.trackDao()

            val activity = activity as NavigationActivity
            isPlayAlbum = false
            isPlayProgram = false
            activity.hidePlayerUI()

            GlobalScope.launch {
                val list = program?.records as MutableList<Int>

                when {
                    action.equals("track_move_up") -> {
                        if (positionFor == 0) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(requireContext(), "Track in first position", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        moveTrack(list, true, programDao)
                    }
                    action.equals("track_move_down") -> {
                        if (positionFor == list.size - 1) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(requireContext(), "Track in last position", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        moveTrack(list, false, programDao)
                    }
                    action.equals("track_remove") -> {
                        positionFor?.let { mTracks?.get(it)?.id?.let { it1 -> trackDao.isTrackFavorite(false, it1) } }

                        list.removeAt(positionFor!!)
                        program?.records = list as ArrayList<Int>
                        program?.let { programDao.updateProgram(it) }
                    }
                }
            }
        }
    }

    private fun moveTrack(list: MutableList<Int>, isMoveUp: Boolean, programDao: ProgramDao) {
        val positionFrom = positionFor!!
        val positionTo = if (isMoveUp) {
            positionFor!! - 1
        } else {
            positionFor!! + 1
        }
        Collections.swap(list, positionFrom, positionTo)
        program?.records = list as ArrayList<Int>
        program?.let { programDao.updateProgram(it) }
    }

    companion object {
        const val ARG_PROGRAM_ID = "arg_program"

        @JvmStatic
        fun newInstance(id: Int) = ProgramDetailFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PROGRAM_ID, id)
            }
        }
    }
}