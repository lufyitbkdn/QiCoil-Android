package com.Meditation.Sounds.frequencies.lemeor.ui.albums.tabs

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.api.RetrofitBuilder
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.data.remote.ApiHelper
import com.Meditation.Sounds.frequencies.lemeor.data.utils.Resource
import com.Meditation.Sounds.frequencies.lemeor.data.utils.ViewModelFactory
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper
import com.Meditation.Sounds.frequencies.lemeor.ui.purchase.new_flow.NewPurchaseActivity
import com.Meditation.Sounds.frequencies.utils.Utils
import com.Meditation.Sounds.frequencies.views.ItemOffsetDecoration
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.share.Sharer
import com.facebook.share.widget.ShareDialog
import kotlinx.android.synthetic.main.fragment_albums_category.albums_recycler_view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AlbumsRecyclerFragment : Fragment() {
    var callbackManager: CallbackManager? = null
    var shareDialog: ShareDialog? = null
    var selectedAlbum: Album? = null

    interface AlbumsRecyclerListener {
        fun onStartAlbumDetail(album: Album)
        fun onStartLongAlbumDetail(album: Album)
    }

    private var mListener: AlbumsRecyclerListener? = null
    private lateinit var mViewModel: AlbumsViewModel
    private var mAlbumAdapter: AlbumsAdapter? = null
    private var mListAlbum = ArrayList<Album>()

    private var categoryId: Int? = null
    var isRegenerate = false
    //var  isSoothe = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = arguments?.getInt(ARG_SECTION_NUMBER)
        callbackManager = CallbackManager.Factory.create();
        shareDialog = ShareDialog(this);
        //shareDialog.registerCallback(callbackManager, FacebookCallback<Sharer.Result>() {})
        shareDialog!!.registerCallback(
                callbackManager!!,
                object : FacebookCallback<Sharer.Result?> {
                    override fun onError(e: FacebookException) {}
                    override fun onCancel() {}
                    override fun onSuccess(result: Sharer.Result?) {
                        //Toast.makeText(context,"Successs",Toast.LENGTH_LONG).show()
                        unlockAlbum(selectedAlbum!!)
                    }
                })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_albums_category, container, false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            albums_recycler_view.layoutManager = GridLayoutManager(context, 3)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            albums_recycler_view.layoutManager = GridLayoutManager(context, 2)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mViewModel = ViewModelProvider(this,
                ViewModelFactory(
                        ApiHelper(RetrofitBuilder(requireContext()).apiService),
                        DataBase.getInstance(requireContext()))
        ).get(AlbumsViewModel::class.java)

        mViewModel.albums(categoryId!!)?.observe(viewLifecycleOwner) {
            mListAlbum = it as ArrayList<Album>
            mAlbumAdapter?.setData(it)
        }

        getAlbumData()

        albums_recycler_view.setHasFixedSize(true)
        if (activity?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            albums_recycler_view.layoutManager = GridLayoutManager(context, 3)
        } else if (activity?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
            albums_recycler_view.layoutManager = GridLayoutManager(context, 2)
        }
        val itemDecoration = ItemOffsetDecoration(requireContext(),
                if (Utils.isTablet(requireContext())) R.dimen.margin_buttons else R.dimen.item_offset)
        albums_recycler_view.addItemDecoration(itemDecoration)
    }

    private fun getAlbumData() {
        mAlbumAdapter = AlbumsAdapter(requireContext(), mListAlbum, false, false)
        albums_recycler_view.adapter = mAlbumAdapter
        mAlbumAdapter!!.setOnClickListener(object : AlbumsAdapter.Listener {
            override fun onClickItem(album: Album) {
                startAlbumDetails(album)
            }

            override fun onLongClickItem(album: Album) {
               startLongAlbumDetails(album)
            }
        })

      /*  if (Utils.isConnectedToNetwork(activity)) {
            getAlbumsShareList()
        } else {
            (activity as BaseActivity).showAlert(getString(R.string.err_network_available))
        }*/


    }

    private fun getAlbumsShareList() {
        val user = PreferenceHelper.getUser(requireContext())

        mViewModel.CheckFreeAlbum("" + user?.id).observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Resource.Status.SUCCESS -> {
                        if (it.data?.data?.albums != null) {
                            for (item in it.data.data.albums) {
                                /*if (item.id == 221) {
                                    isSoothe = true
                                } else*/
                                if (item.id == 1) {
                                    isRegenerate = true
                                }
                            }
                        }

                        mAlbumAdapter = AlbumsAdapter(requireContext(), mListAlbum, isRegenerate, true)
                        albums_recycler_view.adapter = mAlbumAdapter

                        mAlbumAdapter?.setOnClickListener(object : AlbumsAdapter.Listener {
                            override fun onClickItem(album: Album) {
                                startAlbumDetails(album)
                            }

                            override fun onLongClickItem(album: Album) {
                                startLongAlbumDetails(album)
                            }
                        })
                    }
                    Resource.Status.ERROR -> {

                    }
                    Resource.Status.LOADING -> {

                    }
                }
            }
        }
    }

    fun startAlbumDetails(album: Album) {
        if (album.isUnlocked) {
            mListener?.onStartAlbumDetail(album)
        } else {
            startActivity(NewPurchaseActivity.newIntent(requireContext(), album.category_id, album.tier_id, album.id))
        }
    }

    fun startLongAlbumDetails(album: Album) {
        mListener?.onStartLongAlbumDetail(album)
    }

    private fun ShareOnTwitter() {
        val tweetIntent = Intent(Intent.ACTION_SEND)
        tweetIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
        tweetIntent.type = "text/plain"

        val packageManager = requireActivity().packageManager
        val resolvedInfoList = packageManager.queryIntentActivities(tweetIntent, PackageManager.MATCH_DEFAULT_ONLY)

        var resolved = false
        for (resolveInfo in resolvedInfoList) {
            if (resolveInfo.activityInfo.packageName.startsWith("com.twitter.android")) {
                tweetIntent.setClassName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name)
                resolved = true
                break
            }
        }
        if (resolved) {
            startActivity(tweetIntent)
        } else {
            val i = Intent()
            i.putExtra(Intent.EXTRA_TEXT, "Share")
            i.action = Intent.ACTION_VIEW
            i.data = Uri.parse("https://twitter.com/intent/tweet?text=" + getString(R.string.share_message))
            startActivity(i)
            //Toast.makeText(activity, "Twitter app isn't found", Toast.LENGTH_LONG).show()
        }

        Handler().postDelayed(Runnable {
            unlockAlbum(selectedAlbum!!)
        }, 5000)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int, listener: AlbumsRecyclerListener): AlbumsRecyclerFragment {
            return AlbumsRecyclerFragment().apply {
                mListener = listener
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    fun unlockAlbum(album: Album) {
        val albumDao = DataBase.getInstance(requireContext()).albumDao()
        CoroutineScope(Dispatchers.IO).launch {
            albumDao.setNewUnlockedById(true, album.id)
        }

        if (Utils.isConnectedToNetwork(activity)) {
            val user = PreferenceHelper.getUser(requireContext())
            SaveFreeAlbums(user?.id, album.id)
        }

        getAlbumData()
    }

    private fun SaveFreeAlbums(user_id: Int?, album_id: Int) {
        mViewModel.SaveFreeAlbum("" + user_id, "" + album_id).observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Resource.Status.SUCCESS -> {
                        Log.i("data", "d-->" + it.data)
                    }
                    Resource.Status.ERROR -> {

                    }
                    Resource.Status.LOADING -> {

                    }
                }
            }
        }
    }
}