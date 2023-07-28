package com.Meditation.Sounds.frequencies.lemeor.ui.options

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.Meditation.Sounds.frequencies.BuildConfig
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.db.QFDatabase.Companion.getDatabase
import com.Meditation.Sounds.frequencies.lemeor.*
import com.Meditation.Sounds.frequencies.lemeor.InappPurchase.*
import com.Meditation.Sounds.frequencies.lemeor.data.api.RetrofitBuilder
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.remote.ApiHelper
import com.Meditation.Sounds.frequencies.lemeor.data.utils.Resource
import com.Meditation.Sounds.frequencies.lemeor.data.utils.ViewModelFactory
import com.Meditation.Sounds.frequencies.lemeor.tools.HudHelper
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper.isLogged
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper.preference
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper.saveUser
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper.token
import com.Meditation.Sounds.frequencies.lemeor.ui.auth.AuthActivity
import com.Meditation.Sounds.frequencies.lemeor.ui.auth.updateUnlocked
import com.Meditation.Sounds.frequencies.lemeor.ui.main.NavigationActivity
import com.Meditation.Sounds.frequencies.lemeor.ui.options.change_pass.ChangePassActivity
import com.Meditation.Sounds.frequencies.lemeor.ui.purchase.new_flow.NewPurchaseActivity
import com.Meditation.Sounds.frequencies.utils.Constants
import com.Meditation.Sounds.frequencies.utils.Constants.Companion.SKU_RIFE_ADVANCED_MONTHLY
import com.Meditation.Sounds.frequencies.utils.Constants.Companion.SKU_RIFE_ADVANCED_YEAR_FLASHSALE
import com.Meditation.Sounds.frequencies.utils.Constants.Companion.SKU_RIFE_HIGHER_ANNUAL_FLASH_SALE
import com.Meditation.Sounds.frequencies.utils.Constants.Companion.SKU_RIFE_HIGHER_MONTHLY
import com.Meditation.Sounds.frequencies.utils.Constants.Companion.SKU_RIFE_MONTHLY
import com.Meditation.Sounds.frequencies.utils.Constants.Companion.SKU_RIFE_YEARLY_FLASHSALE
import com.Meditation.Sounds.frequencies.utils.Utils
import com.Meditation.Sounds.frequencies.views.DeleteUserDialog
import com.Meditation.Sounds.frequencies.views.DisclaimerDialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesResponseListener
import kotlinx.android.synthetic.main.fragment_new_options.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


const val REQUEST_CODE_AUTH = 2222

class NewOptionsFragment : Fragment() {

