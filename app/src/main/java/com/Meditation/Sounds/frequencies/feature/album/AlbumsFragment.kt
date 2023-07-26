package com.Meditation.Sounds.frequencies.feature.album

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.db.QFDatabase
import com.Meditation.Sounds.frequencies.feature.album.detail.AlbumDetailGroupFragment
import com.Meditation.Sounds.frequencies.feature.base.BaseActivity
import com.Meditation.Sounds.frequencies.feature.base.BaseFragment
import com.Meditation.Sounds.frequencies.feature.main.MainActivity
import com.Meditation.Sounds.frequencies.lemeor.data.utils.Resource.Status.*
import com.Meditation.Sounds.frequencies.models.Album
import com.Meditation.Sounds.frequencies.models.Playlist
import com.Meditation.Sounds.frequencies.models.Song
import com.Meditation.Sounds.frequencies.utils.Constants
import com.Meditation.Sounds.frequencies.utils.SharedPreferenceHelper
import com.Meditation.Sounds.frequencies.utils.Utils
import com.Meditation.Sounds.frequencies.views.ItemOffsetDecoration
import kotlinx.android.synthetic.main.fragment_albums.*

class AlbumsFragment : BaseFragment() {
    private lateinit var mAdapter: AlbumsAdapter
    private lateinit var mViewModel: AlbumsViewModel

    private var isAdvanced: Boolean = false

    var baseActivity: BaseActivity? = null

