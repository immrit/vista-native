package ir.coffevista.vista_native.features.profile.settings.pricing

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Base64
import androidx.activity.result.IntentSenderRequest
import com.android.vending.billing.IInAppBillingService
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

data class MyketPurchaseResult(
    val productId: String,
    val purchaseToken: String,
    val packageName: String,
)

sealed interface MyketBuyLaunchResult {
    data class LaunchIntentSender(
        val request: IntentSenderRequest,
        val productId: String,
        val expectedPayload: String,
    ) : MyketBuyLaunchResult

    data class RecoveredOwnedPurchase(
        val purchase: MyketPurchaseResult,
    ) : MyketBuyLaunchResult

    data class Error(
        val message: String,
    ) : MyketBuyLaunchResult
}

sealed interface MyketActivityParseResult {
    data class Success(val purchase: MyketPurchaseResult) : MyketActivityParseResult
    data object Canceled : MyketActivityParseResult
    data class Error(val message: String) : MyketActivityParseResult
}

/**
 * Direct AIDL billing client for Myket (ir.mservices.market).
 * Handles connection binding, getBuyIntent ("inapp" per Myket specification),
 * ITEM_ALREADY_OWNED recovery via getPurchases, RSA signature verification, and consumption.
 */
class MyketBillingClient(
    private val context: Context,
    private val publicKeyBase64: String,
) {
    private val appContext = context.applicationContext

    @Volatile
    private var service: IInAppBillingService? = null

    @Volatile
    private var connection: ServiceConnection? = null

    private var pendingBind: CompletableDeferred<IInAppBillingService?>? = null

    suspend fun ensureConnected(): Result<IInAppBillingService> {
        service?.let { return Result.success(it) }

        val deferred = CompletableDeferred<IInAppBillingService?>()
        pendingBind = deferred

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val iab = IInAppBillingService.Stub.asInterface(binder)
                service = iab
                deferred.complete(iab)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                deferred.complete(null)
            }
        }
        connection = conn

        val bindIntent = Intent(MYKET_BIND_ACTION).apply {
            setPackage(MYKET_PACKAGE_NAME)
        }
        val bound = runCatching {
            appContext.bindService(bindIntent, conn, Context.BIND_AUTO_CREATE)
        }.getOrDefault(false)

        if (!bound) {
            disconnect()
            return Result.failure(
                IllegalStateException("برنامه مایکت روی دستگاه نصب نیست یا سرویس پرداخت آن در دسترس نیست."),
            )
        }

        val resolved = withTimeoutOrNull(8_000L) { deferred.await() }
        if (resolved == null) {
            disconnect()
            return Result.failure(
                IllegalStateException("اتصال به درگاه مایکت برقرار نشد. لطفاً از نصب و ورود به حساب مایکت مطمئن شوید."),
            )
        }
        return Result.success(resolved)
    }

    suspend fun startPurchase(
        sku: String,
        developerPayload: String,
    ): MyketBuyLaunchResult = withContext(Dispatchers.IO) {
        val iab = ensureConnected().getOrElse { err ->
            return@withContext MyketBuyLaunchResult.Error(
                err.message ?: "اتصال به درگاه پرداخت مایکت ناموفق بود.",
            )
        }

        val supported = runCatching {
            iab.isBillingSupported(API_VERSION, appContext.packageName, ITEM_TYPE_INAPP)
        }.getOrDefault(-1)
        if (supported != BILLING_RESPONSE_RESULT_OK) {
            return@withContext MyketBuyLaunchResult.Error(
                "پرداخت درون‌برنامه‌ای مایکت در این نسخه پشتیبانی نمی‌شود. لطفاً مایکت را به‌روزرسانی کنید.",
            )
        }

        val buyBundle = runCatching {
            iab.getBuyIntent(API_VERSION, appContext.packageName, sku, ITEM_TYPE_INAPP, developerPayload)
        }.getOrNull() ?: return@withContext MyketBuyLaunchResult.Error("درخواست پرداخت از مایکت پاسخ نداد.")

        val code = buyBundle.getInt(RESPONSE_CODE, -1)
        if (code == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            val recovered = findOwnedPurchase(iab, sku)
            if (recovered != null) {
                return@withContext MyketBuyLaunchResult.RecoveredOwnedPurchase(recovered)
            }
            return@withContext MyketBuyLaunchResult.Error("این محصول قبلاً خریداری شده اما هنوز مصرف نشده است.")
        }
        if (code != BILLING_RESPONSE_RESULT_OK) {
            return@withContext MyketBuyLaunchResult.Error(mapResponseError(code))
        }

        val pendingIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buyBundle.getParcelable(RESPONSE_BUY_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            buyBundle.getParcelable(RESPONSE_BUY_INTENT)
        }

        if (pendingIntent == null) {
            return@withContext MyketBuyLaunchResult.Error("صفحه پرداخت مایکت آماده نشد.")
        }

        MyketBuyLaunchResult.LaunchIntentSender(
            request = IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
            productId = sku,
            expectedPayload = developerPayload,
        )
    }

    fun parseActivityResult(
        resultCode: Int,
        data: Intent?,
        expectedSku: String,
    ): MyketActivityParseResult {
        if (resultCode == Activity.RESULT_CANCELED) {
            return MyketActivityParseResult.Canceled
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            return MyketActivityParseResult.Error("تراکنش درگاه مایکت تکمیل نشد.")
        }

        val responseCode = data.getIntExtra(RESPONSE_CODE, -1)
        if (responseCode == BILLING_RESPONSE_RESULT_USER_CANCELED) {
            return MyketActivityParseResult.Canceled
        }
        if (responseCode != BILLING_RESPONSE_RESULT_OK) {
            return MyketActivityParseResult.Error(mapResponseError(responseCode))
        }

        val purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA).orEmpty()
        val signature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE).orEmpty()
        if (purchaseData.isBlank()) {
            return MyketActivityParseResult.Error("اطلاعات رسید مایکت دریافت نشد.")
        }

        if (publicKeyBase64.isNotBlank() && !verifySignature(publicKeyBase64, purchaseData, signature)) {
            return MyketActivityParseResult.Error("امضای دیجیتال رسید مایکت نامعتبر است.")
        }

        val parsed = runCatching { JSONObject(purchaseData) }.getOrNull()
            ?: return MyketActivityParseResult.Error("خطا در خواندن رسید خرید مایکت.")

        val purchaseState = parsed.optInt("purchaseState", 0)
        if (purchaseState != 0) {
            return MyketActivityParseResult.Error("پرداخت در مایکت نهایی نشد.")
        }

        val sku = parsed.optString("productId").ifBlank { expectedSku }
        val token = parsed.optString("purchaseToken").trim()
        val pkg = parsed.optString("packageName").ifBlank { appContext.packageName }
        if (token.isBlank()) {
            return MyketActivityParseResult.Error("توکن تراکنش مایکت خالی است.")
        }

        return MyketActivityParseResult.Success(
            MyketPurchaseResult(
                productId = sku,
                purchaseToken = token,
                packageName = pkg,
            ),
        )
    }

    suspend fun consumePurchase(purchaseToken: String): Boolean = withContext(Dispatchers.IO) {
        if (purchaseToken.isBlank()) return@withContext true
        val iab = ensureConnected().getOrNull() ?: return@withContext false
        runCatching {
            iab.consumePurchase(API_VERSION, appContext.packageName, purchaseToken) == BILLING_RESPONSE_RESULT_OK
        }.getOrDefault(false)
    }

    fun disconnect() {
        connection?.let { conn ->
            runCatching { appContext.unbindService(conn) }
        }
        connection = null
        service = null
        pendingBind?.cancel()
        pendingBind = null
    }

    private fun findOwnedPurchase(iab: IInAppBillingService, sku: String): MyketPurchaseResult? {
        val ownedBundle = runCatching {
            iab.getPurchases(API_VERSION, appContext.packageName, ITEM_TYPE_INAPP, null)
        }.getOrNull() ?: return null
        if (ownedBundle.getInt(RESPONSE_CODE, -1) != BILLING_RESPONSE_RESULT_OK) return null

        val dataList = ownedBundle.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST) ?: return null
        val sigList = ownedBundle.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST).orEmpty()

        for (index in dataList.indices) {
            val raw = dataList[index]
            val sig = sigList.getOrNull(index).orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: continue
            val itemSku = json.optString("productId")
            val token = json.optString("purchaseToken").trim()
            if (itemSku == sku && token.isNotBlank()) {
                if (publicKeyBase64.isNotBlank() && !verifySignature(publicKeyBase64, raw, sig)) {
                    continue
                }
                return MyketPurchaseResult(
                    productId = itemSku,
                    purchaseToken = token,
                    packageName = json.optString("packageName").ifBlank { appContext.packageName },
                )
            }
        }
        return null
    }

    private fun mapResponseError(code: Int): String = when (code) {
        BILLING_RESPONSE_RESULT_USER_CANCELED -> "پرداخت توسط کاربر لغو شد."
        BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE -> "ارتباط با سرور مایکت برقرار نشد."
        BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "سرویس پرداخت مایکت روی دستگاه فعال نیست."
        BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "این محصول در پنل مایکت تعریف یا فعال نشده است."
        BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> "خطا در پیکربندی شناسه محصول مایکت."
        else -> "خطا در پرداخت مایکت (کد $code)."
    }

    companion object {
        private const val API_VERSION = 3
        private const val ITEM_TYPE_INAPP = "inapp"
        private const val MYKET_PACKAGE_NAME = "ir.mservices.market"
        private const val MYKET_BIND_ACTION = "ir.mservices.market.InAppBillingService.BIND"

        private const val RESPONSE_CODE = "RESPONSE_CODE"
        private const val RESPONSE_BUY_INTENT = "BUY_INTENT"
        private const val RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA"
        private const val RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE"
        private const val RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
        private const val RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"

        private const val BILLING_RESPONSE_RESULT_OK = 0
        private const val BILLING_RESPONSE_RESULT_USER_CANCELED = 1
        private const val BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2
        private const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3
        private const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4
        private const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5
        private const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7

        internal fun verifySignature(
            base64PublicKey: String,
            signedData: String,
            signature: String,
        ): Boolean {
            if (signedData.isBlank() || signature.isBlank() || base64PublicKey.isBlank()) return false
            val publicKey = runCatching {
                val decodedKey = Base64.decode(base64PublicKey.trim(), Base64.DEFAULT)
                KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decodedKey))
            }.getOrNull() ?: return false
            val signatureBytes = runCatching { Base64.decode(signature.trim(), Base64.DEFAULT) }.getOrNull() ?: return false
            val payloadBytes = signedData.toByteArray(Charsets.UTF_8)

            return listOf("SHA1withRSA", "SHA256withRSA").any { alg ->
                runCatching {
                    val sig = Signature.getInstance(alg)
                    sig.initVerify(publicKey)
                    sig.update(payloadBytes)
                    sig.verify(signatureBytes)
                }.getOrDefault(false)
            }
        }
    }
}
