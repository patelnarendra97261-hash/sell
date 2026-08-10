package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InventoryItem
import com.example.model.ItemFraction
import com.example.model.PaymentMode
import com.example.model.SaleItem
import com.example.model.SaleTransaction
import com.example.model.UserProfile
import com.example.ui.components.FractionalSelectorRow
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SalesScreen(
    inventory: List<InventoryItem>,
    cartItems: List<SaleItem>,
    cartTotal: Double,
    selectedPaymentMode: PaymentMode,
    customerName: String,
    currentUser: UserProfile?,
    sales: List<SaleTransaction> = emptyList(),
    onAddToCart: (InventoryItem, ItemFraction, Int) -> Unit,
    onRemoveFromCart: (Int) -> Unit,
    onClearCart: () -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onSubmitSale: () -> Unit,
    onCancelSale: (String) -> Unit = {}
) {
    var selectedItem by remember(inventory) { mutableStateOf(inventory.firstOrNull()) }
    var selectedFraction by remember { mutableStateOf(ItemFraction.FULL) }
    var quantity by remember { mutableStateOf(1) }
    var itemDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var scannedCodeInput by remember { mutableStateOf("") }

    val filteredInventory = remember(inventory, searchQuery) {
        if (searchQuery.isBlank()) inventory
        else inventory.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val topSellingItems = remember(inventory) {
        // Take top 6 items by stock or list order as quick shortcuts
        inventory.take(6)
    }

    val calculatedPrice = remember(selectedItem, selectedFraction, quantity) {
        selectedItem?.calculatePrice(selectedFraction.factor, quantity) ?: 0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Quick POS Terminal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = GoldAccent)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Log Daily Sale (POS)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = OnSurfaceDark
                                )
                                Text(
                                    text = "Fast 1-Tap Entry for Quick Transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariantDark
                                )
                            }
                        }
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = currentUser?.name ?: "Shopkeeper",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Tap Grid for Top Items
                    Text(
                        text = "Quick Select Top Items:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topSellingItems.forEach { item ->
                            val isSelected = selectedItem?.id == item.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedItem = item
                                    },
                                color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.background,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldAccent else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.Black else OnSurfaceDark
                                        )
                                        Text(
                                            text = "₹${item.unitPrice.toInt()} • Stock: ${item.stockQuantity.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.Black.copy(alpha = 0.8f) else OnSurfaceVariantDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search & Barcode Scanner Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search liquor item name or code...", color = OnSurfaceVariantDark) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBarcodeScannerDialog = true },
                            color = GoldAccent,
                            contentColor = Color.Black
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Quick Barcode Scanner",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropdown Item Selector
                    ExposedDropdownMenuBox(
                        expanded = itemDropdownExpanded,
                        onExpandedChange = { itemDropdownExpanded = !itemDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedItem?.let { "${it.name} (1 Pc = ₹${it.unitPrice.toInt()})" } ?: "Select Item",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selected Liquor Item") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = itemDropdownExpanded,
                            onDismissRequest = { itemDropdownExpanded = false }
                        ) {
                            filteredInventory.forEach { inv ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(inv.name, fontWeight = FontWeight.Bold, color = OnSurfaceDark)
                                            Text("₹${inv.unitPrice.toInt()} (Stock: ${inv.stockQuantity.toInt()})", color = GoldAccent)
                                        }
                                    },
                                    onClick = {
                                        selectedItem = inv
                                        itemDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Portion / Fraction Selector
                    if (selectedItem != null) {
                        FractionalSelectorRow(
                            selectedFraction = selectedFraction,
                            unitPrice = selectedItem!!.unitPrice,
                            onFractionSelected = { selectedFraction = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quantity Stepper with Quick Presets
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quantity Stepper:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnSurfaceDark
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, GoldAccent),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = GoldAccent, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$quantity",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = OnSurfaceDark
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, GoldAccent),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = GoldAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Quantity Presets (+1, +5, +10)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Add:",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantDark
                            )
                            listOf(1, 5, 10).forEach { preset ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { quantity += preset },
                                    color = MaterialTheme.colorScheme.background,
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "+$preset",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calculated Price Preview & Add Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Selected Item Total:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantDark)
                            Text(
                                text = "₹${calculatedPrice.toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                            )
                        }

                        Button(
                            onClick = {
                                selectedItem?.let { item ->
                                    onAddToCart(item, selectedFraction, quantity)
                                    quantity = 1
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ADD TO CART", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Cart Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Cart (${cartItems.size} items)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                }
                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClearCart() },
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Clear Cart",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Cart Items List
        itemsIndexed(cartItems) { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${item.itemName} (${item.fractionLabel}) x${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "₹${(item.unitPrice * item.fraction).toInt()} per unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${item.itemTotal.toInt()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { onRemoveFromCart(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Checkout Section
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payment & Settlement",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Select Payment Mode:",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentMode.values().forEach { mode ->
                                val isSelected = mode == selectedPaymentMode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onPaymentModeChange(mode) },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = Color.Black,
                                        containerColor = MaterialTheme.colorScheme.background,
                                        labelColor = OnSurfaceDark
                                    )
                                )
                            }
                        }

                        if (selectedPaymentMode == PaymentMode.UDHAR) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = onCustomerNameChange,
                                label = { Text("Customer Name (Required for Udhar)") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldAccent) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total Payable:", style = MaterialTheme.typography.titleMedium, color = OnSurfaceDark)
                            Text(
                                text = "₹${cartTotal.toInt()}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onSubmitSale,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUBMIT SALE & DEDUCT STOCK", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Recent Sales History & Delete / Auto Stock Restore
        if (sales.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Sales History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                    Text(
                        text = "${sales.size} logged transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantDark
                    )
                }
            }

            items(sales.take(10), key = { it.id }) { sale ->
                val isCancelled = sale.status == "CANCELLED"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCancelled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCancelled) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "₹${sale.totalAmount.toInt()} • ${sale.paymentMode.label} (${sale.shopkeeperName})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCancelled) MaterialTheme.colorScheme.error else GoldAccent
                                    )
                                )
                                Text(
                                    text = sale.dateString + if (sale.customerName.isNullOrBlank()) "" else " • Cust: ${sale.customerName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariantDark
                                )
                            }

                            if (isCancelled) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CANCELLED & RESTORED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onCancelSale(sale.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete & Restore Stock", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sale.items.joinToString(", ") { "${it.itemName} (${it.fractionLabel}) x${it.quantity}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceDark
                        )
                    }
                }
            }
        }
    }

    // Quick Barcode Scanner Dialog
    if (showBarcodeScannerDialog) {
        AlertDialog(
            onDismissRequest = { showBarcodeScannerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Barcode Scanner", fontWeight = FontWeight.Bold, color = OnSurfaceDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Scan liquor bottle barcode or enter product code below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                    OutlinedTextField(
                        value = scannedCodeInput,
                        onValueChange = { scannedCodeInput = it },
                        label = { Text("Barcode / Item Code (e.g. RS-750, IB-180)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Popular codes: RS-750 (Royal Stag), IB-180 (Imperial Blue), BP-750 (Blenders Pride), KF-500 (Kingfisher)",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val matched = inventory.find {
                            it.code.equals(scannedCodeInput.trim(), ignoreCase = true) ||
                                    it.name.contains(scannedCodeInput.trim(), ignoreCase = true)
                        }
                        if (matched != null) {
                            selectedItem = matched
                        }
                        scannedCodeInput = ""
                        showBarcodeScannerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("SELECT ITEM", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBarcodeScannerDialog = false }) {
                    Text("CANCEL", color = OnSurfaceVariantDark)
                }
            }
        )
    }
}