    private lateinit var mViewModel: NewOptionsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_options, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initUI()
    }

    private fun initUI() {
        mViewModel = ViewModelProvider(this,
                ViewModelFactory(
                        ApiHelper(RetrofitBuilder(requireContext()).apiService),
                        DataBase.getInstance(requireContext()))
        ).get(NewOptionsViewModel::class.java)

        //region Update UI
        val user = PreferenceHelper.getUser(requireContext())
        options_user_name.text = user?.name

        if (user != null) {
            options_user_name.visibility = View.VISIBLE
            options_log_out.visibility = View.VISIBLE
        }

        options_restore_purchase.setOnClickListener {
            val billingClient: BillingClient = BillingClient.newBuilder(requireContext())
                    .setListener { _, _ -> }
                    .enablePendingPurchases()
                    .build()
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                        //get purchases
                        //val subsList: List<Purchase> = billingClient.queryPurchases(BillingClient.SkuType.SUBS).purchasesList!!
                        //val inappList: List<Purchase> = billingClient.queryPurchases(BillingClient.SkuType.INAPP).purchasesList!!


                        val subsList: MutableList<com.android.billingclient.api.Purchase> = ArrayList()
                        val inappList: MutableList<com.android.billingclient.api.Purchase> = ArrayList()

                        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, PurchasesResponseListener { billingResult, mutableList -> subsList })

                        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, PurchasesResponseListener { billingResult, mutableList -> inappList })

                        if (subsList.isEmpty() && inappList.isEmpty()) {
                            if (context != null) {
                                Toast.makeText(context, "No purchases available", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val albumDao = DataBase.getInstance(requireContext()).albumDao()

                            GlobalScope.launch {
                                subsList.forEach { purchase ->
                                    when (purchase.skus.get(0)) {
                                        SKU_RIFE_MONTHLY,
                                        SKU_RIFE_YEARLY_FLASHSALE,
                                        SKU_RIFE_ADVANCED_MONTHLY,
                                        SKU_RIFE_ADVANCED_YEAR_FLASHSALE,
                                        SKU_RIFE_HIGHER_MONTHLY,
                                        SKU_RIFE_HIGHER_ANNUAL_FLASH_SALE,
                                        QUANTUM_TIER_SUBS_MONTH,
                                        QUANTUM_TIER_SUBS_ANNUAL -> {
                                            albumDao.setNewUnlockedByTierId(true, NewPurchaseActivity.QUANTUM_TIER_ID)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_WELLNESS_I.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_WELLNESS_I.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_WELLNESS_II.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_WELLNESS_II.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_WELLNESS_III.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_WELLNESS_III.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_LIFE_FORCE.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_LIFE_FORCE.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_LUCK.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_LUCK.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_SUCCESS.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_SUCCESS.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_HAPPINESS.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_HAPPINESS.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_LOVE.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_ABUNDANCE_LOVE.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_BRAIN.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_BRAIN.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_WISDOM.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_WISDOM.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_MANIFESTING.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_MANIFESTING.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_TRANSFORMATION_MEDITATION.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_TRANSFORMATION_MEDITATION.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_PROTECTION.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_PROTECTION.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_BEAUTY_I.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_BEAUTY_I.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_BEAUTY_II.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_BEAUTY_II.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_SKIN_CARE.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_SKIN_CARE.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_FITNESS.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_FITNESS.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_DMT.sku -> {
                                            //albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_DMT.Id)
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_DMT.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_AYAHUASCA.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_AYAHUASCA.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_NAD.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_NAD.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_NMN.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_NMN.categoryId)
                                        }

                                        HIGHER_QUANTUM_TIER_INAPP_DIGITAL_IVM.sku -> {
                                            albumDao.setNewUnlockedByCategoryId(true, HIGHER_QUANTUM_TIER_INAPP_DIGITAL_IVM.categoryId)
                                        }
                                    }
                                }
                            }
                            Toast.makeText(requireContext(), "Purchases have been restored", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.e("TAG_INAPP", "Billing client Disconnected")
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }

        options_instruction.setOnClickListener { startActivity(Intent(requireContext(), InstructionsActivity::class.java)) }

        options_disclaimer.setOnClickListener {
            val dialog = DisclaimerDialog(requireContext(), false,
                    object : DisclaimerDialog.IOnSubmitListener {
                        override fun submit(isCheck: Boolean) {}
                    })
            dialog.show()
            dialog.setButtonText(getString(R.string.txt_ok))
        }

        options_about.setOnClickListener {
            activity?.let {
                AlertDialog.Builder(it)
                        .setTitle(R.string.txt_about)
                        .setMessage(getString(R.string.app_name) + getString(R.string.options_version, BuildConfig.VERSION_NAME))
                        .setPositiveButton(R.string.txt_ok, null).show()
            }
        }

        options_change_pass.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePassActivity::class.java))
        }

        options_delete_user.setOnClickListener {
            onDeleteUserClick()
        }

        options_log_out.setOnClickListener {
            if (Utils.isConnectedToNetwork(requireContext())) {
                onLogoutClick()
            } else {
                showAlert(requireContext(), getString(R.string.err_network_available))
            }
        }

        options_sign_in.setOnClickListener {
            startActivityForResult(Intent(requireContext(), AuthActivity::class.java), REQUEST_CODE_AUTH)
        }

        options_help.setOnClickListener {

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@qilifestore.com")
            }
            startActivity(Intent.createChooser(emailIntent, "Send feedback"))
        }

        if (BuildConfig.IS_FREE) {
            options_flash_sale.visibility = View.GONE
            options_subscription.visibility = View.GONE
            options_restore_purchase.visibility = View.GONE
        }

        if (Constants.isGuestLogin) {
            options_delete_user.visibility = View.GONE
            options_log_out.visibility = View.GONE
            options_change_pass.visibility = View.GONE
            options_sign_in.visibility = View.VISIBLE
        } else {
            options_delete_user.visibility = View.VISIBLE
            options_log_out.visibility = View.VISIBLE
            options_change_pass.visibility = View.VISIBLE
            options_sign_in.visibility = View.GONE
        }
    }

    private fun onDeleteUserClick() {
        val dialogBuilder = AlertDialog.Builder(requireActivity())
        dialogBuilder.setMessage(getString(R.string.txt_msg_deleteuser))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.txt_no), null)
                .setPositiveButton(getString(R.string.txt_yes)) { dialog, _ ->
                    val dialog = DeleteUserDialog(activity, object : DeleteUserDialog.IOnSubmitListener {
                        override fun submit(password: String) {
                            mViewModel.deleteUser(password).observe(viewLifecycleOwner) {
                                it?.let { resource ->
                                    when (resource.status) {
                                        Resource.Status.SUCCESS -> {
                                            Toast.makeText(
                                                context,
                                                "Your account deleted successfully!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onLogoutSuccess()
                                            dialog.dismiss()
                                        }
                                        Resource.Status.ERROR -> {
                                            activity?.let { HudHelper.hide() }
                                            Toast.makeText(context, it.message, Toast.LENGTH_LONG)
                                                .show()
                                        }
                                        Resource.Status.LOADING -> {
                                            activity?.let { activity -> HudHelper.show(activity) }
                                        }
                                    }

                                }
                            }
                        }
                    })
                    dialog.show()
                }.show()
    }

    private fun onLogoutSuccess() {
        clearData()
        activity?.let { HudHelper.hide() }

        hashMapTiers = HashMap()
        val activity = activity as NavigationActivity
        isPlayProgram = false
        isPlayAlbum = false
        activity.hidePlayerUI()

        preference(requireContext()).isLogged = false
        preference(requireContext()).token = null

        val user = PreferenceHelper.getUser(requireContext())

        user?.let { updateUnlocked(requireContext(), it, false) }
        saveUser(requireContext(), null)
        initUI()

        startActivityForResult(Intent(requireContext(), AuthActivity::class.java), REQUEST_CODE_AUTH)
    }

    private fun clearData() {
        val database = DataBase.getInstance(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            database.homeDao().clear()
            database.tierDao().clear()
            database.categoryDao().clear()
            database.tagDao().clear()
            database.albumDao().clear()
            database.trackDao().clear()
            database.programDao().clear()
            database.playlistDao().clear()
        }
    }

    private fun onLogoutClick() {
        val dialogBuilder = AlertDialog.Builder(requireActivity())
        dialogBuilder.setMessage(getString(R.string.txt_msg_logout))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.txt_cancel), null)
                .setPositiveButton(getString(R.string.txt_ok)) { dialog, _ ->

                    if (options_user_name.text.equals("Guest")) {
                        onLogoutSuccess()
                        dialog.dismiss()
                    } else {
                        mViewModel.logout().observe(viewLifecycleOwner) {
                            it?.let { resource ->
                                when (resource.status) {
                                    Resource.Status.SUCCESS -> {
                                        onLogoutSuccess()
                                        dialog.dismiss()
                                    }
                                    Resource.Status.ERROR -> {
                                        activity?.let { HudHelper.hide() }
                                        Toast.makeText(context, it.message, Toast.LENGTH_LONG)
                                            .show()
                                    }
                                    Resource.Status.LOADING -> {
                                        activity?.let { activity -> HudHelper.show(activity) }
                                    }
                                }
                            }
                        }
                    }
                }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            if (requestCode == REQUEST_CODE_AUTH) {
                if (resultCode == RESULT_OK) {
                    initUI()
                }
            }
        }
    }
}