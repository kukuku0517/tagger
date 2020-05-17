package com.project.tagger.premium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.billingclient.api.*
import com.project.tagger.R
import com.project.tagger.util.tag
import kotlinx.android.synthetic.main.activity_purchage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PurchaseActivity : AppCompatActivity() {
    companion object {
        val REPO = "REPO"
    }

    suspend fun querySkuDetails(): SkuDetailsResult {
        val skuList = ArrayList<String>()
        skuList.add("premium_upgrade")
        skuList.add("gas")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }
        return skuDetailsResult
        // Process the result.
    }

    lateinit private var billingClient: BillingClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchage)

        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener(object : PurchasesUpdatedListener {
                override fun onPurchasesUpdated(
                    p0: BillingResult?,
                    p1: MutableList<Purchase>?
                ) {

                }
            }).build()
        mBtnPurchaseUpgrade.setOnClickListener {

            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                    }
                    GlobalScope.launch {
                        val details = querySkuDetails()
                        Log.i(tag(), "$details")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })

        }


    }
}
