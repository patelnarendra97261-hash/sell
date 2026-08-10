package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InventoryItem
import com.example.model.ItemFraction
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.FractionalSelectorRow
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark

@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    currentUser: UserProfile?,
    onAdjustStock: (String, Double) -> Unit,
    onSetStock: (String, Double) -> Unit,
    onUpdatePrice: (String, Double) -> Unit,
    onAddNewItem: (String, String, Double, Double, String) -> Unit,
    onTransferReserve: (String, Double) -> Unit = { _, _ -> },
    onUpdateThresholds: (String, Double, Double) -> Unit = { _, _, _ -> },
    onResetAllStockToZero: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showResetStockDialog by remember { mutableStateOf(false) }
    var editingPriceItem by remember { mutableStateOf<InventoryItem?>(null) }
    var editingStockItem by remember { mutableStateOf<InventoryItem?>(null) }
    var transferringReserveItem by remember { mutableStateOf<InventoryItem?>(null) }
    var editingThresholdsItem by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val isAdmin = currentUser?.role == UserRole.ADMIN

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = GoldAccent,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { showResetStockDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET ALL STOCK TO 0", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search inventory (Beer, LP, Kotar, RS)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Inventory List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        isAdmin = isAdmin,
                        onAdjustStock = { delta -> onAdjustStock(item.id, delta) },
                        onEditStock = { editingStockItem = item },
                        onEditPrice = { editingPriceItem = item },
                        onTransferReserve = { transferringReserveItem = item },
                        onEditThresholds = { editingThresholdsItem = item }
                    )
                }
            }
        }
    }

    // Dialog: Add New Inventory Item
    if (showAddItemDialog) {
        AddNewItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, code, price, stock, category ->
                onAddNewItem(name, code, price, stock, category)
                showAddItemDialog = false
            }
        )
    }

    // Dialog: Reset All Stock to 0 Confirmation
    if (showResetStockDialog) {
        AlertDialog(
            onDismissRequest = { showResetStockDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Stock to 0?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to reset active and reserve stock quantities for ALL items to 0? This action will update Firebase Realtime Database live for all devices.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllStockToZero()
                        showResetStockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("YES, RESET ALL TO 0", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStockDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Dialog: Edit Unit Price
    if (editingPriceItem != null) {
        EditPriceDialog(
            item = editingPriceItem!!,
            onDismiss = { editingPriceItem = null },
            onConfirm = { newPrice ->
                onUpdatePrice(editingPriceItem!!.id, newPrice)
                editingPriceItem = null
            }
        )
    }

    // Dialog: Set Exact Main Active Stock
    if (editingStockItem != null) {
        SetStockDialog(
            item = editingStockItem!!,
            onDismiss = { editingStockItem = null },
            onConfirm = { newStock ->
                onSetStock(editingStockItem!!.id, newStock)
                editingStockItem = null
            }
        )
    }

    // Dialog: Transfer Reserve Stock to Main Active Stock
    if (transferringReserveItem != null) {
        TransferReserveDialog(
            item = transferringReserveItem!!,
            onDismiss = { transferringReserveItem = null },
            onConfirm = { qty ->
                onTransferReserve(transferringReserveItem!!.id, qty)
                transferringReserveItem = null
            }
        )
    }

    // Dialog: Edit Low Stock Threshold Limits
    if (editingThresholdsItem != null) {
        EditThresholdsDialog(
            item = editingThresholdsItem!!,
            onDismiss = { editingThresholdsItem = null },
            onConfirm = { activeLimit, reserveLimit ->
                onUpdateThresholds(editingThresholdsItem!!.id, activeLimit, reserveLimit)
                editingThresholdsItem = null
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    isAdmin: Boolean,
    onAdjustStock: (Double) -> Unit,
    onEditStock: () -> Unit,
    onEditPrice: () -> Unit,
    onTransferReserve: () -> Unit = {},
    onEditThresholds: () -> Unit = {}
) {
    var selectedFraction by remember { mutableStateOf(ItemFraction.FULL) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = GoldAccent)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }

                // Current Price Badge & Threshold Settings Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "₹${item.unitPrice.toInt()} / Pc",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (isAdmin) {
                        IconButton(onClick = onEditPrice) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = GoldAccent)
                        }
                    }
                }
            }

            // Warning Alerts Section
            if (item.isLowActiveStock() || item.isLowReserveStock()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isLowActiveStock()) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LOW ACTIVE STOCK: ${item.stockQuantity.toInt()} Pcs left (Alert Limit: ≤${item.lowActiveStockThreshold.toInt()})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }

                    if (item.isLowReserveStock()) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RESERVE STOCK LOW: ${item.reserveStockQuantity.toInt()} Reserve Pcs left (Limit: ≤${item.lowReserveStockThreshold.toInt()}) - Order Stock",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Active Stock (Visible to Shopkeeper)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Main Active Stock",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = EmeraldSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "Shopkeeper Sellable",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = EmeraldSuccess),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${item.stockQuantity} Units (Alert: ≤${item.lowActiveStockThreshold.toInt()})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (!item.isLowActiveStock()) EmeraldSuccess else MaterialTheme.colorScheme.error
                        )
                    )
                }

                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onAdjustStock(-1.0) },
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onEditStock) {
                            Text("Set", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { onAdjustStock(1.0) },
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Reserve / Warehouse Stock Row (Visible Read-Only to Shopkeepers, Editable by Admin)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldAccent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Reserve Warehouse Stock",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = GoldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = if (isAdmin) "Admin Managed" else "Read-Only View",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = GoldAccent),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${item.reserveStockQuantity} Reserve Units (Alert: ≤${item.lowReserveStockThreshold.toInt()})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (!item.isLowReserveStock()) MaterialTheme.colorScheme.onSurface else GoldAccent
                        )
                    )
                }

                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onEditThresholds) {
                            Text("Alert Limits", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = onTransferReserve,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Transfer -> Active", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "Read-Only",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fractional Pricing Logic Calculator
            FractionalSelectorRow(
                selectedFraction = selectedFraction,
                unitPrice = item.unitPrice,
                onFractionSelected = { selectedFraction = it }
            )
        }
    }
}

