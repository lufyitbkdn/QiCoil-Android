package com.Meditation.Sounds.frequencies.lemeor.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.QUANTUM_TIER_SUBS_ANNUAL_7_DAY_TRIAL
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.ui.purchase.new_flow.AlbumsPagerAdapter
import com.android.billingclient.api.*
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AppsFlyerLib
import kotlinx.android.synthetic.main.activity_trial.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TrialActivity : AppCompatActivity() {


    private val mSubsList = ArrayList<SkuDetails>()
    private var billingClient: BillingClient? = null
    private var mInapp: SkuDetails? = null
    private var mSubsAnnual: SkuDetails? = null
    private var mSubsMonth: SkuDetails? = null
    private var mSkuDetails: SkuDetails? = null
    private  val QUANTUM_SUBS_MONTH = "P1M"
    private  val QUANTUM_SUBS_YEAR = "P1Y"
    val QUANTUM_TIER_ID = 1


    override fun onCreate(savedInstanceState: Bundle?) {
         var close_btn: ImageView? = null

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trial)
        initUI()
        setUpBillingClient()
        close_btn = findViewById(R.id.btn_close)
        close_btn.setOnClickListener {
            finish()
        }
    }

    private fun initUI() {
        val albumDao = DataBase.getInstance(applicationContext).albumDao()

        CoroutineScope(Dispatchers.IO).launch {
            val albumList = ArrayList<Album>()
            albumDao.getAllAlbums()?.let { albumList.addAll(it) }
            var isAllPurchase = true
            for (album in albumList) {
                if(!album.isUnlocked) {
                    isAllPurchase = false
                    break
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
            if(isAllPurchase)
                finish()
            else {
                    val albumsPagerAdapter = AlbumsPagerAdapter(supportFragmentManager, albumList)
                    purchase_container.adapter = albumsPagerAdapter

                }
            }
        }
        purchase_container.clipToPadding = false

        orientationChangesUI(resources.configuration.orientation)

         purchase_continue.setOnClickListener {pay() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationChangesUI(newConfig.orientation)
    }

    private fun orientationChangesUI(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            purchase_container.setPadding(275, 0, 275, 0)
            purchase_container.pageMargin = 50
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val result = (width / 4.0).roundToInt()
            purchase_container.setPadding(result, 0, result, 0)
            purchase_container.pageMargin = 80
        }
    }

    private fun setUpBillingClient() {
        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(purchaseUpdateListener)
            .enablePendingPurchases()
            .build()
        startConnection()
    }

    private val purchaseUpdateListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            Log.d("TAG_INAPP", "billingResult responseCode : ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED && purchases != null) {


                val eventValues = HashMap<String, Any>()
                eventValues.put(AFInAppEventParameterName.REVENUE, 0)
                AppsFlyerLib.getInstance().logEvent(getApplicationContext(),
                    "purchase",
                    eventValues)

                for (purchase in purchases) {
                    handleConsumedPurchases(purchase)
                }
            }else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                val eventValues = HashMap<String, Any>()
                eventValues.put(AFInAppEventParameterName.REVENUE, 0)
                AppsFlyerLib.getInstance().logEvent(getApplicationContext(),
                    "cancel_purchase",
                    eventValues)
                // Handle an error caused by a user cancelling the purchase flow.
            }
        }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("TAG_INAPP", "Setup Billing Done")
                    // The BillingClient is ready. You can query purchases here.
                    queryAvailableProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("TAG_INAPP", "Billing client Disconnected")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun queryAvailableProducts() {
        val subsList = ArrayList<String>()

        subsList.add(QUANTUM_TIER_SUBS_ANNUAL_7_DAY_TRIAL)

        val subsParams = SkuDetailsParams.newBuilder()
        subsParams.setSkusList(subsList).setType(BillingClient.SkuType.SUBS)

        billingClient?.querySkuDetailsAsync(subsParams.build()) { billingResult, skuDetailsList ->
            // Process the result.
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                mSubsList.addAll(skuDetailsList)
                mSubsList.forEach {
                    if (it.subscriptionPeriod == QUANTUM_SUBS_MONTH) {
                        mSubsMonth = it
                    } else if (it.subscriptionPeriod == QUANTUM_SUBS_YEAR) {
                        mSubsAnnual = it
                    }
                }

            }
        }

    }

    private fun pay() {
        mSubsAnnual?.let { openPurchaseDialog(it) }
    }

    private fun openPurchaseDialog(skuDetails: SkuDetails) {
        mSkuDetails = skuDetails

        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        billingClient?.launchBillingFlow(this, flowParams)?.responseCode
    }


    private fun handleConsumedPurchases(purchase: Purchase) {
        Log.d("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is $purchase")
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.consumeAsync(consumeParams) { billingResult, _ ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Handle the success of the consume operation.
                    Log.d("TAG_INAPP", "Update the appropriate tables/databases to grant user the items")
                    val albumDao = DataBase.getInstance(applicationContext).albumDao()
                    CoroutineScope(Dispatchers.IO).launch {
                        when (mSkuDetails?.sku) {
                            QUANTUM_TIER_SUBS_ANNUAL_7_DAY_TRIAL -> {
                                albumDao.setNewUnlockedByTierId(
                                    true,
                                    QUANTUM_TIER_ID
                                )
                            }
                        }
                    }
                    finish()
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    // Handle the success of the consume operation.
                    Log.d("TAG_INAPP", "Update the appropriate tables/databases to grant user the items")
                    val albumDao = DataBase.getInstance(applicationContext).albumDao()
                    CoroutineScope(Dispatchers.IO).launch {
                        when (mSkuDetails?.sku) {
                            QUANTUM_TIER_SUBS_ANNUAL_7_DAY_TRIAL -> {
                                albumDao.setNewUnlockedByTierId(
                                    true,
                                    QUANTUM_TIER_ID
                                )
                            }
                        }
                    }
                    finish()
                }
                else -> {
                    Log.e("TAG_INAPP", billingResult.debugMessage)
                    finish()
                }
            }
        }
    }


}