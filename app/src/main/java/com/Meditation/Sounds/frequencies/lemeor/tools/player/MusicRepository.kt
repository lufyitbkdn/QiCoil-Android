package com.Meditation.Sounds.frequencies.lemeor.tools.player

import android.net.Uri
import com.Meditation.Sounds.frequencies.lemeor.currentTrack
import com.Meditation.Sounds.frequencies.lemeor.currentTrackIndex
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import java.util.*

class MusicRepository(private val data: List<Track>) {
    private val maxIndex = data.size - 1
    var currentItemIndex = 0

    fun getNext(): Track {
        if (currentItemIndex == maxIndex) currentItemIndex = 0 else currentItemIndex++
        return getCurrent()
    }

    fun getPrevious(): Track {
        if (currentItemIndex == 0) currentItemIndex = maxIndex else currentItemIndex--
        return getCurrent()
    }

    fun getRandom(): Track {
        val random = Random().nextInt(maxIndex + 1)
        if (currentItemIndex != random) { currentItemIndex = random }
        return getCurrent()
    }

    fun getCurrent(): Track {
        currentTrack.value = data[currentItemIndex]
        currentTrackIndex.value = currentItemIndex
        return data[currentItemIndex]
    }

    fun isLastTrack(): Boolean {
        return currentItemIndex == maxIndex
    }

    class Track(
            val title: String,
            val artist: String,
            val album: Album,
            val resId: Int,
            val uri: Uri,
            var duration: Long,
            var position: Long,
            var multiplay: Int
    )
}