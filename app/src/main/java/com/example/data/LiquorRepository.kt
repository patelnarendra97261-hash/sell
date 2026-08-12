package com.example.data //

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.CreditEntry
import com.example.model.DailySettlementReport
import com.example.model.ExpenseEntry
import com.example.model.InventoryItem
import com.example.model.MonthlyAuditReport
import com.example.model.MonthlyItemSummary
import com.example.model.PaymentMode
import com.example.model.SaleItem
import com.example.model.SaleTransaction
import com.example.model.ShopkeeperSettlement
import com.example.model.StockPurchase
import com.example.model.UdharStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LiquorRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("liquor_realtime_cache_v4", Context.MODE_PRIVATE)

    private val _inventory = MutableStateFlow<List<InventoryItem>>(loadInventoryFromLocalCache() ?: InventoryItem.DEFAULTS)
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _shopkeeperUsers = MutableStateFlow<List<UserProfile>>(loadUsersFromLocalCache() ?: UserProfile.SHOPKEEPERS)
    val shopkeeperUsers: StateFlow<List<UserProfile>> = _shopkeeperUsers.asStateFlow()

    private val _sales = MutableStateFlow<List<SaleTransaction>>(emptyList())
    val sales: StateFlow<List<SaleTransaction>> = _sales.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseEntry>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntry>> = _expenses.asStateFlow()

    private val _credits = MutableStateFlow<List<CreditEntry>>(emptyList())
    val credits: StateFlow<List<CreditEntry>> = _credits.asStateFlow()

    private val _purchases = MutableStateFlow<List<StockPurchase>>(emptyList())
    val purchases: StateFlow<List<StockPurchase>> = _purchases.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var realtimeDb: FirebaseDatabase? = null

    init {
        seedSampleDataIfEmpty()
        initFirebaseIfAvailable()
    }

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val hardware = Build.HARDWARE
        val product = Build.PRODUCT
        val brand = Build.BRAND
        val device = Build.DEVICE

        return (fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("sdk_gphone")
                || fingerprint.contains("emulator")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || model.contains("sdk_gphone")
                || model.contains("sdk_x86")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("cuttlefish")
                || hardware.contains("vbox86")
                || Build.MANUFACTURER.contains("Genymotion")
                || product.contains("sdk")
                || product.contains("google_sdk")
                || product.contains("emulator")
                || product.contains("gphone")
                || (brand.startsWith("generic") && device.startsWith("generic")))
    }

    private fun initFirebaseIfAvailable() {
        try {
            if (isEmulator()) {
                Log.i("LiquorRepository", "Running on Android Emulator. Utilizing high-performance local persistent cache.")
                _isFirebaseConnected.value = false
                firestore = null
                realtimeDb = null
                return
            }

            val isGmsInstalled = try {
                context.packageManager.getPackageInfo("com.google.android.gms", 0) != null
            } catch (e: Throwable) {
                false
            }

            if (!isGmsInstalled) {
                _isFirebaseConnected.value = false
                firestore = null
                realtimeDb = null
                return
            }

            val gmsCode = try {
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            } catch (e: Throwable) {
                ConnectionResult.SERVICE_MISSING
            }

            if (gmsCode != ConnectionResult.SUCCESS) {
                _isFirebaseConnected.value = false
                firestore = null
                realtimeDb = null
                return
            }

            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val dbUrl = "https://dixit-14de0-default-rtdb.firebaseio.com/"
            val rdb = try {
                FirebaseDatabase.getInstance()
            } catch (e: Throwable) {
                try { FirebaseDatabase.getInstance(dbUrl) } catch (e2: Throwable) { null }
            }

            if (rdb != null) {
                try { rdb.setPersistenceEnabled(true) } catch (t: Throwable) {}
                realtimeDb = rdb
            }

            try {
                val instance = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                instance.firestoreSettings = settings
                firestore = instance
            } catch (e: Throwable) {
                firestore = null
            }

            if (realtimeDb != null || firestore != null) {
                _isFirebaseConnected.value = true
                listenToRealtimeDatabase()
                listenToFirestoreCollections()
            } else {
                _isFirebaseConnected.value = false
            }
        } catch (e: Throwable) {
            _isFirebaseConnected.value = false
            firestore = null
            realtimeDb = null
        }
    }

    private fun listenToRealtimeDatabase() {
        val db = realtimeDb ?: return

        // 1. Inventory Realtime Sync
        try {
            db.getReference("inventory").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        val itemsList = mutableListOf<InventoryItem>()
                        for (child in snapshot.children) {
                            try {
                                val itemKey = child.key ?: ""
                                val name = child.child("name").getValue(String::class.java) ?: itemKey
                                val code = child.child("code").getValue(String::class.java) ?: itemKey.uppercase()
                                val unitPrice = child.child("unitPrice").getValue(Double::class.java) ?: 0.0
                                val stockQty = child.child("stockQuantity").getValue(Double::class.java) ?: 0.0
                                val reserveQty = child.child("reserveStockQuantity").getValue(Double::class.java) ?: 0.0
                                val lowActive = child.child("lowActiveStockThreshold").getValue(Double::class.java) ?: 10.0
                                val lowReserve = child.child("lowReserveStockThreshold").getValue(Double::class.java) ?: 20.0
                                val category = child.child("category").getValue(String::class.java) ?: "Liquor"
                                val desc = child.child("description").getValue(String::class.java) ?: ""
                                val lastUpdated = child.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()

                                itemsList.add(
                                    InventoryItem(
                                        id = itemKey,
                                        name = name,
                                        code = code,
                                        unitPrice = unitPrice,
                                        stockQuantity = stockQty,
                                        reserveStockQuantity = reserveQty,
                                        lowActiveStockThreshold = lowActive,
                                        lowReserveStockThreshold = lowReserve,
                                        category = category,
                                        description = desc,
                                        lastUpdated = lastUpdated
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("LiquorRepository", "Error parsing item", e)
                            }
                        }
                        if (itemsList.isNotEmpty()) {
                            _inventory.value = itemsList
                            saveInventoryToLocalCache(itemsList)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (t: Throwable) {}

        // 2. Udhar Realtime Sync
        try {
            db.getReference("udhar").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        val creditList = mutableListOf<CreditEntry>()
                        for (child in snapshot.children) {
                            try {
                                val id = child.key ?: UUID.randomUUID().toString()
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                val dateStr = child.child("dateString").getValue(String::class.java) ?: ""
                                val customerName = child.child("customerName").getValue(String::class.java) ?: ""
                                val customerPhone = child.child("customerPhone").getValue(String::class.java) ?: ""
                                val amount = child.child("amount").getValue(Double::class.java) ?: 0.0
                                val skName = child.child("shopkeeperName").getValue(String::class.java) ?: ""
                                val saleId = child.child("saleId").getValue(String::class.java) ?: ""
                                val itemSummary = child.child("itemSummary").getValue(String::class.java) ?: ""
                                val statusStr = child.child("status").getValue(String::class.java) ?: "PENDING"
                                val status = try { UdharStatus.valueOf(statusStr) } catch (e: Exception) { UdharStatus.PENDING }
                                val settledTs = child.child("settledTimestamp").getValue(Long::class.java)

                                creditList.add(CreditEntry(id, timestamp, dateStr, customerName, customerPhone, amount, skName, saleId, itemSummary, status, settledTs))
                            } catch (e: Exception) {
                                Log.e("LiquorRepository", "Error parsing udhar", e)
                            }
                        }
                        if (creditList.isNotEmpty()) {
                            _credits.value = creditList.sortedByDescending { it.timestamp }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (t: Throwable) {}
    }

    // --- ૧. RESERVE STOCK TRANSFER LOGIC ---
    fun transferReserveToActive(itemId: String, transferQty: Double): Result<Unit> {
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }

        if (index == -1) {
            return Result.failure(Exception("આઇટમ શોધી શકાઈ નથી!"))
        }

        val item = currentList[index]
        if (item.reserveStockQuantity < transferQty) {
            return Result.failure(Exception("રિઝર્વ સ્ટોકમાં પૂરતી બોટલો ઉપલબ્ધ નથી! (ઉપલબ્ધ: ${item.reserveStockQuantity})"))
        }

        val updatedItem = item.copy(
            stockQuantity = item.stockQuantity + transferQty,
            reserveStockQuantity = item.reserveStockQuantity - transferQty,
            lastUpdated = System.currentTimeMillis()
        )

        currentList[index] = updatedItem
        _inventory.value = currentList
        saveInventoryToLocalCache(currentList)

        // Firebase Sync
        realtimeDb?.getReference("inventory")?.child(itemId)?.setValue(updatedItem)

        return Result.success(Unit)
    }

    // --- ૨. PARTIAL UDHAR PAYMENT LOGIC ---
    fun recordUdharPayment(creditId: String, paidAmount: Double): Result<Double> {
        val currentList = _credits.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == creditId }

        if (index == -1) {
            return Result.failure(Exception("ઉધાર એન્ટ્રી મળી નથી!"))
        }

        val credit = currentList[index]
        val remainingAmount = credit.amount - paidAmount

        val updatedCredit = if (remainingAmount <= 0) {
            credit.copy(
                amount = 0.0,
                status = UdharStatus.SETTLED,
                settledTimestamp = System.currentTimeMillis()
            )
        } else {
            credit.copy(
                amount = remainingAmount,
                status = UdharStatus.PENDING
            )
        }

        currentList[index] = updatedCredit
        _credits.value = currentList.sortedByDescending { it.timestamp }

        // Firebase Sync
        realtimeDb?.getReference("udhar")?.child(creditId)?.setValue(updatedCredit)

        return Result.success(if (remainingAmount > 0) remainingAmount else 0.0)
    }

    // --- Local Cache Helpers ---
    private fun saveInventoryToLocalCache(items: List<InventoryItem>) {
        try {
            val array = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("code", item.code)
                obj.put("unitPrice", item.unitPrice)
                obj.put("stockQuantity", item.stockQuantity)
                obj.put("reserveStockQuantity", item.reserveStockQuantity)
                obj.put("lowActiveStockThreshold", item.lowActiveStockThreshold)
                obj.put("lowReserveStockThreshold", item.lowReserveStockThreshold)
                obj.put("category", item.category)
                obj.put("description", item.description)
                obj.put("lastUpdated", item.lastUpdated)
                array.put(obj)
            }
            prefs.edit().putString("cached_inventory_v4", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("LiquorRepository", "Failed to save local inventory cache", e)
        }
    }

    private fun loadInventoryFromLocalCache(): List<InventoryItem>? {
        val jsonStr = prefs.getString("cached_inventory_v4", null) ?: return null
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<InventoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InventoryItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        code = obj.optString("code"),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        stockQuantity = obj.optDouble("stockQuantity", 0.0),
                        reserveStockQuantity = obj.optDouble("reserveStockQuantity", 0.0),
                        lowActiveStockThreshold = obj.optDouble("lowActiveStockThreshold", 10.0),
                        lowReserveStockThreshold = obj.optDouble("lowReserveStockThreshold", 20.0),
                        category = obj.optString("category", "Liquor"),
                        description = obj.optString("description"),
                        lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            null
        }
    }

    private fun loadUsersFromLocalCache(): List<UserProfile>? = null
    private fun listenToFirestoreCollections() {}
    private fun seedSampleDataIfEmpty() {}
}
