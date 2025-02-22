package com.Meditation.Sounds.frequencies.lemeor

import android.util.Log
import com.Meditation.Sounds.frequencies.BuildConfig
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.*

fun syncTiers(db: DataBase, response: HomeResponse?) {
    Log.d("LOG", "syncTiers")

    val localData = db.tierDao().getData() as ArrayList
    val responseData = response?.tiers as ArrayList

    if (localData.isNotEmpty()) {
        Log.d("LOG", "Local bd is not empty")

        responseData.forEach { resp ->
            var isFind = false
            localData.forEach { local->
                if (resp.id == local.id) {
                    isFind = true

                    if (resp.updated_at > local.updated_at) {
                        //update
                        Log.d("LOG", "update id " + resp.id)

                        db.tierDao().insert(Tier(resp.id, resp.name, resp.order,
                                resp.updated_at, local.isShow, local.isPurchased))
                    }
                }
            }

            if (!isFind) {
                Log.d("LOG", "insert id " + resp.id)

                db.tierDao().insert(resp)
            }
        }

        localData.forEach { local ->
            var isFind = false
            responseData.forEach { resp ->
                if (local.id == resp.id) { isFind = true }
            }

            if (!isFind) {
                Log.d("LOG", "delete id " + local.id)

                db.tierDao().delete(local)
            }
        }
    } else {
        Log.d("LOG", "Bd is empty")

        db.tierDao().insertAll(responseData)

        db.tierDao().updateShowStatus(true, 1)
        db.tierDao().updateShowStatus(true, 2)
//        db.tierDao().updateShowStatus(true, 3) //hide
    }
}

fun syncCategories(db: DataBase, response: HomeResponse?) {
    val localData = db.categoryDao().getData() as ArrayList
    val responseData = response?.categories as ArrayList

    if (localData.isNotEmpty()) {
        responseData.forEach { resp ->
            var isFind = false
            localData.forEach { local->
                if (resp.id == local.id) {
                    isFind = true

                    if (resp.updated_at > local.updated_at) {
                        db.categoryDao().insert(Category(resp.id, resp.tier_id, resp.name, resp.order,
                                resp.updated_at, local.isShow, local.isPurchased))
                    }
                }
            }

            if (!isFind) {
                db.categoryDao().insert(resp)
            }
        }

        localData.forEach { local ->
            var isFind = false
            responseData.forEach { resp ->
                if (local.id == resp.id) { isFind = true }
            }

            if (!isFind) {
                db.categoryDao().delete(local)
            }
        }
    } else {
        db.categoryDao().insertAll(responseData)
    }
}

fun syncAlbums(db: DataBase, response: HomeResponse?) {
    val localData = db.albumDao().getData() as ArrayList
    val responseData = response?.albums as ArrayList

    //save tiers to albums
    response.categories.forEach { category ->
        responseData.forEach { album ->
            if (album.category_id == category.id) {
                album.tier_id = category.tier_id
            }
        }
    }

    if (localData.isNotEmpty()) {
        responseData.forEach { r->
            var isFind = false
            localData.forEach { l->
                if (r.id == l.id) {
                    isFind = true

                    if (r.updated_at > l.updated_at) {
                        db.albumDao().insert(Album(r.id, r.category_id, r.tier_id, r.name, r.image, r.audio_folder,
                                r.is_free, r.order, r.updated_at, r.descriptions, r.tracks, r.tag, l.isDownloaded, checkUnlocked(r.is_free)))
                    }
                }
            }

            if (!isFind) {
                db.albumDao().insert(Album(r.id, r.category_id, r.tier_id, r.name, r.image, r.audio_folder,
                        r.is_free, r.order, r.updated_at, r.descriptions, r.tracks, r.tag, true, checkUnlocked(r.is_free)))
            }
        }

        localData.forEach { l->
            var isFind = false
            responseData.forEach { r->
                if (l.id == r.id) { isFind = true }
            }

            if (!isFind) {
                db.albumDao().delete(l)
            }
        }
    } else {
        db.albumDao().insertAll(responseData)

        if (BuildConfig.IS_FREE) {
            responseData.forEach {
                db.albumDao().syncAlbums(isDownloaded = false, isUnlocked = true, id = it.id)
            }
        } else {
            responseData.forEach {
                db.albumDao().syncAlbums(it.isDownloaded, checkUnlocked(it.is_free), it.id)
            }
            db.albumDao().syncDownloaded(true, 1)
            db.albumDao().syncDownloaded(true, 2)
            db.albumDao().syncDownloaded(true, 3)
        }
    }
}

