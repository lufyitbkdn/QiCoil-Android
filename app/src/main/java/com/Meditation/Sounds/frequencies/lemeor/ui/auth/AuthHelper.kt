package com.Meditation.Sounds.frequencies.lemeor.ui.auth

import android.content.Context
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun updateUnlocked(context: Context, user: User, isUnlocked: Boolean) {
    val tiers = user.unlocked_tiers
    val categories = user.unlocked_categories
    val albums = user.unlocked_albums

    val db = DataBase.getInstance(context)

    CoroutineScope(Dispatchers.IO).launch {
        tiers.forEach { tier->
            if (tier >= 3) { db.tierDao().updateShowStatus(isUnlocked, tier) }

            db.albumDao().setNewUnlockedByTierId(isUnlocked, tier)
        }

        categories.forEach {
                category-> db.albumDao().setNewUnlockedByCategoryId(isUnlocked, category)
        }

        albums.forEach { album->
            db.albumDao().syncAlbums(isUnlocked = isUnlocked, id = album)
        }
    }
}

fun updateTier(context: Context, user: User) {
    CoroutineScope(Dispatchers.IO).launch {
        val db = DataBase.getInstance(context)
        val tierDao = db.tierDao()

        tierDao.getData()?.forEach { tier ->
            if (tier.id >= 3) {

                var isExist = false

                user.unlocked_tiers.forEach { unlocked ->
                    if (tier.id == unlocked) {
                        isExist = true
                    }
                }

                if (isExist) {
                    if (!tierDao.getShowStatus(tier.id)) {
                        tierDao.updateShowStatus(true, tier.id)
                    }
                } else {
                    if (tierDao.getShowStatus(tier.id)) {
                        tierDao.updateShowStatus(false, tier.id)
                    }
                }
            }
        }
    }
}