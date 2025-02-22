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
    private var callback: MediaControllerCompat.Callback? = null
    private var serviceConnection: ServiceConnection? = null
    private var playing: Boolean = false
    private var repeat: Int = Player.REPEAT_MODE_OFF
    private var shuffle: Boolean = false

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.player_ui_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        playerInit()
        setListeners()
    }

    private fun setListeners() {
        currentTrack.observe(viewLifecycleOwner, {
            track_name.text = it.title

            loadImage(requireContext(), track_image, it.album)
            Log.i("currenttracl","t-->"+it.duration)
            seekBar.max = it.duration.toInt()
        })

        currentPosition.observe(viewLifecycleOwner, {
            track_position.text = getConvertedTime(it)
            seekBar.progress = it.toInt()
        })

        duration.observe(viewLifecycleOwner, {
            track_duration.text = getConvertedTime(it)
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                EventBus.getDefault().post(PlayerSeek(seekBar?.progress))
            }
        })
    }

    private fun playerInit() {
        callback = object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                if (state == null) return
                playing = state.state == PlaybackStateCompat.STATE_PLAYING

                if (playing) {
                    player_play.setImageDrawable(getDrawable(requireContext(), R.drawable.oc_pause_song))
                } else {
                    player_play.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_play_song))
                }
            }
        }

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                playerServiceBinder = binder as PlayerService.PlayerServiceBinder
                try {
                    mediaController = MediaControllerCompat(requireContext(), playerServiceBinder!!.mediaSessionToken)
                    mediaController!!.registerCallback(callback as MediaControllerCompat.Callback)
                    callback!!.onPlaybackStateChanged(mediaController!!.playbackState)

                    if (mediaController != null)
                        if (playing) mediaController!!.transportControls.pause()
                        else mediaController!!.transportControls.play()
                } catch (e: RemoteException) {
                    mediaController = null
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerServiceBinder = null
                mediaController?.unregisterCallback(callback as MediaControllerCompat.Callback)
                mediaController = null
            }
        }

        requireContext().bindService(Intent(requireContext(), PlayerService::class.java), serviceConnection as ServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)

        player_play.setOnClickListener {
            if (mediaController != null)
                if (playing) {
                    isUserPaused = true
                    mediaController!!.transportControls.pause()
                } else {
                    isUserPaused = false
                    mediaController!!.transportControls.play()
                }
        }

        player_next.setOnClickListener {
            if (mediaController != null)
                mediaController!!.transportControls.skipToNext()
            isMultiPlay = false
        }

        player_previous.setOnClickListener {
            if (mediaController != null)
                mediaController!!.transportControls.skipToPrevious()
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
            when (repeat) {
                Player.REPEAT_MODE_OFF -> {
                    repeat = Player.REPEAT_MODE_ONE
                    player_repeat.setImageResource(R.drawable.ic_replay_1)
                }
                Player.REPEAT_MODE_ONE -> {
                    repeat = Player.REPEAT_MODE_ALL
                    player_repeat.setImageResource(R.drawable.ic_replay_selected)
                }
                Player.REPEAT_MODE_ALL -> {
                    repeat = Player.REPEAT_MODE_OFF
                    player_repeat.setImageResource(R.drawable.ic_replay)
                }
            }
            EventBus.getDefault().post(PlayerRepeat(repeat))
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mediaController != null)
            mediaController!!.transportControls.stop()

        playerServiceBinder = null

        mediaController!!.unregisterCallback(callback!!)
        mediaController = null
        requireContext().unbindService(serviceConnection!!)
    }
}