fun syncTracks(db: DataBase, response: HomeResponse?) {
    val localData = db.trackDao().getData() as ArrayList
    val responseData: ArrayList<Track> = ArrayList()

    response?.albums?.forEach { album ->
        album.tracks.forEach { track ->

            if (!BuildConfig.IS_FREE) { if (album.id == 1 || album.id == 2 || album.id == 3) { track.isDownloaded = true } }

            track.albumId = album.id
            track.isUnlocked = checkUnlocked(album.is_free)
        }
        responseData.addAll(album.tracks)
    }

    //save tiers to albums
    response?.categories?.forEach { category ->
        response.albums.forEach { album ->
            album.tracks.forEach { track ->
                if (album.category_id == category.id) {
                    track.tier_id = category.tier_id
                }
            }
        }
    }

    if (localData.isNotEmpty()) {
        responseData.forEach { resp ->
            var isFind = false
            localData.forEach { local->
                if (resp.id == local.id) {
                    isFind = true

                    if (resp.updated_at > local.updated_at) {
                        db.trackDao().insert(Track(resp.id, resp.name, resp.filename, resp.tier_id, resp.updated_at,
                                false, local.isFavorite, local.isDownloaded, resp.albumId,
                                local.isUnlocked, local.duration, null, 0))
                    }
                }
            }

            if (!isFind) { db.trackDao().insert(resp) }
        }

        localData.forEach { local ->
            var isFind = false
            responseData.forEach { resp ->
                if (local.id == resp.id) { isFind = true }
            }

            if (!isFind) { db.trackDao().delete(local) }
        }
    } else {
        db.trackDao().insertAll(responseData)

        if (BuildConfig.IS_FREE) {
            responseData.forEach {
                db.trackDao().setTrackUnlocked(true, it.id)
                db.trackDao().setTrackDownloaded(true, it.id)
            }
        }
    }
}

fun syncPrograms(db: DataBase, response: HomeResponse?) {
    val localData = db.programDao().getData(false) as ArrayList
    val delete = db.programDao().getData(false) as ArrayList
    val responseData = response?.programs as ArrayList

    if (localData.isNotEmpty()) {
        val itrResp = responseData.iterator()
        while (itrResp.hasNext()) {
            val resp = itrResp.next()
            localData.forEach { local ->
                if (resp.id == local.id) {
                    if (resp.updated_at > local.updated_at) {
                        db.programDao().insert(resp)
                        db.programDao().syncPrograms(local.isMy, resp.id)
                    }
                    delete.remove(local)
                    itrResp.remove()
                }
            }
        }
        db.programDao().insertAll(responseData)
        db.programDao().deletePrograms(delete)
    } else {
        db.programDao().insertAll(response.programs)
        db.programDao().insert(Program(0, FAVORITES, 0, 0, ArrayList(), isMy = true))
        db.programDao().insert(Program(0, "Playlist 1", 0, 0, ArrayList(), isMy = true))
    }
}

fun syncPlaylists(db: DataBase, response: HomeResponse?) {
    val localData = db.playlistDao().getData() as ArrayList
    val delete = db.playlistDao().getData() as ArrayList
    val responseData = response?.playlists as ArrayList

    if (localData.isNotEmpty()) {
        val itrResp = responseData.iterator()
        while (itrResp.hasNext()) {
            val resp = itrResp.next()
            localData.forEach { local ->
                if (resp.id == local.id) {
                    if (resp.updated_at > local.updated_at) {
                        db.playlistDao().insert(resp)
                    }
                    delete.remove(local)
                    itrResp.remove()
                }
            }
        }
        db.playlistDao().insertAll(responseData)
        db.playlistDao().deletePlaylists(delete)
    } else {
        db.playlistDao().insertAll(response.playlists)
    }
}

fun syncTags(db: DataBase, response: HomeResponse?) {
    val localData = db.tagDao().getData() as ArrayList
    val delete = db.tagDao().getData() as ArrayList
    val responseData = response?.tags as ArrayList

    if (localData.isNotEmpty()) {
        val itrResp = responseData.iterator()
        while (itrResp.hasNext()) {
            val resp = itrResp.next()
            localData.forEach { local ->
                if (resp.id == local.id) {
                    if (resp.updated_at > local.updated_at) {
                        db.tagDao().insert(resp)
                    }
                    delete.remove(local)
                    itrResp.remove()
                }
            }
        }
        db.tagDao().insertAll(responseData)
        db.tagDao().deleteTags(delete)
    } else {
        db.tagDao().insertAll(response.tags)
    }
}

fun checkUnlocked(isFree: Int): Boolean {
    return isFree == 1
}

fun checkTest(responseIsFree: Int, localIsFree: Boolean): Boolean {
    return when {
        localIsFree -> { true }
        responseIsFree == 1 -> { true }
        else -> { false }
    }
}

