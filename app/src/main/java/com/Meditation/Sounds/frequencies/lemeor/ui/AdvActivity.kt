package com.Meditation.Sounds.frequencies.lemeor.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.feature.album.detail.DescriptionAdapter
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.loadImage
import com.Meditation.Sounds.frequencies.lemeor.ui.purchase.new_flow.NewPurchaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdvActivity : AppCompatActivity() {
    private var mDescriptionAdapter: DescriptionAdapter? = null
    private var album: Album? = null
    private var album_description_recycler: RecyclerView? = null
    private var close_btn: ImageView? = null
    private var adv_tital: TextView? = null
    private var album_image: ImageView? = null
    private var btn_Unlock: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adv)
        val db = DataBase.getInstance(applicationContext)
        val albumDao = db.albumDao()
        album_description_recycler = findViewById(R.id.album_description_recycler)
        close_btn = findViewById(R.id.btn_close)
        adv_tital = findViewById(R.id.adv_tital)
        album_image = findViewById(R.id.album_image);
        btn_Unlock = findViewById(R.id.btn_Unlock)


        btn_Unlock?.setOnClickListener {
            startActivity(
                NewPurchaseActivity.newIntent(
                    this@AdvActivity,
                    album!!.category_id,
                    album!!.tier_id,
                    album!!.id
                )
            )
            finish()
        }

        close_btn?.setOnClickListener {
            finish()
        }

        CoroutineScope(Dispatchers.IO).launch {
            album = albumDao.getRandomAlbum(false)
            CoroutineScope(Dispatchers.Main).launch {
                if (album?.isUnlocked == true)
                    finish()
                else {
                    Log.e("TEST", album.toString())
                    mDescriptionAdapter =
                        album?.descriptions?.let { DescriptionAdapter(this@AdvActivity, it) }
                    album_description_recycler?.adapter = mDescriptionAdapter
                    adv_tital?.text = album?.name


                    loadImage(
                        this@AdvActivity,
                        album_image!!,
                        album!!
                    )
                }
            }
        }

    }


}