    private val broadcastReceiverPurchased = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            if (mTabAdvanced.isSelected) {
                checkPurchase(1)
            }
        }
    }

    private lateinit  var mSongsAdapter: ItemSearchSongAdapter
    private lateinit var mAlbumAdapter: ItemSearchAlbumsAdapter
    private lateinit var mPlaylistAdapter: ItemSearchPlaylistAdapter
    private var mSongs = ArrayList<Song>()
    private var mSongAdvanced = ArrayList<Song>()
    private var mSongHigherAbundance = ArrayList<Song>()
    private var mSongHigherQuantum = ArrayList<Song>()
    private var mAlbumsSearch = ArrayList<Album>()
    private var mAlbumAdvanced = ArrayList<Album>()
    private var mAlbumAdvancedSearch = ArrayList<Album>()
    private var mAlbumHigherAbundance = ArrayList<Album>()
    private var mAlbumHigherAbundanceSearch = ArrayList<Album>()
    private var mAlbumHigherQuantum = ArrayList<Album>()
    private var mAlbumHigherQuantumSearch = ArrayList<Album>()
    private var mPlaylist = ArrayList<Playlist>()

    var mSearchRunnable = Runnable { searchTracks() }
    var mHandler = Handler(Looper.getMainLooper())

    var isPurchasedBasic: Boolean = false
    var isPurchasedAdvanced: Boolean = false
    var isPurchasedHigherAbundance: Boolean = false
    var isPurchasedHigherQuantum: Boolean = false

    override fun initLayout(): Int {
        return R.layout.fragment_albums
    }

    override fun initComponents() {
//        FilesUtils.deleteRecursive(File(FilesUtils.getSdcardStore(), Constants.DEFAULT_DATA_FOLDER))

        updateView()
        val itemDecoration = ItemOffsetDecoration(
            requireContext(),
            if (Utils.isTablet(context)) R.dimen.item_offset else R.dimen.margin_buttons
        )
        rcTrack.addItemDecoration(itemDecoration)
        baseActivity = mContext as BaseActivity
        isAdvanced = false
        mAdapter = AlbumsAdapter(
            mContext,
            ArrayList(),
            isAdvanced,
            object : AlbumsAdapter.IOnItemClickListener {
                override fun onItemClick(album: Album, position: Int) {
                    val playlistDao = QFDatabase.getDatabase(mContext!!).playlistDAO()
                    if (playlistDao.getFirstPlaylist().isNotEmpty()) {
                        if (mAdapter.data[position].isPurchase) {
                            (activity!! as MainActivity).addToStackFragment(
                                AlbumDetailGroupFragment.newInstance(
                                    album,
                                    TAB_ALBUM,
                                    -1,
                                    false
                                ), this@AlbumsFragment
                            )
                        } else {
                            val flashSaleRemainTimeGloble = Utils.getFlashSaleRemainTime()
                            if (flashSaleRemainTimeGloble > 0) {
                                baseActivity?.showDialogSubscriptionFS(TAB_ALBUM)
                            } else {
                                baseActivity?.showDialogSubscriptionNormal(TAB_ALBUM)
                            }
                        }
                    } else {
                        Toast.makeText(
                            mContext,
                            mContext!!.getString(R.string.msg_add_first_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        rcTrack.layoutManager = GridLayoutManager(mContext, if (Utils.isTablet(context)) 3 else 2)
        rcTrack.isNestedScrollingEnabled = false
        rcTrack.adapter = mAdapter

        rcAlbumSearch.layoutManager = LinearLayoutManager(mContext)
        rcFrequenciesSearch.layoutManager = LinearLayoutManager(mContext)
        rcProgramSearch.layoutManager = LinearLayoutManager(mContext)
        mSongsAdapter = ItemSearchSongAdapter(
            mSongs,
            object : ItemSearchSongAdapter.IOnClickItemListener {
                override fun onClickItem(position: Int) {
                    val song = mSongs[position]
                    mAdapter.data.firstOrNull {
                        it.id == song.albumId && it.name == song.albumName
                    }?.let {
                        (activity as? MainActivity)?.addFragment(
                            AlbumDetailGroupFragment.newInstance(
                                it,
                                TAB_ALBUM,
                                -1,
                                false
                            )
                        )
                    }
                    view_data?.visibility = View.GONE
                }
            })
        mAlbumAdapter = ItemSearchAlbumsAdapter(
            mAlbumsSearch,
            object : ItemSearchAlbumsAdapter.IOnClickItemListener {
                override fun onClickItem(position: Int) {
                    (activity as MainActivity).addFragment(
                        AlbumDetailGroupFragment.newInstance(
                            mAlbumsSearch[position],
                            TAB_ALBUM,
                            -1,
                            false
                        )
                    )//, this@AlbumsFragment

                    view_data?.visibility = View.GONE
                }
            })
        mPlaylistAdapter = ItemSearchPlaylistAdapter(
            mPlaylist,
            object : ItemSearchPlaylistAdapter.IOnClickItemListener {
                override fun onClickItem(position: Int) {
                    (activity as MainActivity).addToStackFragment(
                        AlbumDetailGroupFragment.newInstance(
                            Album(),
                            TAB_ALBUM,
                            mPlaylist[position].id,
                            true
                        ), this@AlbumsFragment
                    )

                    view_data?.visibility = View.GONE
                }
            })

        rcAlbumSearch.adapter = mAlbumAdapter
        rcFrequenciesSearch.adapter = mSongsAdapter
        rcProgramSearch.adapter = mPlaylistAdapter

        mViewModel = ViewModelProviders.of(this).get(AlbumsViewModel::class.java)

        mViewModel.getAllAlbumBasic().observe(this, Observer {
            if (it != null) {
                mAdapter.data = it
                checkPurchase(0)
            }
        })

        mViewModel.getAlbumAdvanced().observe(this, Observer {
            if (it != null) {
                mAlbumAdvanced.clear()
                for (item in it) {
                    mAlbumAdvanced.add(item)
                }
            }
        })
        mViewModel.getAlbumsHigherAbundance().observe(this, Observer {
            if (it != null) {
                mAlbumHigherAbundance.clear()
                for (item in it) {
                    mAlbumHigherAbundance.add(item)
                }
            }
        })
        mViewModel.getAlbumsHigherQuantum().observe(this, Observer {
            if (it != null) {
                mAlbumHigherQuantum.clear()
                for (item in it) {
                    mAlbumHigherQuantum.add(item)
                }
            }
        })
        mTabBasic.isSelected = true

        mContext!!.registerReceiver(
            broadcastReceiverPurchased,
            IntentFilter(Constants.BROADCAST_ACTION_PURCHASED)
        )
    }

    private var TAB_ALBUM = 0

    fun getTabAlbum(): Int {
        return TAB_ALBUM
    }

    fun updateView() {
        isPurchasedBasic = SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)
        isPurchasedAdvanced =
            SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_ADVANCED)
        isPurchasedHigherAbundance =
            SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE)
        isPurchasedHigherQuantum =
            SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_HIGH_QUANTUM)

        if (isPurchasedHigherQuantum) {
            tabHigher.visibility = View.GONE
            viewHigher.visibility = View.VISIBLE
        } else {
            viewHigher.visibility = View.GONE
            tabHigher.visibility = View.VISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkPurchase(typeAlbum: Int) {//isAdvanced: Boolean
        when (typeAlbum) {
            0 -> {
                for (i in 0..mAdapter.data.size - 1) {
                    if (mAdapter.data[i].album_priority < 3) {
                        mAdapter.data[i].isPurchase = true
                    } else {
                        mAdapter.data[i].isPurchase =
                            SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)
                    }
                }
            }

            1 -> {
                for (i in 0..mAdapter.data.size - 1) {
                    mAdapter.data[i].isPurchase = SharedPreferenceHelper.getInstance()
                        .getBool(Constants.KEY_PURCHASED_ADVANCED)
                }
            }

            2 -> {
                for (i in 0..mAdapter.data.size - 1) {
                    mAdapter.data[i].isPurchase = SharedPreferenceHelper.getInstance()
                        .getBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE)
                }
            }

            3 -> {
                for (i in 0 until mAdapter.data.size) {
                    mAdapter.data[i].isPurchase = SharedPreferenceHelper.getInstance()
                        .getBool(Constants.KEY_PURCHASED_HIGH_QUANTUM)
                }
            }
        }
        updateView()
        mAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mContext!!.unregisterReceiver(broadcastReceiverPurchased)
    }

    override fun addListener() {
        edtSearch?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (edtSearch.text.isNotEmpty()) {
                    view_data?.visibility = View.VISIBLE
                    imgClear.visibility = View.VISIBLE
                    rcFrequenciesSearch.adapter = mSongsAdapter
                    rcAlbumSearch.adapter = mAlbumAdapter
                    rcProgramSearch.adapter = mPlaylistAdapter
                    mHandler.removeCallbacksAndMessages(null)
                    mHandler.postDelayed(mSearchRunnable, 100)
                } else {
                    imgClear?.visibility = View.GONE
                    view_data?.visibility = View.GONE
                    mHandler.removeCallbacksAndMessages(null)
                    rcFrequenciesSearch.adapter = mSongsAdapter
                    rcAlbumSearch.adapter = mAlbumAdapter
                    rcProgramSearch.adapter = mPlaylistAdapter
                    mSongs.clear()
                    mAlbumsSearch.clear()
                    mPlaylist.clear()
                    mAlbumAdapter.notifyDataSetChanged()
                    mSongsAdapter.notifyDataSetChanged()
                    mPlaylistAdapter.notifyDataSetChanged()
                }
            }
        })

        imgClear.setOnClickListener {
            edtSearch.setText("")
            (activity as BaseActivity).hideKeyBoard()
        }

        mTabBasic.setOnClickListener {
            mTabBasic.isSelected = true
            mTabAdvanced.isSelected = false
            tabHigher?.isSelected = false
            tabHigher2?.isSelected = false
            tabHigherQuantum?.isSelected = false
            isAdvanced = false
            mAdapter.setIsAdvanced(isAdvanced)
            TAB_ALBUM = 0
            mViewModel.getAlbumAdvanced().removeObservers(this)
            mViewModel.getAlbumsHigherAbundance().removeObservers(this)
            mViewModel.getAlbumsHigherQuantum().removeObservers(this)
            mViewModel.getAllAlbumBasic().observe(this, Observer {
                if (mTabBasic.isSelected && it != null) {
                    mAdapter.data = it
                    checkPurchase(0)
                }
            })
        }
        mTabAdvanced.setOnClickListener {
            mTabAdvanced.isSelected = true
            mTabBasic.isSelected = false
            tabHigher?.isSelected = false
            tabHigher2?.isSelected = false
            tabHigherQuantum?.isSelected = false
            isAdvanced = true
            mAdapter.setIsAdvanced(isAdvanced)
            TAB_ALBUM = 1
            mViewModel.getAllAlbumBasic().removeObservers(this)
            mViewModel.getAlbumsHigherAbundance().removeObservers(this)
            mViewModel.getAlbumsHigherQuantum().removeObservers(this)
            mViewModel.getAlbumAdvanced().observe(this, Observer {
                if (mTabAdvanced.isSelected && it != null) {
                    mAdapter.data = it
                    checkPurchase(1)
                }
            })
        }
        tabHigher?.setOnClickListener {
            mTabAdvanced.isSelected = false
            mTabBasic.isSelected = false
            tabHigher?.isSelected = true
            TAB_ALBUM = 2
            mViewModel.getAllAlbumBasic().removeObservers(this)
            mViewModel.getAlbumAdvanced().removeObservers(this)
            mViewModel.getAlbumsHigherQuantum().removeObservers(this)
            mViewModel.getAlbumsHigherAbundance().observe(this, Observer {
                if (tabHigher.isSelected && it != null) {
                    mAdapter.data = it
                    checkPurchase(2)
                }
            })
        }
        tabHigher2?.setOnClickListener {
            mTabAdvanced.isSelected = false
            mTabBasic.isSelected = false
            tabHigherQuantum.isSelected = false
            tabHigher2?.isSelected = true
            TAB_ALBUM = 2
            mViewModel.getAllAlbumBasic().removeObservers(this)
            mViewModel.getAlbumAdvanced().removeObservers(this)
            mViewModel.getAlbumsHigherQuantum().removeObservers(this)
            mViewModel.getAlbumsHigherAbundance().observe(this, Observer {
                if (tabHigher2.isSelected && it != null) {
                    mAdapter.data = it
                    checkPurchase(2)
                }
            })
        }
        tabHigherQuantum.setOnClickListener {
            mTabAdvanced.isSelected = false
            mTabBasic.isSelected = false
            tabHigher2.isSelected = false
            tabHigherQuantum?.isSelected = true
            TAB_ALBUM = 3
            mViewModel.getAllAlbumBasic().removeObservers(this)
            mViewModel.getAlbumsHigherAbundance().removeObservers(this)
            mViewModel.getAlbumAdvanced().removeObservers(this)
            mViewModel.getAlbumsHigherQuantum().observe(this, Observer {
                if (tabHigherQuantum.isSelected && it != null) {
                    mAdapter.data = it
                    checkPurchase(3)
                }
            })
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun searchTracks() {
        val database = QFDatabase.getDatabase(mContext!!.applicationContext)
        var positionPurchase = 3
        if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)) {
            if (SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_FREE ||
                SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_1_MONTH_25 ||
                SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_1_MONTH_99 ||
                SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_LIFETIME
            ) {
                positionPurchase = 7
            }
        }

        var positionPurchaseAdvanced = 0
        if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_ADVANCED)) {
            positionPurchaseAdvanced = mAlbumAdvanced.size
        }

        //search all song

        val albumIds = IntArray(positionPurchase)
        for (i in mAdapter.data.indices) {
            if (i < positionPurchase) {
                albumIds[i] = mAdapter.data[i].id.toInt()
            } else {
                break
            }
        }
        val arrs =
            database.songDAO().searchTracksByAlbum("%" + edtSearch?.text.toString() + "%", albumIds)
        mSongs.clear()
        for (item in arrs) {
            mSongs.add(item)
        }

        val albumAdvancedIds = IntArray(positionPurchaseAdvanced)
        for (i in 0 until mAlbumAdvanced.size) {
            if (i < positionPurchaseAdvanced) {
                albumAdvancedIds[i] = mAlbumAdvanced[i].id.toInt()
            } else {
                break
            }
        }
        val listSongAdvanced = database.songDAO()
            .searchTracksByAlbum("%" + edtSearch?.text.toString() + "%", albumAdvancedIds)
        mSongAdvanced.clear()
        for (item in listSongAdvanced) {
            mSongAdvanced.add(item)
        }
        mSongs.addAll(mSongAdvanced)

        var positionPurchaseHigherAbundance = 0
        if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_HIGH_ABUNDANCE)) {
            positionPurchaseHigherAbundance = mAlbumHigherAbundance.size
        }

        val albumHigherAbundanceIds = IntArray(positionPurchaseHigherAbundance)
        for (i in 0 until mAlbumHigherAbundanceSearch.size) {
            if (i < positionPurchaseHigherAbundance) {
                albumHigherAbundanceIds[i] = mAlbumHigherAbundanceSearch[i].id.toInt()
            } else {
                break
            }
        }
        val listSongHigherAbundance = database.songDAO()
            .searchTracksByAlbum("%" + edtSearch?.text.toString() + "%", albumHigherAbundanceIds)
        mSongHigherAbundance.clear()
        for (item in listSongHigherAbundance) {
            mSongHigherAbundance.add(item)
        }
        mSongs.addAll(mSongHigherAbundance)

        var positionPurchaseHigherQuantum = 0
        if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_HIGH_QUANTUM)) {
            positionPurchaseHigherQuantum = mAlbumHigherQuantum.size
        }

        val albumHigherQuantumIds = IntArray(positionPurchaseHigherQuantum)
        for (i in 0 until mAlbumHigherAbundanceSearch.size) {
            if (i < positionPurchaseHigherQuantum) {
                albumHigherQuantumIds[i] = mAlbumHigherQuantumSearch[i].id.toInt()
            } else {
                break
            }
        }
        val listSongHigherQuantum = database.songDAO()
            .searchTracksByAlbum("%" + edtSearch?.text.toString() + "%", albumHigherQuantumIds)
        mSongHigherQuantum.clear()
        for (item in listSongHigherQuantum) {
            mSongHigherQuantum.add(item)
        }
        mSongs.addAll(mSongHigherQuantum)

        mSongsAdapter.notifyDataSetChanged()

        //search all album
        val albumSearch =
            database.albumDAO().searchAlbum("%" + edtSearch?.text.toString() + "%", albumIds)
        mAlbumsSearch.clear()
        for (item in albumSearch) {
            mAlbumsSearch.add(item)
        }
        val albumsAdvanced = database.albumDAO()
            .searchAlbum("%" + edtSearch?.text.toString() + "%", albumAdvancedIds)
        mAlbumAdvancedSearch.clear()
        for (item in albumsAdvanced) {
            mAlbumAdvancedSearch.add(item)
        }
        mAlbumsSearch.addAll(mAlbumAdvancedSearch)

        val albumsHigherAbundanceIds = database.albumDAO()
            .searchAlbum("%" + edtSearch?.text.toString() + "%", albumHigherAbundanceIds)
        mAlbumHigherAbundanceSearch.clear()
        for (item in albumsHigherAbundanceIds) {
            mAlbumHigherAbundanceSearch.add(item)
        }
        mAlbumsSearch.addAll(mAlbumHigherAbundanceSearch)

        val albumsHigherQuantumIds = database.albumDAO()
            .searchAlbum("%" + edtSearch?.text.toString() + "%", albumHigherQuantumIds)
        mAlbumHigherQuantumSearch.clear()
        for (item in albumsHigherQuantumIds) {
            mAlbumHigherQuantumSearch.add(item)
        }
        mAlbumsSearch.addAll(mAlbumHigherQuantumSearch)

        mAlbumAdapter.notifyDataSetChanged()
        searchPlaylist()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun searchPlaylist() {
        val database = QFDatabase.getDatabase(mContext!!.applicationContext)
        var positionPlaylist = 3
        if (SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED)) {
            if (SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_FREE ||
                SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_1_MONTH_25 ||
                SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_1_MONTH_99 ||
                SharedPreferenceHelper.getInstance()
                    .getInt(Constants.INApp_PACKED) == Constants.INApp_LIFETIME
            ) {
                positionPlaylist = 12
            }
        }
        val playLists = database.playlistDAO().getAll()
        for (playlist in playLists) {
            if (playlist.fromUsers > 0) {
                positionPlaylist++
            }
        }

        IntArray(positionPlaylist)
        val playlists = database.playlistDAO()
            .searchPlaylist("%" + edtSearch?.text.toString() + "%")//, playlistIds
        mPlaylist.clear()
        for (item in playlists) {
            if (isUnClocked(item.mediaType!!)) {
                mPlaylist.add(item)
            }
        }
        mPlaylistAdapter.notifyDataSetChanged()
    }

    private fun isUnClocked(mediaType: Int): Boolean {
        return when {
            mediaType == Constants.MEDIA_TYPE_ADVANCED -> isPurchasedAdvanced //check advanced playlist
            mediaType < Constants.MEDIA_TYPE_ADVANCED -> {
                //check basic playlist
                if (isPurchasedBasic) {
                    mediaType == Constants.MEDIA_TYPE_BASIC || mediaType == Constants.MEDIA_TYPE_BASIC_FREE
                } else {
                    mediaType == Constants.MEDIA_TYPE_BASIC_FREE
                }
            }

            mediaType == Constants.MEDIA_TYPE_ABUNDANCE -> isPurchasedHigherAbundance
            mediaType == Constants.MEDIA_TYPE_HIGHER_QUANTUM -> isPurchasedHigherQuantum
            else -> true
        }
    }
}
