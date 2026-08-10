package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.UserHeaderBadge
import com.example.ui.screens.CreditUdharScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.FirebaseSetupScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MonthlyAuditScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettlementReportScreen
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.IconButton
import com.example.model.UserRole
import com.example.ui.screens.ShopkeeperManagementScreen
import com.example.ui.screens.StockPurchaseScreen
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val icon: ImageVector
)

val NAV_ITEMS = listOf(
    NavItem(AppScreen.Inventory, "Inventory", Icons.Default.Inventory2),
    NavItem(AppScreen.StockPurchase, "Purchases", Icons.Default.AddBusiness),
    NavItem(AppScreen.NewSale, "Log Sale", Icons.Default.PointOfSale),
    NavItem(AppScreen.UdharCredit, "Udhar", Icons.Default.CreditCard),
    NavItem(AppScreen.Expenses, "Expenses", Icons.Default.ReceiptLong),
    NavItem(AppScreen.SettlementReport, "Daily", Icons.Default.Analytics),
    NavItem(AppScreen.MonthlyAudit, "Audit", Icons.Default.Assessment)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val selectedPaymentMode by viewModel.selectedPaymentMode.collectAsState()
    val customerNameInput by viewModel.customerNameInput.collectAsState()
    val reportDate by viewModel.selectedReportDate.collectAsState()
    val dailyReport by viewModel.dailySettlementReport.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    val monthlyAuditReport by viewModel.monthlyAuditReport.collectAsState()
    val shopkeeperUsers by viewModel.shopkeeperUsers.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (currentUser == null || currentScreen == AppScreen.Login) {
        LoginScreen(
            onLoginSuccess = { user ->
                viewModel.selectScreen(AppScreen.Inventory)
            },
            onAdminPhoneLogin = { phone, otp ->
                viewModel.loginAdminWithPhoneOtp(phone, otp)
            },
            onShopkeeperLogin = { id, pass ->
                viewModel.loginShopkeeperWithIdAndPassword(id, pass)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isFirebaseConnected) EmeraldSuccess else GoldAccent)
                        )
                    }
                },
                actions = {
                    if (currentUser?.role == UserRole.ADMIN) {
                        IconButton(onClick = { viewModel.selectScreen(AppScreen.ShopkeeperManagement) }) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = "Manage Shopkeepers",
                                tint = GoldAccent
                            )
                        }
                    }
                    UserHeaderBadge(
                        user = currentUser,
                        onSwitchUser = { viewModel.logout() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NAV_ITEMS.forEach { navItem ->
                    val isSelected = navItem.screen == currentScreen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectScreen(navItem.screen) },
                        icon = {
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.title,
                                tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = navItem.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GoldAccent
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.Inventory -> {
                    InventoryScreen(
                        items = inventory,
                        currentUser = currentUser,
                        onAdjustStock = { itemId, delta -> viewModel.adjustStock(itemId, delta) },
                        onSetStock = { itemId, newQty -> viewModel.setStock(itemId, newQty) },
                        onUpdatePrice = { itemId, newPrice -> viewModel.updateUnitPrice(itemId, newPrice) },
                        onAddNewItem = { name, code, price, stock, category ->
                            viewModel.addNewInventoryItem(name, code, price, stock, category)
                        },
                        onTransferReserve = { itemId, qty -> viewModel.transferReserveToActive(itemId, qty) },
                        onUpdateThresholds = { itemId, activeLimit, reserveLimit ->
                            viewModel.updateStockThresholds(itemId, activeLimit, reserveLimit)
                        },
                        onResetAllStockToZero = { viewModel.resetAllStockToZero() }
                    )
                }

                AppScreen.StockPurchase -> {
                    StockPurchaseScreen(
                        inventory = inventory,
                        purchases = purchases,
                        onRecordPurchase = { itemId, itemName, qty, cost, supplier, notes ->
                            viewModel.recordStockPurchase(itemId, itemName, qty, cost, supplier, notes)
                        }
                    )
                }

                AppScreen.NewSale -> {
                    SalesScreen(
                        inventory = inventory,
                        cartItems = cartItems,
                        cartTotal = cartTotal,
                        selectedPaymentMode = selectedPaymentMode,
                        customerName = customerNameInput,
                        currentUser = currentUser,
                        sales = sales,
                        onAddToCart = { item, fraction, qty -> viewModel.addItemToCart(item, fraction, qty) },
                        onRemoveFromCart = { index -> viewModel.removeCartItem(index) },
                        onClearCart = { viewModel.clearCart() },
                        onPaymentModeChange = { mode -> viewModel.setPaymentMode(mode) },
                        onCustomerNameChange = { name -> viewModel.setCustomerName(name) },
                        onSubmitSale = { viewModel.submitSale() },
                        onCancelSale = { saleId -> viewModel.cancelSale(saleId) }
                    )
                }

                AppScreen.UdharCredit -> {
                    CreditUdharScreen(
                        credits = credits,
                        currentUser = currentUser,
                        onSettleUdhar = { creditId -> viewModel.settleUdhar(creditId) },
                        onAddUdhar = { entry -> viewModel.repository.recordUdhar(entry) }
                    )
                }

                AppScreen.Expenses -> {
                    ExpenseScreen(
                        expenses = expenses,
                        currentUser = currentUser,
                        onAddExpense = { shopkeeper, amount, reason ->
                            viewModel.addExpense(shopkeeper, amount, reason)
                        }
                    )
                }

                AppScreen.SettlementReport -> {
                    SettlementReportScreen(
                        report = dailyReport,
                        selectedDate = reportDate,
                        onDateChange = { newDate -> viewModel.setSelectedReportDate(newDate) }
                    )
                }

                AppScreen.MonthlyAudit -> {
                    MonthlyAuditScreen(
                        report = monthlyAuditReport,
                        selectedMonthYear = selectedMonthYear,
                        onMonthSelected = { newMonth -> viewModel.setSelectedMonthYear(newMonth) }
                    )
                }

                AppScreen.FirebaseGuide -> {
                    FirebaseSetupScreen(
                        isConnected = isFirebaseConnected
                    )
                }

                AppScreen.ShopkeeperManagement -> {
                    ShopkeeperManagementScreen(
                        currentUser = currentUser,
                        shopkeeperList = shopkeeperUsers,
                        onCreateShopkeeper = { name, email, pass, phone ->
                            viewModel.createShopkeeperAccount(name, email, pass, phone)
                        },
                        onDeleteShopkeeper = { userId ->
                            viewModel.deleteShopkeeperAccount(userId)
                        },
                        onSeedDefaultShopkeepers = {
                            viewModel.seedInitialShopkeepers()
                        }
                    )
                }

                AppScreen.Login -> {
                    LoginScreen(
                        onLoginSuccess = { viewModel.selectScreen(AppScreen.Inventory) },
                        onAdminPhoneLogin = { phone, otp -> viewModel.loginAdminWithPhoneOtp(phone, otp) },
                        onShopkeeperLogin = { id, pass -> viewModel.loginShopkeeperWithIdAndPassword(id, pass) }
                    )
                }
            }
        }
    }
}
