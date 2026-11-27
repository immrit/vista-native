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
import android.os.Parcel
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

    data class LaunchIntent(
        val intent: Intent,
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
    private var rawBinder: IBinder? = null

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
                rawBinder = binder
                service = iab
                deferred.complete(iab)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                rawBinder = null
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

        val safePayload = developerPayload.take(MAX_DEVELOPER_PAYLOAD_CHARS)
        val binder = rawBinder ?: iab.asBinder()
        val supportsIntentV2 = runCatching {
            queryPurchaseConfig(binder)?.getBoolean(INTENT_V2_SUPPORT, false) == true
        }.getOrDefault(false)

        val buyBundle = if (supportsIntentV2) {
            runCatching {
                queryBuyIntentV2(
                    binder = binder,
                    apiVersion = API_VERSION,
                    packageName = appContext.packageName,
                    sku = sku,
                    type = ITEM_TYPE_INAPP,
                    developerPayload = safePayload,
                )
            }.getOrNull()
        } else {
            runCatching {
                iab.getBuyIntent(API_VERSION, appContext.packageName, sku, ITEM_TYPE_INAPP, safePayload)
            }.getOrNull()
        } ?: return@withContext MyketBuyLaunchResult.Error("درخواست پرداخت از مایکت پاسخ نداد.")

        val code = buyBundle.getInt(RESPONSE_CODE, -1)
        if (code == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            val recovered = findOwnedPurchase(iab, sku)
            if (recovered != null) {
                return@withContext MyketBuyLaunchResult.RecoveredOwnedPurchase(recovered)
            }
            return@withContext MyketBuyLaunchResult.Error("این محصول قبلاً خریداری شده اما هنوز مصرف نشده است.")
        }
        if (code != BILLING_RESPONSE_RESULT_OK) {
            val skuHint = if (code == BILLING_RESPONSE_RESULT_ERROR) {
                inspectSkuAvailabilityHint(iab, sku)
            } else {
                null
            }
            return@withContext MyketBuyLaunchResult.Error(mapResponseError(code, skuHint))
        }

        if (supportsIntentV2) {
            val launchIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                buyBundle.getParcelable(RESPONSE_BUY_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                buyBundle.getParcelable(RESPONSE_BUY_INTENT)
            }
            if (launchIntent != null) {
                return@withContext MyketBuyLaunchResult.LaunchIntent(
                    intent = launchIntent,
                    productId = sku,
                    expectedPayload = safePayload,
                )
            }
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
            expectedPayload = safePayload,
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
        rawBinder = null
        service = null
        pendingBind?.cancel()
        pendingBind = null
    }

    private fun queryPurchaseConfig(binder: IBinder): Bundle? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(BILLING_DESCRIPTOR)
            data.writeInt(API_VERSION)
            if (!binder.transact(TRANSACTION_GET_PURCHASE_CONFIG, data, reply, 0)) {
                return null
            }
            reply.readException()
            if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun queryBuyIntentV2(
        binder: IBinder,
        apiVersion: Int,
        packageName: String,
        sku: String,
        type: String,
        developerPayload: String,
    ): Bundle? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(BILLING_DESCRIPTOR)
            data.writeInt(apiVersion)
            data.writeString(packageName)
            data.writeString(sku)
            data.writeString(type)
            data.writeString(developerPayload)
            if (!binder.transact(TRANSACTION_GET_BUY_INTENT_V2, data, reply, 0)) {
                return null
            }
            reply.readException()
            if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun inspectSkuAvailabilityHint(iab: IInAppBillingService, sku: String): String? {
        val query = Bundle().apply {
            putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, arrayListOf(sku))
        }
        val detailsBundle = runCatching {
            iab.getSkuDetails(API_VERSION, appContext.packageName, ITEM_TYPE_INAPP, query)
        }.getOrNull() ?: return "خطا در استعلام محصول از سرور مایکت؛ فیلترشکن (VPN) را خاموش کنید."
        val detailsCode = detailsBundle.getInt(RESPONSE_CODE, -1)
        if (detailsCode != BILLING_RESPONSE_RESULT_OK) {
            return "استعلام محصول ناموفق بود (کد $detailsCode)؛ اگر فیلترشکن روشن است آن را خاموش کنید."
        }
        val list = detailsBundle.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST).orEmpty()
        if (list.isEmpty()) {
            return "شناسه «$sku» در پنل مایکت ثبت/فعال نشده یا نسخه فعلی با امضای پنل مایکت مطابقت ندارد."
        }
        return null
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

    private fun mapResponseError(code: Int, diagnosticHint: String? = null): String = when (code) {
        BILLING_RESPONSE_RESULT_USER_CANCELED -> "پرداخت توسط کاربر لغو شد."
        BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE -> "ارتباط با سرور مایکت برقرار نشد."
        BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "سرویس پرداخت مایکت روی دستگاه فعال نیست."
        BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "این محصول در پنل مایکت تعریف یا فعال نشده است."
        BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> "خطا در پیکربندی شناسه محصول مایکت."
        BILLING_RESPONSE_RESULT_ERROR -> buildString {
            append("خطا در پرداخت مایکت (کد 6).")
            if (!diagnosticHint.isNullOrBlank()) {
                append(' ')
                append(diagnosticHint)
            } else {
                append(" فیلترشکن (VPN) را خاموش کنید، وارد حساب مایکت شوید و از ثبت محصول و APK در پنل مایکت مطمئن شوید.")
            }
        }
        else -> "خطا در پرداخت مایکت (کد $code)."
    }

    companion object {
        private const val API_VERSION = 3
        private const val MAX_DEVELOPER_PAYLOAD_CHARS = 700
        private const val ITEM_TYPE_INAPP = "inapp"
        private const val MYKET_PACKAGE_NAME = "ir.mservices.market"
        private const val MYKET_BIND_ACTION = "ir.mservices.market.InAppBillingService.BIND"
        private const val BILLING_DESCRIPTOR = "com.android.vending.billing.IInAppBillingService"
        private const val TRANSACTION_GET_BUY_INTENT_V2 = IBinder.FIRST_CALL_TRANSACTION + 5
        private const val TRANSACTION_GET_PURCHASE_CONFIG = IBinder.FIRST_CALL_TRANSACTION + 6
        private const val INTENT_V2_SUPPORT = "INTENT_V2_SUPPORT"
        private const val GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST"
        private const val RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST"

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
        private const val BILLING_RESPONSE_RESULT_ERROR = 6
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
