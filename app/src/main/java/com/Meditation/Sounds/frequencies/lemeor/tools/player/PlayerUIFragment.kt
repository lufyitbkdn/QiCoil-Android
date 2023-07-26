package com.Meditation.Sounds.frequencies.lemeor.tools.player

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.*
import com.Meditation.Sounds.frequencies.lemeor.ui.base.NewBaseFragment
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.player_ui_fragment.*
import org.greenrobot.eventbus.EventBus


class PlayerUIFragment : NewBaseFragment() {
    private var playerServiceBinder: PlayerService.PlayerServiceBinder? = null
    private var mediaController: MediaControllerCompat? = null
    private var callback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                if (state == null) return
                playing = state.state == PlaybackStateCompat.STATE_PLAYING
                player_play?.post {
                    if (playing) {
                        player_play.setImageDrawable(
                            getDrawable(
                                requireContext(),
                                R.drawable.oc_pause_song
                            )
                        )
                    } else {
                        player_play.setImageDrawable(
                            getDrawable(
                                requireContext(),
                                R.drawable.ic_play_song
                            )
                        )
                    }
                }
            }
        }

    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            playerServiceBinder = binder as PlayerService.PlayerServiceBinder
            try {
                mediaController = MediaControllerCompat(
                    requireContext(),
                    playerServiceBinder!!.mediaSessionToken
                )
                mediaController?.registerCallback(callback)
                callback.onPlaybackStateChanged(mediaController!!.playbackState)

                if (mediaController != null) {
                    if (playing) mediaController?.transportControls?.pause()
                    else mediaController?.transportControls?.play()
                }
            } catch (e: RemoteException) {
                mediaController = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerServiceBinder = null
            mediaController?.unregisterCallback(callback)
            mediaController = null
        }
    }
    private var playing: Boolean = false
    private var repeat: Int = Player.REPEAT_MODE_ONE
    private var shuffle: Boolean = false
    private var isSeeking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.player_ui_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().bindService(
            Intent(requireContext(), PlayerService::class.java),
            serviceConnection as ServiceConnection,
            AppCompatActivity.BIND_AUTO_CREATE
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playerInit()
        setListeners()
    }

    private fun setListeners() {
        currentTrack.observe(viewLifecycleOwner) {
            track_name.text = it.title

            loadImage(requireContext(), track_image, it.album)
            Log.i("currenttracl", "t-->" + it.duration)
        }

        currentPosition.observe(viewLifecycleOwner) {
            track_position.text = getConvertedTime(it)
            if(!isSeeking) {
                seekBar.progress = it.toInt()
            }
        }
        max.observe(viewLifecycleOwner) {
            seekBar.max = it.toInt()
        }

        duration.observe(viewLifecycleOwner) {
            seekBar.isEnabled = it > 0
            track_duration.text = getConvertedTime(it)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {}

            override fun onStartTrackingTouch(p0: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeeking = false
                EventBus.getDefault().post(PlayerSeek(seekBar.progress))
            }
        })
    }

    private fun playerInit() {
        player_play.setOnClickListener {
            val rotation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.clockwise_rotation)
            rotation.repeatCount = Animation.INFINITE
            player_repeat.clearAnimation()
            if (mediaController != null)
                if (playing) {
                    isUserPaused = true
                    mediaController?.transportControls?.pause()
                } else {
                    isUserPaused = false
                    mediaController?.transportControls?.play()
                    if (repeat == Player.REPEAT_MODE_ALL) {
                        player_repeat.startAnimation(rotation)
                    }
                }
        }

        player_next.setOnClickListener {
            if (mediaController != null)
                mediaController?.transportControls?.skipToNext()
            isMultiPlay = false
        }

        player_previous.setOnClickListener {
            if (mediaController != null)
                mediaController?.transportControls?.skipToPrevious()
        }

        player_shuffle.setOnClickListener {
            if (!shuffle) {
                shuffle = true
                player_shuffle.setImageResource(R.drawable.ic_shuffle_selected)
            } else {
                shuffle = false
                player_shuffle.setImageResource(R.drawable.ic_shuffle)
            }
            EventBus.getDefault().post(PlayerShuffle(shuffle))
        }

        player_repeat.setOnClickListener {
            val rotation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.clockwise_rotation)
            rotation.repeatCount = Animation.INFINITE
            player_repeat.clearAnimation()
            when (repeat) {
                Player.REPEAT_MODE_OFF -> {
                    repeat = Player.REPEAT_MODE_ONE
                    player_repeat.setImageResource(R.drawable.ic_loop_one)
                }

                Player.REPEAT_MODE_ONE -> {
                    repeat = Player.REPEAT_MODE_ALL
                    player_repeat.setImageResource(R.drawable.ic_loop_all)
                    if (playing) {
                        player_repeat.startAnimation(rotation)
                    }
                }

                Player.REPEAT_MODE_ALL -> {
                    repeat = Player.REPEAT_MODE_OFF
                    player_repeat.setImageResource(R.drawable.ic_loop_off)
                }
            }
            EventBus.getDefault().post(PlayerRepeat(repeat))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaController != null)
            mediaController?.transportControls?.stop()
        playerServiceBinder = null
        mediaController?.unregisterCallback(callback)
        mediaController = null
        serviceConnection.let { requireContext().unbindService(it) }
    }
}