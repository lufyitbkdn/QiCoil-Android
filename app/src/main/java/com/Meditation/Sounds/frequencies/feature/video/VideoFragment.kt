package com.Meditation.Sounds.frequencies.feature.video

import android.app.Activity
import android.widget.MediaController
import android.widget.Toast
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.feature.base.BaseFragment
import com.Meditation.Sounds.frequencies.feature.main.MainActivity
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Playlist
import com.Meditation.Sounds.frequencies.lemeor.ui.videos.PlaylistAdapter
import com.Meditation.Sounds.frequencies.models.Video
import com.Meditation.Sounds.frequencies.BuildConfig
import com.Meditation.Sounds.frequencies.utils.Utils
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import android.net.Uri
import android.net.Uri.*
import android.util.Log
import com.Meditation.Sounds.frequencies.lemeor.ui.videos.NewVideosFragment
import com.Meditation.Sounds.frequencies.utils.Constants
import kotlinx.android.synthetic.main.fragment_video.*
import org.json.JSONException

class VideoFragment : BaseFragment(), YouTubePlayer.OnInitializedListener {

    private var mYouTubePlayer: YouTubePlayer? = null
    private var mVideoAdapter: VideoAdapter? = null
    private var mListVideo = ArrayList<Video>()
    private var mPlaylistAdapter: PlaylistAdapter? = null
    private var mListPlaylist = ArrayList<Playlist>()

    override fun initLayout(): Int { return R.layout.fragment_video }

    override fun initComponents() {
        mPlaylistAdapter = PlaylistAdapter(requireContext(), mListPlaylist)
        mPlaylistAdapter!!.setOnClickListener(object: PlaylistAdapter.Listener {
            override fun onClickItem(playlist: Playlist, i: Int) {
                mPlaylistAdapter!!.setSelected(i)
                getJsonPlaylist(getString(R.string.video_url, playlist.youtube_id, API_KEY + Constants.API_KEY))
            }
        })
        rv_playlist.adapter = mPlaylistAdapter

        val dao = DataBase.getInstance(requireContext()).homeDao()

        dao.getHome().observe(viewLifecycleOwner) {
            mPlaylistAdapter?.setData(it.playlists)
            getJsonPlaylist(
                getString(
                    R.string.video_url,
                    it.playlists[0].youtube_id,
                    API_KEY + Constants.API_KEY
                )
            )
        }

        val mediaController = MediaController(mContext)
        mediaController.setAnchorView(mvideoView)
        mvideoView?.setMediaController(mediaController)

        if (Utils.isConnectedToNetwork(mContext)){
           // yt_pv.initialize(API_KEY, this)

            //specify the location of media file
            if (mListVideo.isNotEmpty()) {
                val uri:Uri = parse(mListVideo[0].videoId)
                //Setting MediaController and URI, then starting the videoView
                mvideoView?.setVideoURI(uri)
                mvideoView?.requestFocus()
                mvideoView?.start()
            }

        }
        Log.i("video","video");
        mVideoAdapter = VideoAdapter(context, mListVideo)
        mVideoAdapter?.setOnClickListener(object: VideoAdapter.IOnClickItemListener {
            override fun onClickItem(video: Video, position: Int) {
                //mYouTubePlayer?.loadVideo(video.videoId)
                //mYouTubePlayer?.play()
                val uri:Uri = parse(video.videoId)
                mvideoView?.setVideoURI(uri)
                mvideoView?.requestFocus()
                mvideoView?.start()
            }
        })
        rv_video.adapter = mVideoAdapter

        if (mvideoView != null && mvideoView!!.isPlaying)
            if (activity is MainActivity) {
                val musicService = (activity as MainActivity).musicService
                musicService?.stopMusicService()
            }
    }

    override fun onInitializationSuccess(p0: YouTubePlayer.Provider, player: YouTubePlayer, wasRestored: Boolean) {
        if (!wasRestored) {
            mYouTubePlayer = player
            if (Utils.isTablet(mContext)) {
                player.setShowFullscreenButton(true)
            } else {
                player.setShowFullscreenButton(false)
            }
            if (mListVideo.isNotEmpty()) {
                player.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT)
                player.cueVideo(mListVideo[0].videoId)
                if (!Utils.isTablet(mContext)) {
                    player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
                }
            }
        }
    }

    override fun onInitializationFailure(p0: YouTubePlayer.Provider, errorReason: YouTubeInitializationResult) {
        if (errorReason.isUserRecoverableError) {
            errorReason.getErrorDialog(mContext as Activity, RECOVERY_DIALOG_REQUEST).show()
        } else {
            val error = getString(R.string.player_error, errorReason.toString())
            Toast.makeText(mContext, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun getJsonPlaylist(url: String) {
        if (!Utils.isConnectedToNetwork(mContext)) { return }

        val requestQueue = Volley.newRequestQueue(context)
        val jsonObject = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            try {
                mListVideo.clear()
                val jsonItems = response.getJSONArray("items")
                var title: String
                var urlImage: String
                var idVideo: String
                for (i in 0 until jsonItems.length()) {
                    val jsonItem = jsonItems.getJSONObject(i)
                    val jsonSnippet = jsonItem.getJSONObject("snippet")
                    title = jsonSnippet.getString("title")
                    if (!title.equals("Private video", true) && !title.equals("Deleted video", true)) {
                        val jsonThumbnail = jsonSnippet.getJSONObject("thumbnails")
                        val jsonImageMedium = jsonThumbnail.getJSONObject("medium")
                        urlImage = jsonImageMedium.getString("url")

                        val jsonResourceId = jsonSnippet.getJSONObject("resourceId")
                        idVideo = jsonResourceId.getString("videoId")

                        mListVideo.add(Video(idVideo, title, urlImage))
                    }
                }
                mVideoAdapter?.setListVideo(mListVideo)
                if (mvideoView != null) {
                    if (mListVideo.size > 0) {
                       // mYouTubePlayer?.cueVideo(mListVideo[0].videoId)
                        val uri:Uri = parse(mListVideo[0].videoId)
                        //Setting MediaController and URI, then starting the videoView
                        mvideoView?.setVideoURI(uri)
                        mvideoView?.requestFocus()
                        mvideoView?.start()
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }) { error -> Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show() }

        requestQueue.add(jsonObject)
    }

    override fun addListener() {}

    companion object {
        const val RECOVERY_DIALOG_REQUEST = 1
        const val API_KEY = "AIzaSyB7gg_gAT4mg7d"
    }
}
