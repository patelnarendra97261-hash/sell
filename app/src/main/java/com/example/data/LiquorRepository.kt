package com.example.data

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
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        // Initialize sample sales, expenses, and credit data for immediate preview
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
                Log.w("LiquorRepository", "GMS package not installed on this device/emulator. Running with local persistent cache.")
                _isFirebaseConnected.value = false
                firestore = null
                realtimeDb = null
                return
            }

            val gmsCode = try {
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            } catch (e: SecurityException) {
                Log.w("LiquorRepository", "GMS SecurityException checking Play Services availability: ${e.message}")
                ConnectionResult.SERVICE_MISSING
            } catch (e: Throwable) {
                Log.w("LiquorRepository", "GMS error checking Play Services availability: ${e.message}")
                ConnectionResult.SERVICE_MISSING
            }

            if (gmsCode != ConnectionResult.SUCCESS) {
                Log.w("LiquorRepository", "Google Play Services unavailable on this device/emulator (code: $gmsCode). Running with local persistent cache.")
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
                try {
                    FirebaseDatabase.getInstance(dbUrl)
                } catch (e2: Throwable) {
                    Log.w("LiquorRepository", "Error getting FirebaseDatabase instance: ${e2.message}")
                    null
                }
            }

            if (rdb != null) {
                try {
                    rdb.setPersistenceEnabled(true)
                } catch (t: Throwable) {
                    // Persistence already enabled or ignored
                }
                realtimeDb = rdb
            }

            // Initialize Firestore
            try {
                val instance = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                instance.firestoreSettings = settings
                firestore = instance
            } catch (e: Throwable) {
                Log.w("LiquorRepository", "Firestore setup note: ${e.message}")
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
            Log.w("LiquorRepository", "Firebase initialization exception: ${e.message}", e)
            _isFirebaseConnected.value = false
            firestore = null
            realtimeDb = null
        }
    }

    // --- Realtime Database Listeners (/inventory, /users, /sales, /expenses, /udhar, /purchases) ---
    private fun listenToRealtimeDatabase() {
        val db = realtimeDb ?: return

        // 1. Listen to Realtime DB /inventory
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
                                Log.e("LiquorRepository", "Error parsing item from Realtime DB snapshot", e)
                            }
                        }
                        if (itemsList.isNotEmpty()) {
                            _inventory.value = itemsList
                            saveInventoryToLocalCache(itemsList)
                        }
                    } else {
                        // Seed Realtime DB /inventory if empty
                        InventoryItem.DEFAULTS.forEach { item ->
                            db.getReference("inventory").child(item.id).setValue(item)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LiquorRepository", "Realtime DB /inventory listener cancelled", error.toException())
                }
            })
        } catch (t: Throwable) {
            Log.w("LiquorRepository", "Failed to attach Realtime DB /inventory listener", t)
        }

        // 2. Listen to Realtime DB /users
        try {
            db.getReference("users").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        val userList = mutableListOf<UserProfile>()
                        for (child in snapshot.children) {
                            try {
                                val userId = child.key ?: UUID.randomUUID().toString()
                                val name = child.child("name").getValue(String::class.java) ?: "Shopkeeper"
                                val roleStr = child.child("role").getValue(String::class.java) ?: "SHOPKEEPER"
                                val role = if (roleStr.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.SHOPKEEPER
                                val pin = child.child("defaultPin").getValue(String::class.java)
                                    ?: child.child("password").getValue(String::class.java) ?: "1234"
                                val email = child.child("email").getValue(String::class.java)
                                    ?: child.child("username").getValue(String::class.java) ?: ""
                                val phone = child.child("phone").getValue(String::class.java) ?: ""

                                userList.add(
                                    UserProfile(
                                        id = userId,
                                        name = name,
                                        role = role,
                                        defaultPin = pin,
                                        email = email,
                                        phone = phone
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("LiquorRepository", "Error parsing user from Realtime DB snapshot", e)
                            }
                        }
                        if (userList.isNotEmpty()) {
                            _shopkeeperUsers.value = userList
                            saveUsersToLocalCache(userList)
                        }
                    } else {
                        // Seed Realtime DB /users if empty
                        UserProfile.ALL_USERS.forEach { u ->
                            val userMap = mapOf(
                                "id" to u.id,
                                "name" to u.name,
                                "role" to u.role.name,
                                "defaultPin" to u.defaultPin,
                                "password" to u.defaultPin,
                                "email" to u.email,
                                "username" to u.email,
                                "phone" to u.phone
                            )
                            db.getReference("users").child(u.id).setValue(userMap)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LiquorRepository", "Realtime DB /users listener cancelled", error.toException())
                }
            })
        } catch (t: Throwable) {
            Log.w("LiquorRepository", "Failed to attach Realtime DB /users listener", t)
        }

        // 3. Listen to Realtime DB /sales
        try {
            db.getReference("sales").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        val salesList = mutableListOf<SaleTransaction>()
                        for (child in snapshot.children) {
                            try {
                                val id = child.key ?: UUID.randomUUID().toString()
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                val dateStr = child.child("dateString").getValue(String::class.java) ?: ""
                                val skId = child.child("shopkeeperId").getValue(String::class.java) ?: ""
                                val skName = child.child("shopkeeperName").getValue(String::class.java) ?: "Shopkeeper"
                                val total = child.child("totalAmount").getValue(Double::class.java) ?: 0.0
                                val pmStr = child.child("paymentMode").getValue(String::class.java) ?: "CASH"
                                val paymentMode = try { PaymentMode.valueOf(pmStr) } catch (e: Exception) { PaymentMode.CASH }
                                val customerName = child.child("customerName").getValue(String::class.java)
                                val notes = child.child("notes").getValue(String::class.java)
                                val status = child.child("status").getValue(String::class.java) ?: "COMPLETED"

                                val items = mutableListOf<SaleItem>()
                                val itemsSnap = child.child("items")
                                if (itemsSnap.exists()) {
                                    for (itemChild in itemsSnap.children) {
                                        val itemId = itemChild.child("itemId").getValue(String::class.java) ?: ""
                                        val itemName = itemChild.child("itemName").getValue(String::class.java) ?: ""
                                        val unitPrice = itemChild.child("unitPrice").getValue(Double::class.java) ?: 0.0
                                        val fraction = itemChild.child("fraction").getValue(Double::class.java) ?: 1.0
                                        val fractionLabel = itemChild.child("fractionLabel").getValue(String::class.java) ?: "Full"
                                        val qty = itemChild.child("quantity").getValue(Int::class.java) ?: 1
                                        val itemTotal = itemChild.child("itemTotal").getValue(Double::class.java) ?: 0.0
                                        items.add(SaleItem(itemId, itemName, unitPrice, fraction, fractionLabel, qty, itemTotal))
                                    }
                                }

                                salesList.add(
                                    SaleTransaction(
                                        id = id,
                                        timestamp = timestamp,
                                        dateString = dateStr,
                                        shopkeeperId = skId,
                                        shopkeeperName = skName,
                                        items = items,
                                        totalAmount = total,
                                        paymentMode = paymentMode,
                                        customerName = customerName,
                                        notes = notes,
                                        status = status
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("LiquorRepository", "Error parsing sale from Realtime DB snapshot", e)
                            }
                        }
                        if (salesList.isNotEmpty()) {
                            _sales.value = salesList.sortedByDescending { it.timestamp }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LiquorRepository", "Realtime DB /sales listener cancelled", error.toException())
                }
            })
        } catch (t: Throwable) {
            Log.w("LiquorRepository", "Failed to attach Realtime DB /sales listener", t)
        }

        // 4. Listen to Realtime DB /expenses
        try {
            db.getReference("expenses").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        val expList = mutableListOf<ExpenseEntry>()
                        for (child in snapshot.children) {
                            try {
                                val id = child.key ?: UUID.randomUUID().toString()
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                val dateStr = child.child("dateString").getValue(String::class.java) ?: ""
                                val skName = child.child("shopkeeperName").getValue(String::class.java) ?: ""
                                val amount = child.child("amount").getValue(Double::class.java) ?: 0.0
                                val reason = child.child("reason").getValue(String::class.java) ?: ""

                                expList.add(ExpenseEntry(id, timestamp, dateStr, skName, amount, reason))
                            } catch (e: Exception) {
                                Log.e("LiquorRepository", "Error parsing expense from Realtime DB", e)
                            }
                        }
                        if (expList.isNotEmpty()) {
                            _expenses.value = expList.sortedByDescending { it.timestamp }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (t: Throwable) {
            Log.w("LiquorRepository", "Failed to attach Realtime DB /expenses listener", t)
        }

        // 5. Listen to Realtime DB /udhar
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
                                Log.e("LiquorRepository", "Error parsing udhar from Realtime DB", e)
                            }
                        }
                        if (creditList.isNotEmpty()) {
                            _credits.value = creditList.sortedByDescending { it.timestamp }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (t: Throwable) {
            Log.w("LiquorRepository", "Failed to attach Realtime DB /udhar listener", t)
        }
    }

    // --- SharedPreferences Persistent Local Disk Cache ---
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
            Log.e("LiquorRepository", "Failed to parse local inventory cache", e)
            null
        }
    }

    private fun saveUsersToLocalCache(users: List<UserProfile>) {
        try {
            val array = JSONArray()
            users.forEach { u ->
                val obj = JSONObject()
                obj.put("id", u.id)
                obj.put("name", u.name)
                obj.put("role", u.role.name)
                obj.put("defaultPin", u.defaultPin)
                obj.put("email", u.email)
                obj.put("phone", u.phone)
                array.put(obj)
            }
            prefs.edit().putString("cached_users_v3", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("LiquorRepository", "Failed to save local users cache", e)
        }
    }

    private fun loadUsersFromLocalCache(): List<UserProfile>? {
        val jsonStr = prefs.getString("cached_users_v3", null) ?: return null
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<UserProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val roleStr = obj.optString("role", "SHOPKEEPER")
                val role = if (roleStr == "ADMIN") UserRole.ADMIN else UserRole.SHOPKEEPER
                list.add(
                    UserProfile(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        role = role,
                        defaultPin = obj.optString("defaultPin", "1234"),
                        email = obj.optString("email"),
                        phone = obj.optString("phone")
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            Log.e("LiquorRepository", "Failed to parse local users cache", e)
            null
        }
    }

    private fun safeFirestoreWrite(action: (FirebaseFirestore) -> Unit) {
        val fs = firestore ?: return
        try {
            action(fs)
        } catch (e: Throwable) {
            Log.w("LiquorRepository", "Firestore write failed, using local in-memory fallback", e)
        }
    }

    private fun listenToFirestoreCollections() {
        val fs = firestore ?: return
        try {
            // Sync Sales
            fs.collection("sales").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val salesList = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(SaleTransaction::class.java) } catch (t: Throwable) { null }
                    }.sortedByDescending { it.timestamp }
                    _sales.value = salesList
                }
            }

            // Sync Expenses
            fs.collection("expenses").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val expenseList = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(ExpenseEntry::class.java) } catch (t: Throwable) { null }
                    }.sortedByDescending { it.timestamp }
                    _expenses.value = expenseList
                }
            }

            // Sync Udhar (Credits)
            fs.collection("udhar").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val creditList = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(CreditEntry::class.java) } catch (t: Throwable) { null }
                    }.sortedByDescending { it.timestamp }
                    _credits.value = creditList
                }
            }

            // Sync Stock Purchases
            fs.collection("purchases").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val purchaseList = snapshot.documents.mapNotNull { doc ->
                        try { doc.toObject(StockPurchase::class.java) } catch (t: Throwable) { null }
                    }.sortedByDescending { it.timestamp }
                    _purchases.value = purchaseList
                }
            }
        } catch (e: Throwable) {
            Log.w("LiquorRepository", "Failed to attach Firestore snapshot listeners", e)
        }
    }

    private fun seedSampleDataIfEmpty() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (_sales.value.isEmpty()) {
            val sampleSales = listOf(
                SaleTransaction(
                    id = "sale_101",
                    timestamp = System.currentTimeMillis() - 3600000 * 3,
                    dateString = todayStr,
                    shopkeeperName = "Shopkeeper 1",
                    items = listOf(
                        SaleItem("rs", "RS", 300.0, 0.5, "Half / Adtha (0.5)", 1, 150.0),
                        SaleItem("beer", "Beer", 200.0, 1.0, "Full (1.0)", 1, 200.0)
                    ),
                    totalAmount = 350.0,
                    paymentMode = PaymentMode.CASH
                ),
                SaleTransaction(
                    id = "sale_102",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    dateString = todayStr,
                    shopkeeperName = "Shopkeeper 2",
                    items = listOf(
                        SaleItem("lp", "LP", 200.0, 0.5, "Half / Adtha (0.5)", 2, 200.0)
                    ),
                    totalAmount = 200.0,
                    paymentMode = PaymentMode.UDHAR,
                    customerName = "Ramesh Kumar"
                )
            )
            _sales.value = sampleSales
        }
    }

    // --- Realtime Database Stock Updates with Database Transaction ---
    fun updateStockQuantity(itemId: String, delta: Double) {
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        val currentItem = if (index != -1) currentList[index] else null

        val newQty = ((currentItem?.stockQuantity ?: 0.0) + delta).coerceAtLeast(0.0)
        if (currentItem != null) {
            val updated = currentItem.copy(stockQuantity = newQty, lastUpdated = System.currentTimeMillis())
            currentList[index] = updated
            _inventory.value = currentList
            saveInventoryToLocalCache(currentList)
        }

        // Database Transaction on Realtime DB under /inventory/{itemId}/stockQuantity
        val db = realtimeDb
        if (db != null) {
            try {
                val stockRef = db.getReference("inventory").child(itemId).child("stockQuantity")
                stockRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val currentStock = mutableData.getValue(Double::class.java) ?: (currentItem?.stockQuantity ?: 0.0)
                        val calculated = (currentStock + delta).coerceAtLeast(0.0)
                        mutableData.value = calculated
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (error != null) {
                            Log.e("LiquorRepository", "Stock Transaction failed for $itemId", error.toException())
                        }
                    }
                })
                db.getReference("inventory").child(itemId).child("lastUpdated").setValue(System.currentTimeMillis())
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to execute Realtime DB Transaction for $itemId", t)
            }
        }

        // Backup write to Firestore
        if (currentItem != null) {
            val updated = currentItem.copy(stockQuantity = newQty, lastUpdated = System.currentTimeMillis())
            safeFirestoreWrite { fs -> fs.collection("inventory").document(itemId).set(updated) }
        }
    }

    fun setStockQuantity(itemId: String, newQty: Double) {
        val validQty = newQty.coerceAtLeast(0.0)
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = currentList[index]
            val updated = item.copy(stockQuantity = validQty, lastUpdated = System.currentTimeMillis())
            currentList[index] = updated
            _inventory.value = currentList
            saveInventoryToLocalCache(currentList)

            // Database Transaction on Realtime DB under /inventory/{itemId}/stockQuantity
            val db = realtimeDb
            if (db != null) {
                try {
                    val stockRef = db.getReference("inventory").child(itemId).child("stockQuantity")
                    stockRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            mutableData.value = validQty
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            if (error != null) {
                                Log.e("LiquorRepository", "setStock Transaction failed for $itemId", error.toException())
                            }
                        }
                    })
                    db.getReference("inventory").child(itemId).child("lastUpdated").setValue(System.currentTimeMillis())
                } catch (t: Throwable) {
                    Log.w("LiquorRepository", "Failed to set stock in Realtime DB for $itemId", t)
                }
            }

            safeFirestoreWrite { fs -> fs.collection("inventory").document(itemId).set(updated) }
        }
    }

    fun updateReserveStockQuantity(itemId: String, delta: Double) {
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        val currentItem = if (index != -1) currentList[index] else null

        val newReserve = ((currentItem?.reserveStockQuantity ?: 0.0) + delta).coerceAtLeast(0.0)
        if (currentItem != null) {
            val updated = currentItem.copy(reserveStockQuantity = newReserve, lastUpdated = System.currentTimeMillis())
            currentList[index] = updated
            _inventory.value = currentList
            saveInventoryToLocalCache(currentList)
        }

        val db = realtimeDb
        if (db != null) {
            try {
                val reserveRef = db.getReference("inventory").child(itemId).child("reserveStockQuantity")
                reserveRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val curVal = mutableData.getValue(Double::class.java) ?: (currentItem?.reserveStockQuantity ?: 0.0)
                        mutableData.value = (curVal + delta).coerceAtLeast(0.0)
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (error != null) Log.e("LiquorRepository", "Reserve stock transaction error", error.toException())
                    }
                })
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Reserve stock update error", t)
            }
        }

        if (currentItem != null) {
            val updated = currentItem.copy(reserveStockQuantity = newReserve, lastUpdated = System.currentTimeMillis())
            safeFirestoreWrite { fs -> fs.collection("inventory").document(itemId).set(updated) }
        }
    }

    fun transferReserveToActive(itemId: String, transferQty: Double): Result<Boolean> {
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = currentList[index]
            if (item.reserveStockQuantity < transferQty) {
                return Result.failure(Exception("Insufficient reserve stock for ${item.name}. Reserve available: ${item.reserveStockQuantity}, Requested: $transferQty"))
            }
            val newReserve = (item.reserveStockQuantity - transferQty).coerceAtLeast(0.0)
            val newActive = item.stockQuantity + transferQty
            val updated = item.copy(
                stockQuantity = newActive,
                reserveStockQuantity = newReserve,
                lastUpdated = System.currentTimeMillis()
            )
            currentList[index] = updated
            _inventory.value = currentList
            saveInventoryToLocalCache(currentList)

            val db = realtimeDb
            if (db != null) {
                try {
                    val itemRef = db.getReference("inventory").child(itemId)
                    itemRef.child("stockQuantity").setValue(newActive)
                    itemRef.child("reserveStockQuantity").setValue(newReserve)
                    itemRef.child("lastUpdated").setValue(System.currentTimeMillis())
                } catch (t: Throwable) {
                    Log.w("LiquorRepository", "Transfer error in Realtime DB", t)
                }
            }

            safeFirestoreWrite { fs -> fs.collection("inventory").document(itemId).set(updated) }
            return Result.success(true)
        }
        return Result.failure(Exception("Item not found in inventory"))
    }

    fun recordStockPurchase(purchase: StockPurchase): Result<String> {
        val newId = if (purchase.id.isBlank()) UUID.randomUUID().toString() else purchase.id
        val finalPurchase = purchase.copy(id = newId)

        val updatedPurchases = listOf(finalPurchase) + _purchases.value
        _purchases.value = updatedPurchases

        val db = realtimeDb
        if (db != null) {
            try {
                db.getReference("purchases").child(newId).setValue(finalPurchase)
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to write purchase to Realtime DB", t)
            }
        }
        safeFirestoreWrite { fs -> fs.collection("purchases").document(newId).set(finalPurchase) }

        updateReserveStockQuantity(finalPurchase.itemId, finalPurchase.quantity)

        return Result.success(newId)
    }

    fun updateUnitPrice(itemId: String, newPrice: Double) {
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = currentList[index]
            val updated = item.copy(unitPrice = newPrice.coerceAtLeast(0.0), lastUpdated = System.currentTimeMillis())
            currentList[index] = updated
            _inventory.value = currentList
            saveInventoryToLocalCache(currentList)

            realtimeDb?.getReference("inventory")?.child(itemId)?.child("unitPrice")?.setValue(newPrice.coerceAtLeast(0.0))
            safeFirestoreWrite { fs -> fs.collection("inventory").document(itemId).set(updated) }
        }
    }

    fun updateStockThresholds(itemId: String, activeThreshold: Double, reserveThreshold: Double) {
        val currentList = _inventory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = currentList[index]
            val updated = item.copy(
                lowActiveStockThreshold = activeThreshold.coerceAtLeast(0.0),
                lowReserveStockThreshold = reserveThreshold.coerceAtLeast(0.0),
                lastUpdated = System.currentTimeMillis()
            )
            currentList[index] = updated
            _inventory.value = currentList
            saveInventoryToLocalCache(currentList)

            val db = realtimeDb
            if (db != null) {
                db.getReference("inventory").child(itemId).child("lowActiveStockThreshold").setValue(activeThreshold)
                db.getReference("inventory").child(itemId).child("lowReserveStockThreshold").setValue(reserveThreshold)
            }

            safeFirestoreWrite { fs -> fs.collection("inventory").document(itemId).set(updated) }
        }
    }

    fun addInventoryItem(item: InventoryItem) {
        val currentList = _inventory.value.toMutableList()
        val newItem = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        currentList.add(newItem)
        _inventory.value = currentList
        saveInventoryToLocalCache(currentList)

        realtimeDb?.getReference("inventory")?.child(newItem.id)?.setValue(newItem)
        safeFirestoreWrite { fs -> fs.collection("inventory").document(newItem.id).set(newItem) }
    }

    fun resetAllStockToZero(): Result<Boolean> {
        val currentList = _inventory.value.map { item ->
            item.copy(stockQuantity = 0.0, reserveStockQuantity = 0.0, lastUpdated = System.currentTimeMillis())
        }
        _inventory.value = currentList
        saveInventoryToLocalCache(currentList)

        val db = realtimeDb
        if (db != null) {
            try {
                currentList.forEach { item ->
                    val ref = db.getReference("inventory").child(item.id)
                    ref.child("stockQuantity").setValue(0.0)
                    ref.child("reserveStockQuantity").setValue(0.0)
                    ref.child("lastUpdated").setValue(System.currentTimeMillis())
                }
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to reset stock in Realtime DB", t)
            }
        }

        safeFirestoreWrite { fs ->
            currentList.forEach { item ->
                fs.collection("inventory").document(item.id).set(item)
            }
        }

        return Result.success(true)
    }

    // --- Shopkeeper Account Management under Realtime DB /users ---
    fun seedDefaultShopkeepers(): Result<Boolean> {
        val defaultList = UserProfile.SHOPKEEPERS
        val currentUsers = _shopkeeperUsers.value.toMutableList()

        defaultList.forEach { sk ->
            val existingIndex = currentUsers.indexOfFirst {
                it.id == sk.id || (sk.email.isNotBlank() && it.email.equals(sk.email, ignoreCase = true)) || it.name.equals(sk.name, ignoreCase = true)
            }
            if (existingIndex != -1) {
                currentUsers[existingIndex] = sk
            } else {
                currentUsers.add(sk)
            }
        }

        _shopkeeperUsers.value = currentUsers
        saveUsersToLocalCache(currentUsers)

        val db = realtimeDb
        if (db != null) {
            try {
                defaultList.forEach { sk ->
                    val userMap = mapOf(
                        "id" to sk.id,
                        "name" to sk.name,
                        "role" to sk.role.name,
                        "defaultPin" to sk.defaultPin,
                        "password" to sk.defaultPin,
                        "email" to sk.email,
                        "username" to sk.email,
                        "phone" to sk.phone,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.getReference("users").child(sk.id).setValue(userMap)
                }
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to seed default shopkeepers to Realtime DB", t)
            }
        }

        safeFirestoreWrite { fs ->
            defaultList.forEach { sk ->
                fs.collection("users").document(sk.id).set(sk)
            }
        }

        return Result.success(true)
    }

    fun addShopkeeperUser(userProfile: UserProfile): Result<String> {
        val currentUsers = _shopkeeperUsers.value.toMutableList()
        val existingIndex = currentUsers.indexOfFirst {
            it.id == userProfile.id || (userProfile.email.isNotBlank() && it.email.equals(userProfile.email, ignoreCase = true))
        }

        val cleanId = if (userProfile.id.isBlank()) {
            "sk_" + (userProfile.email.ifBlank { UUID.randomUUID().toString().take(6) }).replace(Regex("[^a-zA-Z0-9_]"), "_")
        } else userProfile.id

        val finalUser = userProfile.copy(
            id = cleanId,
            role = UserRole.SHOPKEEPER
        )

        if (existingIndex != -1) {
            currentUsers[existingIndex] = finalUser
        } else {
            currentUsers.add(finalUser)
        }

        _shopkeeperUsers.value = currentUsers
        saveUsersToLocalCache(currentUsers)

        // Store under /users/{cleanId} in Realtime Database
        val db = realtimeDb
        if (db != null) {
            try {
                val userMap = mapOf(
                    "id" to finalUser.id,
                    "name" to finalUser.name,
                    "role" to finalUser.role.name,
                    "defaultPin" to finalUser.defaultPin,
                    "password" to finalUser.defaultPin,
                    "email" to finalUser.email,
                    "username" to finalUser.email,
                    "phone" to finalUser.phone,
                    "createdAt" to System.currentTimeMillis()
                )
                db.getReference("users").child(finalUser.id).setValue(userMap)
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to write user to Realtime DB", t)
            }
        }

        safeFirestoreWrite { fs -> fs.collection("users").document(finalUser.id).set(finalUser) }
        return Result.success(finalUser.id)
    }

    fun deleteShopkeeperUser(userId: String): Result<Boolean> {
        val currentUsers = _shopkeeperUsers.value.filter { it.id != userId }
        _shopkeeperUsers.value = currentUsers
        saveUsersToLocalCache(currentUsers)

        realtimeDb?.getReference("users")?.child(userId)?.removeValue()
        safeFirestoreWrite { fs -> fs.collection("users").document(userId).delete() }
        return Result.success(true)
    }

    // --- Sales & Real-time Stock Deduction ---
    fun recordSale(sale: SaleTransaction): Result<String> {
        val currentInventory = _inventory.value.associateBy { it.id }
        for (saleItem in sale.items) {
            val invItem = currentInventory[saleItem.itemId]
                ?: return Result.failure(Exception("Item ${saleItem.itemName} not found in inventory"))

            val totalRequiredStock = saleItem.fraction * saleItem.quantity
            if (invItem.stockQuantity < totalRequiredStock) {
                return Result.failure(Exception("Insufficient stock for ${invItem.name}. Available: ${invItem.stockQuantity}, Required: $totalRequiredStock"))
            }
        }

        val newSaleId = if (sale.id.isBlank()) UUID.randomUUID().toString() else sale.id
        val finalSale = sale.copy(id = newSaleId)

        // Deduct stock in real-time via Database Transaction
        for (saleItem in finalSale.items) {
            val deduction = saleItem.fraction * saleItem.quantity
            updateStockQuantity(saleItem.itemId, -deduction)
        }

        val updatedSales = listOf(finalSale) + _sales.value
        _sales.value = updatedSales

        val db = realtimeDb
        if (db != null) {
            try {
                db.getReference("sales").child(newSaleId).setValue(finalSale)
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to write sale to Realtime DB", t)
            }
        }
        safeFirestoreWrite { fs -> fs.collection("sales").document(newSaleId).set(finalSale) }

        if (finalSale.paymentMode == PaymentMode.UDHAR && !finalSale.customerName.isNullOrBlank()) {
            val itemSummaryStr = finalSale.items.joinToString(", ") { "${it.itemName} (${it.fractionLabel}) x${it.quantity}" }
            val udharEntry = CreditEntry(
                id = UUID.randomUUID().toString(),
                timestamp = finalSale.timestamp,
                dateString = finalSale.dateString,
                customerName = finalSale.customerName,
                customerPhone = "",
                amount = finalSale.totalAmount,
                shopkeeperName = finalSale.shopkeeperName,
                saleId = newSaleId,
                itemSummary = itemSummaryStr,
                status = UdharStatus.PENDING
            )
            recordUdhar(udharEntry)
        }

        return Result.success(newSaleId)
    }

    fun cancelSale(saleId: String): Result<Boolean> {
        val currentSales = _sales.value.toMutableList()
        val index = currentSales.indexOfFirst { it.id == saleId }
        if (index == -1) return Result.failure(Exception("Sale not found"))

        val sale = currentSales[index]
        if (sale.status == "CANCELLED") {
            return Result.failure(Exception("Sale is already cancelled"))
        }

        // 1. Restore stock quantity for each item
        for (item in sale.items) {
            val qtyToRestore = item.fraction * item.quantity
            updateStockQuantity(item.itemId, +qtyToRestore)
        }

        // 2. Mark sale status as CANCELLED
        val cancelledSale = sale.copy(status = "CANCELLED")
        currentSales[index] = cancelledSale
        _sales.value = currentSales

        // 3. Sync to Realtime DB & Firestore
        val db = realtimeDb
        if (db != null) {
            try {
                db.getReference("sales").child(saleId).child("status").setValue("CANCELLED")
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to update cancelled status in Realtime DB", t)
            }
        }
        safeFirestoreWrite { fs -> fs.collection("sales").document(saleId).set(cancelledSale) }

        // 4. If this sale had an Udhar credit entry, mark that as SETTLED/CANCELLED
        val udharItem = _credits.value.firstOrNull { it.saleId == saleId }
        if (udharItem != null) {
            settleUdhar(udharItem.id)
        }

        return Result.success(true)
    }

    fun recordExpense(expense: ExpenseEntry): Result<String> {
        val newId = if (expense.id.isBlank()) UUID.randomUUID().toString() else expense.id
        val finalExp = expense.copy(id = newId)

        val updated = listOf(finalExp) + _expenses.value
        _expenses.value = updated

        val db = realtimeDb
        if (db != null) {
            try {
                db.getReference("expenses").child(newId).setValue(finalExp)
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to write expense to Realtime DB", t)
            }
        }
        safeFirestoreWrite { fs -> fs.collection("expenses").document(newId).set(finalExp) }

        return Result.success(newId)
    }

    fun recordUdhar(credit: CreditEntry): Result<String> {
        val newId = if (credit.id.isBlank()) UUID.randomUUID().toString() else credit.id
        val finalCredit = credit.copy(id = newId)

        val updated = listOf(finalCredit) + _credits.value
        _credits.value = updated

        val db = realtimeDb
        if (db != null) {
            try {
                db.getReference("udhar").child(newId).setValue(finalCredit)
            } catch (t: Throwable) {
                Log.w("LiquorRepository", "Failed to write udhar to Realtime DB", t)
            }
        }
        safeFirestoreWrite { fs -> fs.collection("udhar").document(newId).set(finalCredit) }

        return Result.success(newId)
    }

    fun settleUdhar(creditId: String): Result<Boolean> {
        val currentList = _credits.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == creditId }
        if (index != -1) {
            val item = currentList[index]
            val updated = item.copy(status = UdharStatus.SETTLED, settledTimestamp = System.currentTimeMillis())
            currentList[index] = updated
            _credits.value = currentList

            val db = realtimeDb
            if (db != null) {
                try {
                    db.getReference("udhar").child(creditId).child("status").setValue("SETTLED")
                    db.getReference("udhar").child(creditId).child("settledTimestamp").setValue(System.currentTimeMillis())
                } catch (t: Throwable) {
                    Log.w("LiquorRepository", "Failed to update udhar status in Realtime DB", t)
                }
            }
            safeFirestoreWrite { fs -> fs.collection("udhar").document(creditId).set(updated) }
            return Result.success(true)
        }
        return Result.failure(Exception("Udhar entry not found"))
    }

    // --- Settlement Math & Reporting ---
    fun getDailyReport(dateString: String): DailySettlementReport {
        val daySales = _sales.value.filter { it.dateString == dateString && it.status != "CANCELLED" }
        val dayExpenses = _expenses.value.filter { it.dateString == dateString }

        val totalSales = daySales.sumOf { it.totalAmount }
        val cashSales = daySales.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.totalAmount }
        val udharSales = daySales.filter { it.paymentMode == PaymentMode.UDHAR }.sumOf { it.totalAmount }
        val onlineSales = daySales.filter { it.paymentMode == PaymentMode.ONLINE }.sumOf { it.totalAmount }
        val totalExpenses = dayExpenses.sumOf { it.amount }

        val netCashAccount = cashSales - totalExpenses

        val skFromUsers = _shopkeeperUsers.value.map { it.name }
        val skFromSales = daySales.map { it.shopkeeperName }
        val shopkeeperNames = (skFromUsers + skFromSales + listOf("Admin Owner", "TINA PATEL", "Shopkeeper 1", "Shopkeeper 2"))
            .filter { it.isNotBlank() }
            .distinct()

        val breakdowns = shopkeeperNames.mapNotNull { skName ->
            val skSales = daySales.filter { it.shopkeeperName == skName }
            val skExp = dayExpenses.filter { it.shopkeeperName == skName }

            val skTotal = skSales.sumOf { it.totalAmount }
            val skCash = skSales.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.totalAmount }
            val skUdhar = skSales.filter { it.paymentMode == PaymentMode.UDHAR }.sumOf { it.totalAmount }
            val skOnline = skSales.filter { it.paymentMode == PaymentMode.ONLINE }.sumOf { it.totalAmount }
            val skExpensesTotal = skExp.sumOf { it.amount }
            val skNetCash = skCash - skExpensesTotal

            val isUserExists = _shopkeeperUsers.value.any { it.name.equals(skName, ignoreCase = true) }
            // Only show breakdown if shopkeeper is a registered user OR has sales/expenses today
            if (isUserExists || skTotal > 0 || skExpensesTotal > 0) {
                ShopkeeperSettlement(
                    shopkeeperName = skName,
                    totalSales = skTotal,
                    cashSales = skCash,
                    udharSales = skUdhar,
                    onlineSales = skOnline,
                    totalExpenses = skExpensesTotal,
                    netCashInHand = skNetCash
                )
            } else null
        }

        return DailySettlementReport(
            dateString = dateString,
            totalSales = totalSales,
            totalCashSales = cashSales,
            totalUdharSales = udharSales,
            totalOnlineSales = onlineSales,
            totalExpenses = totalExpenses,
            netCashAccount = netCashAccount,
            shopkeeperBreakdowns = breakdowns
        )
    }

    fun getMonthlyAuditReport(monthYearStr: String): MonthlyAuditReport {
        val monthSales = _sales.value.filter { it.dateString.startsWith(monthYearStr) }
        val monthExpenses = _expenses.value.filter { it.dateString.startsWith(monthYearStr) }
        val monthPurchases = _purchases.value.filter { it.monthYear == monthYearStr || it.dateString.startsWith(monthYearStr) }

        val grossSales = monthSales.sumOf { it.totalAmount }
        val cashSales = monthSales.filter { it.paymentMode == PaymentMode.CASH }.sumOf { it.totalAmount }
        val udharSales = monthSales.filter { it.paymentMode == PaymentMode.UDHAR }.sumOf { it.totalAmount }
        val onlineSales = monthSales.filter { it.paymentMode == PaymentMode.ONLINE }.sumOf { it.totalAmount }

        val purchaseCost = monthPurchases.sumOf { it.totalPurchaseCost }
        val expensesCost = monthExpenses.sumOf { it.amount }
        val netProfitLoss = grossSales - (purchaseCost + expensesCost)

        val itemBreakdowns = _inventory.value.map { item ->
            MonthlyItemSummary(
                itemId = item.id,
                itemName = item.name,
                unitPrice = item.unitPrice,
                activeStockUnits = item.stockQuantity,
                reserveStockUnits = item.reserveStockQuantity,
                totalUnits = item.totalStock(),
                unsoldValuation = item.calculateUnsoldValuation()
            )
        }

        val endingActiveUnits = itemBreakdowns.sumOf { it.activeStockUnits }
        val endingReserveUnits = itemBreakdowns.sumOf { it.reserveStockUnits }
        val totalEndingUnits = itemBreakdowns.sumOf { it.totalUnits }
        val pendingSalesValue = itemBreakdowns.sumOf { it.unsoldValuation }

        val displayMonth = try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthYearStr)
            if (date != null) SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date) else monthYearStr
        } catch (e: Exception) {
            monthYearStr
        }

        val openingStockValuation = (pendingSalesValue + (grossSales / 1.15) - purchaseCost).coerceAtLeast(0.0)
        val openingStockUnits = (totalEndingUnits + (grossSales / 200.0) - (monthPurchases.sumOf { it.quantity })).coerceAtLeast(0.0)

        return MonthlyAuditReport(
            monthYear = monthYearStr,
            displayMonth = displayMonth,
            totalGrossSales = grossSales,
            totalCashSales = cashSales,
            totalUdharSales = udharSales,
            totalOnlineSales = onlineSales,
            totalPurchaseCost = purchaseCost,
            totalExpenses = expensesCost,
            netProfitLoss = netProfitLoss,
            openingStockUnits = openingStockUnits,
            openingStockValuation = openingStockValuation,
            endingActiveStockUnits = endingActiveUnits,
            endingReserveStockUnits = endingReserveUnits,
            totalEndingStockUnits = totalEndingUnits,
            pendingSalesValue = pendingSalesValue,
            itemBreakdowns = itemBreakdowns,
            purchasesList = monthPurchases
        )
    }
}
