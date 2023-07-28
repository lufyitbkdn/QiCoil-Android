package com.Meditation.Sounds.frequencies.lemeor.ui.purchase

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.database.DataBase
import com.Meditation.Sounds.frequencies.lemeor.tools.FlashSale
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper.isFlashSalePurchased
import com.Meditation.Sounds.frequencies.lemeor.tools.PreferenceHelper.preference
import com.Meditation.Sounds.frequencies.utils.Constants
import com.android.billingclient.api.*
import kotlinx.android.synthetic.main.activity_flash_sale.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class FlashSaleActivity : AppCompatActivity(), FlashSale.FlashSaleScreenInterface {

    private var billingClient: BillingClient? = null
    private var mSkuDetails: SkuDetails? = null
    private var mSkuDetailsList: List<SkuDetails>? = null

    private var fragmentList = arrayListOf(StarterFragment(), MasterFragment(), Abundance1Fragment(), Abundance2Fragment())
    private var index = Random().nextInt(fragmentList.size - 1) + 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash_sale)

        initUI()

        setUpBillingClient()

        setFragment(fragmentList[index], index)
    }

    private fun initUI() {
        FlashSale.setFSScreenInterface(this)

        flash_sale_subs_back.setOnClickListener { onBackPressed() }

        setTermsSpan()

        flash_sale_subs_continue.setOnClickListener { pay() }
    }

    private fun setTermsSpan() {
        val text = "Terms and Privacy Policy"
        val mSpannableText = SpannableString(text)
        val clickPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "http://www.tattoobookapp.com/quantumwavebiotechnology/privacy"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.WHITE
                ds.typeface = Typeface.DEFAULT_BOLD
            }
        }
        val clickTerms = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "http://www.tattoobookapp.com/quantumwavebiotechnology/terms"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.WHITE
                ds.typeface = Typeface.DEFAULT_BOLD
            }
        }
        mSpannableText.setSpan(clickTerms, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        mSpannableText.setSpan(clickPrivacy, 10, mSpannableText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        flash_sale_subs_info.text = TextUtils.concat(getString(R.string.tv_title_subscription_new), " ", mSpannableText)
        flash_sale_subs_info.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setFragment(fragment: Fragment, id: Int) {
        index = id

        supportFragmentManager.beginTransaction().replace(R.id.flash_sale_subs_container,
                fragment,
                fragment.javaClass.simpleName)
                .commit()
    }

    //region Billing System
    private fun setUpBillingClient() {
        billingClient = BillingClient.newBuilder(applicationContext)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
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

    @SuppressLint("SetTextI18n")
    private fun queryAvailableProducts() {
        val skuList = ArrayList<String>()
        skuList.add(Constants.SKU_RIFE_YEARLY_FLASHSALE)
        skuList.add(Constants.SKU_RIFE_ADVANCED_YEAR_FLASHSALE)
        skuList.add(Constants.SKU_RIFE_HIGHER_ANNUAL_FLASH_SALE)

        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            // Process the result.
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                mSkuDetailsList = skuDetailsList
                mSkuDetails = skuDetailsList[index - 1]
                mTvPriceFS.text = mSkuDetails?.price
                val d = mSkuDetails?.priceAmountMicros?.div(1000000.0)?.times(2)
                mTvPriceFrom.text = "$d ${mSkuDetails?.priceCurrencyCode}"
            }
        }
    }

    private val purchaseUpdateListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                Log.e("TAG_INAPP", "billingResult responseCode : ${billingResult.responseCode}")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handleConsumedPurchases(purchase)
                    }
                }
            }

    private fun handleConsumedPurchases(purchase: Purchase) {
        Log.e("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is $purchase")
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Handle the success of the consume operation.
                    Log.e("TAG_INAPP", "Update the appropriate tables/databases to grant user the items")

                    val db = DataBase.getInstance(applicationContext)
                    val categoryDao = db.categoryDao()
                    val albumDao = db.albumDao()
                    val trackDao = db.trackDao()

                    var categoryId: Int? = null

                    when (mSkuDetails?.sku) {
                        Constants.SKU_RIFE_YEARLY_FLASHSALE -> {
                            categoryId = 1
                        }
                        Constants.SKU_RIFE_ADVANCED_YEAR_FLASHSALE -> {
                            categoryId = 2
                        }
                        Constants.SKU_RIFE_HIGHER_ANNUAL_FLASH_SALE -> {
                            categoryId = 3
                        }
                    }

                    if (categoryId == null) {
                        Toast.makeText(applicationContext, "Critical error!", Toast.LENGTH_SHORT).show()
                        return@consumeAsync
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        categoryDao.updatePurchaseStatus(true, categoryId)
                        albumDao.setUnlockedStatusByCategoryId(true, categoryId, false)

                        albumDao.getUnlockedAlbums(true).forEach { a->
                            a.tracks.forEach { t->
                                trackDao.setTrackUnlocked(true, t.id)
                            }
                        }
                    }

                } else -> {
                    Log.e("TAG_INAPP", billingResult.debugMessage)
                }
            }
        }
    }

    private fun handleNonConsumablePurchase(purchase: Purchase) {
        Log.v("TAG_INAPP", "handlePurchase : $purchase")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    val billingResponseCode = billingResult.responseCode
                    val billingDebugMessage = billingResult.debugMessage

                    Log.v("TAG_INAPP", "response code: $billingResponseCode")
                    Log.v("TAG_INAPP", "debugMessage : $billingDebugMessage")

                }
            }
        }
    }

    private fun pay() {
        if (preference(applicationContext).isFlashSalePurchased) {
            Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT).show()
            return
        }

        mSkuDetails?.let {
            val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
            billingClient?.launchBillingFlow(this, flowParams)?.responseCode
        }?:noSKUMessage()
    }

    private fun noSKUMessage() { } //???

    override fun onFlashSaleTick(hours: String, minutes: String, seconds: String) {
        tvHours.text = hours
        tvMinutes.text = minutes
        tvSeconds.text = seconds
    }

    override fun onFlashSaleFinish() { finish() }
    //endregion

}