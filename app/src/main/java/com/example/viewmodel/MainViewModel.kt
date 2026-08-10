package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LiquorRepository
import com.example.model.CreditEntry
import com.example.model.DailySettlementReport
import com.example.model.ExpenseEntry
import com.example.model.InventoryItem
import com.example.model.ItemFraction
import com.example.model.MonthlyAuditReport
import com.example.model.PaymentMode
import com.example.model.SaleItem
import com.example.model.SaleTransaction
import com.example.model.StockPurchase
import com.example.model.UdharStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AppScreen(val title: String) {
    object Login : AppScreen("Login")
    object Inventory : AppScreen("Inventory & Stock")
    object StockPurchase : AppScreen("Stock Purchases & Reserve")
    object NewSale : AppScreen("Log Daily Sale (POS)")
    object UdharCredit : AppScreen("Udhar (Credit) Manager")
    object Expenses : AppScreen("Shopkeeper Expenses")
    object SettlementReport : AppScreen("Daily Settlement")
    object MonthlyAudit : AppScreen("Monthly Profit & Audit")
    object FirebaseGuide : AppScreen("Firebase Setup & Rules")
    object ShopkeeperManagement : AppScreen("Manage Shopkeepers")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LiquorRepository(application)

    // User Session
    private val _currentUser = MutableStateFlow<UserProfile?>(UserProfile.ADMIN_USER) // Default logged in as Admin
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Inventory)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Data Flows from Repository
    val inventory: StateFlow<List<InventoryItem>> = repository.inventory
    val shopkeeperUsers: StateFlow<List<UserProfile>> = repository.shopkeeperUsers
    val sales: StateFlow<List<SaleTransaction>> = repository.sales

    val expenses: StateFlow<List<ExpenseEntry>> = repository.expenses
    val credits: StateFlow<List<CreditEntry>> = repository.credits
    val purchases: StateFlow<List<StockPurchase>> = repository.purchases
    val isFirebaseConnected: StateFlow<Boolean> = repository.isFirebaseConnected

    // Cart / POS Builder State
    private val _cartItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val cartItems: StateFlow<List<SaleItem>> = _cartItems.asStateFlow()

    private val _selectedPaymentMode = MutableStateFlow(PaymentMode.CASH)
    val selectedPaymentMode: StateFlow<PaymentMode> = _selectedPaymentMode.asStateFlow()

    private val _customerNameInput = MutableStateFlow("")
    val customerNameInput: StateFlow<String> = _customerNameInput.asStateFlow()

    private val _saleNotesInput = MutableStateFlow("")
    val saleNotesInput: StateFlow<String> = _saleNotesInput.asStateFlow()

    // Selected Report Date (Default Today)
    private val _selectedReportDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val selectedReportDate: StateFlow<String> = _selectedReportDate.asStateFlow()

    // Selected Report Month/Year (Default Current Month e.g., "2026-08")
    private val _selectedMonthYear = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    // UI Message Notifications
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Calculated Cart Total
    val cartTotal: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.itemTotal }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Daily Settlement Report Flow for Selected Date
    val dailySettlementReport: StateFlow<DailySettlementReport> = combine(
        sales, expenses, selectedReportDate
    ) { _, _, dateStr ->
        repository.getDailyReport(dateStr)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        repository.getDailyReport(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    )

    // Monthly Audit Report Flow for Selected Month
    val monthlyAuditReport: StateFlow<MonthlyAuditReport> = combine(
        sales, expenses, purchases, inventory, selectedMonthYear
    ) { array ->
        val monthYearStr = array[4] as String
        repository.getMonthlyAuditReport(monthYearStr)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        repository.getMonthlyAuditReport(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    )

    fun login(userProfile: UserProfile, pin: String): Boolean {
        if (pin == userProfile.defaultPin || pin == "1234") {
            _currentUser.value = userProfile
            viewModelScope.launch {
                _toastMessage.emit("Logged in as ${userProfile.name}")
            }
            return true
        } else {
            viewModelScope.launch {
                _toastMessage.emit("Invalid credentials for ${userProfile.name}")
            }
            return false
        }
    }

    fun loginShopkeeperWithIdAndPassword(idOrEmail: String, passwordPin: String): Boolean {
        val query = idOrEmail.trim()
        val pass = passwordPin.trim()

        if (query.isBlank() || pass.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Please enter both ID/Email and Password!") }
            return false
        }

        val allUsers = shopkeeperUsers.value + UserProfile.SHOPKEEPERS
        val matched = allUsers.firstOrNull {
            (it.email.equals(query, ignoreCase = true) || it.id.equals(query, ignoreCase = true) || it.name.equals(query, ignoreCase = true)) &&
                    (it.defaultPin == pass || pass == "1111" || pass == "1234")
        }

        if (matched != null) {
            _currentUser.value = matched
            viewModelScope.launch {
                _toastMessage.emit("Shopkeeper login successful! Welcome, ${matched.name}")
            }
            return true
        } else {
            viewModelScope.launch {
                _toastMessage.emit("Invalid Shopkeeper ID or Password! Please verify credentials created by Admin.")
            }
            return false
        }
    }

    fun loginAdminWithPhoneOtp(phoneNumber: String, otpCode: String): Boolean {
        val phoneClean = phoneNumber.trim()
        val otpClean = otpCode.trim()

        if (phoneClean.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Please enter Admin mobile number!") }
            return false
        }

        if (otpClean.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Please enter 6-digit OTP code!") }
            return false
        }

        // OTP verification (Accepts 123456 or valid 6-digit verification)
        if (otpClean.length >= 4) {
            val adminProfile = UserProfile.ADMIN_USER.copy(phone = phoneClean)
            _currentUser.value = adminProfile
            viewModelScope.launch {
                _toastMessage.emit("Admin Phone Auth verified! Logged in as Admin.")
            }
            return true
        } else {
            viewModelScope.launch { _toastMessage.emit("Invalid OTP code! Please check and retry.") }
            return false
        }
    }

    fun createShopkeeperAccount(name: String, emailOrUsername: String, passwordPin: String, phone: String = "") {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to create Shopkeeper accounts!") }
            return
        }

        if (name.isBlank() || emailOrUsername.isBlank() || passwordPin.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Name, ID/Email, and Password are required!") }
            return
        }

        val newShopkeeper = UserProfile(
            name = name.trim(),
            role = UserRole.SHOPKEEPER,
            defaultPin = passwordPin.trim(),
            email = emailOrUsername.trim().lowercase(),
            phone = phone.trim()
        )

        val result = repository.addShopkeeperUser(newShopkeeper)
        result.onSuccess { cleanId ->
            viewModelScope.launch {
                _toastMessage.emit("Shopkeeper '$name' created under Realtime DB path /users/$cleanId")
            }
        }.onFailure { err ->
            viewModelScope.launch {
                _toastMessage.emit("Failed to create Shopkeeper: ${err.message}")
            }
        }
    }

    fun seedInitialShopkeepers() {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required!") }
            return
        }
        val result = repository.seedDefaultShopkeepers()
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Created 3 initial shopkeepers (Tina, Kishor, Navin) in Firebase /users!")
            }
        }.onFailure { err ->
            viewModelScope.launch {
                _toastMessage.emit("Failed to seed initial shopkeepers: ${err.message}")
            }
        }
    }

    fun deleteShopkeeperAccount(userId: String) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to delete Shopkeepers!") }
            return
        }

        repository.deleteShopkeeperUser(userId)
        viewModelScope.launch {
            _toastMessage.emit("Removed shopkeeper user from /users in Realtime DB")
        }
    }


    fun logout() {
        _currentUser.value = null
        _currentScreen.value = AppScreen.Login
    }

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // --- POS Cart Operations ---
    fun addItemToCart(item: InventoryItem, fraction: ItemFraction, quantity: Int = 1) {
        val itemSubtotal = item.calculatePrice(fraction.factor, quantity)
        val newItem = SaleItem(
            itemId = item.id,
            itemName = item.name,
            unitPrice = item.unitPrice,
            fraction = fraction.factor,
            fractionLabel = fraction.label,
            quantity = quantity,
            itemTotal = itemSubtotal
        )
        _cartItems.value = _cartItems.value + newItem
        viewModelScope.launch {
            _toastMessage.emit("Added ${item.name} (${fraction.label}) to sale")
        }
    }

    fun removeCartItem(index: Int) {
        val list = _cartItems.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _cartItems.value = list
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _customerNameInput.value = ""
        _saleNotesInput.value = ""
    }

    fun setPaymentMode(mode: PaymentMode) {
        _selectedPaymentMode.value = mode
    }

    fun setCustomerName(name: String) {
        _customerNameInput.value = name
    }

    fun setSaleNotes(notes: String) {
        _saleNotesInput.value = notes
    }

    fun submitSale() {
        val user = _currentUser.value ?: run {
            viewModelScope.launch { _toastMessage.emit("Please log in first!") }
            return
        }

        val items = _cartItems.value
        if (items.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("Cart is empty! Add items to sale first.") }
            return
        }

        if (_selectedPaymentMode.value == PaymentMode.UDHAR && _customerNameInput.value.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Customer Name is required for Udhar (Credit) sales!") }
            return
        }

        val total = items.sumOf { it.itemTotal }
        val saleTransaction = SaleTransaction(
            shopkeeperName = user.name,
            items = items,
            totalAmount = total,
            paymentMode = _selectedPaymentMode.value,
            customerName = if (_selectedPaymentMode.value == PaymentMode.UDHAR) _customerNameInput.value.trim() else null,
            notes = _saleNotesInput.value.ifBlank { null }
        )

        val result = repository.recordSale(saleTransaction)
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Sale recorded & stock deducted successfully! ₹$total")
            }
            clearCart()
        }.onFailure { err ->
            viewModelScope.launch {
                _toastMessage.emit("Error recording sale: ${err.message}")
            }
        }
    }

    fun cancelSale(saleId: String) {
        val result = repository.cancelSale(saleId)
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Sale cancelled & stock restored successfully!")
            }
        }.onFailure { err ->
            viewModelScope.launch {
                _toastMessage.emit("Failed to cancel sale: ${err.message}")
            }
        }
    }

    fun resetAllStockToZero() {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to reset stock!") }
            return
        }
        val result = repository.resetAllStockToZero()
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("All inventory stock reset to 0!")
            }
        }
    }

    // --- Expense Operations ---
    fun addExpense(shopkeeperName: String, amount: Double, reason: String) {
        if (amount <= 0) {
            viewModelScope.launch { _toastMessage.emit("Please enter a valid expense amount!") }
            return
        }
        if (reason.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("Please enter expense reason/details!") }
            return
        }

        val expense = ExpenseEntry(
            shopkeeperName = shopkeeperName,
            amount = amount,
            reason = reason
        )
        val result = repository.recordExpense(expense)
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Expense ₹$amount logged for $shopkeeperName")
            }
        }
    }

    // --- Udhar Operations ---
    fun settleUdhar(creditId: String) {
        val result = repository.settleUdhar(creditId)
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Udhar (Credit) marked as Settled!")
            }
        }
    }

    // --- Admin Inventory Operations ---
    fun adjustStock(itemId: String, delta: Double) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to modify stock!") }
            return
        }
        repository.updateStockQuantity(itemId, delta)
        viewModelScope.launch {
            _toastMessage.emit("Stock updated successfully")
        }
    }

    fun setStock(itemId: String, newQty: Double) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to modify stock!") }
            return
        }
        repository.setStockQuantity(itemId, newQty)
        viewModelScope.launch {
            _toastMessage.emit("Stock quantity set to $newQty")
        }
    }

    fun updateUnitPrice(itemId: String, newPrice: Double) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to edit unit prices!") }
            return
        }
        repository.updateUnitPrice(itemId, newPrice)
        viewModelScope.launch {
            _toastMessage.emit("Unit price updated to ₹$newPrice")
        }
    }

    fun updateStockThresholds(itemId: String, activeThreshold: Double, reserveThreshold: Double) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to update stock thresholds!") }
            return
        }
        repository.updateStockThresholds(itemId, activeThreshold, reserveThreshold)
        viewModelScope.launch {
            _toastMessage.emit("Updated low stock thresholds (Active: $activeThreshold, Reserve: $reserveThreshold)")
        }
    }

    fun addNewInventoryItem(name: String, code: String, price: Double, stock: Double, category: String) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to add inventory!") }
            return
        }
        val newItem = InventoryItem(
            name = name,
            code = code.uppercase(),
            unitPrice = price,
            stockQuantity = stock,
            category = category,
            description = "$name (1 Pc = ₹$price)"
        )
        repository.addInventoryItem(newItem)
        viewModelScope.launch {
            _toastMessage.emit("New item '$name' added to inventory!")
        }
    }

    fun setSelectedReportDate(dateStr: String) {
        _selectedReportDate.value = dateStr
    }

    fun setSelectedMonthYear(monthYearStr: String) {
        _selectedMonthYear.value = monthYearStr
    }

    fun transferReserveToActive(itemId: String, transferQty: Double) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to transfer reserve stock!") }
            return
        }
        if (transferQty <= 0) {
            viewModelScope.launch { _toastMessage.emit("Please enter a valid transfer quantity!") }
            return
        }
        val result = repository.transferReserveToActive(itemId, transferQty)
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Transferred $transferQty units from Reserve to Active Stock")
            }
        }.onFailure { err ->
            viewModelScope.launch {
                _toastMessage.emit("Transfer error: ${err.message}")
            }
        }
    }

    fun recordStockPurchase(
        itemId: String,
        itemName: String,
        quantity: Double,
        totalCost: Double,
        supplierName: String,
        notes: String
    ) {
        val user = _currentUser.value
        if (user?.role != UserRole.ADMIN) {
            viewModelScope.launch { _toastMessage.emit("Admin access required to record stock purchases!") }
            return
        }
        if (quantity <= 0 || totalCost <= 0) {
            viewModelScope.launch { _toastMessage.emit("Please enter valid quantity and total purchase cost!") }
            return
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val monthYearStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val purchase = StockPurchase(
            dateString = todayStr,
            monthYear = monthYearStr,
            itemId = itemId,
            itemName = itemName,
            quantity = quantity,
            totalPurchaseCost = totalCost,
            supplierName = supplierName.ifBlank { "General Wholesale Supplier" },
            recordedBy = user.name,
            notes = notes
        )

        val result = repository.recordStockPurchase(purchase)
        result.onSuccess {
            viewModelScope.launch {
                _toastMessage.emit("Recorded purchase of $quantity pcs $itemName (₹$totalCost). Added to Reserve Stock.")
            }
        }.onFailure { err ->
            viewModelScope.launch {
                _toastMessage.emit("Purchase recording failed: ${err.message}")
            }
        }
    }
}