@Composable
fun AddNewItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Liquor") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Liquor Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Imperial Blue)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Short Code (e.g. IB)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Unit Price per Pc (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Initial Stock Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && price > 0) {
                        onConfirm(name, code, price, stock, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditPriceDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceStr by remember { mutableStateOf(item.unitPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Unit Price for ${item.name}") },
        text = {
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("New Unit Price (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price >= 0) {
                        onConfirm(price)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Update Price")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SetStockDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var stockStr by remember { mutableStateOf(item.stockQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Stock for ${item.name}") },
        text = {
            OutlinedTextField(
                value = stockStr,
                onValueChange = { stockStr = it },
                label = { Text("Stock Quantity (Full Units)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val stock = stockStr.toDoubleOrNull()
                    if (stock != null && stock >= 0) {
                        onConfirm(stock)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Save Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransferReserveDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var transferQtyStr by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Reserve Stock -> Main Active") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Item: ${item.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Current Reserve Stock: ${item.reserveStockQuantity} Units",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldAccent
                )
                Text(
                    text = "Current Active Stock: ${item.stockQuantity} Units",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = transferQtyStr,
                    onValueChange = { transferQtyStr = it },
                    label = { Text("Quantity to Transfer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = transferQtyStr.toDoubleOrNull()
                    if (qty != null && qty > 0 && qty <= item.reserveStockQuantity) {
                        onConfirm(qty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditThresholdsDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (activeLimit: Double, reserveLimit: Double) -> Unit
) {
    var activeLimitStr by remember { mutableStateOf(item.lowActiveStockThreshold.toInt().toString()) }
    var reserveLimitStr by remember { mutableStateOf(item.lowReserveStockThreshold.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Low Stock Alert Thresholds") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Item: ${item.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Configure custom numeric threshold limits for low stock warnings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = activeLimitStr,
                    onValueChange = { activeLimitStr = it },
                    label = { Text("Main Active Stock Alert Limit (Units)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reserveLimitStr,
                    onValueChange = { reserveLimitStr = it },
                    label = { Text("Reserve Warehouse Alert Limit (Units)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val activeLimit = activeLimitStr.toDoubleOrNull() ?: item.lowActiveStockThreshold
                    val reserveLimit = reserveLimitStr.toDoubleOrNull() ?: item.lowReserveStockThreshold
                    onConfirm(activeLimit, reserveLimit)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Save Thresholds")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
