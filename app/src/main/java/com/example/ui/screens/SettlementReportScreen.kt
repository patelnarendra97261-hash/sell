package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.DailySettlementReport
import com.example.ui.components.MetricSummaryCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.RoseError

@Composable
fun SettlementReportScreen(
    report: DailySettlementReport,
    selectedDate: String,
    onDateChange: (String) -> Unit
) {
    var showFormulaDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Account Settlement",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OnSurfaceDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Info icon to open mathematical formula
                            IconButton(
                                onClick = { showFormulaDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View Formula",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Date: $selectedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }

                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = onDateChange,
                        label = { Text("Date (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GoldAccent) },
                        singleLine = true,
                        modifier = Modifier.width(170.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Metrics Grid (Total Revenue, Net Cash Account)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Total Sales",
                    value = "₹${report.totalSales.toInt()}",
                    subtitle = "Cash + Udhar + Online",
                    icon = Icons.Default.AttachMoney,
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Net Cash Account",
                    value = "₹${report.netCashAccount.toInt()}",
                    subtitle = "Cash Sales - Expenses",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = if (report.netCashAccount >= 0) EmeraldSuccess else RoseError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Cash Sales",
                    value = "₹${report.totalCashSales.toInt()}",
                    subtitle = "Physical Cash Collected",
                    icon = Icons.Default.AttachMoney,
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Shopkeeper Expenses",
                    value = "₹${report.totalExpenses.toInt()}",
                    subtitle = "Logged Daily Expenses",
                    icon = Icons.Default.MoneyOff,
                    color = RoseError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Shopkeeper Breakdown Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shopkeeper Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurfaceDark
                )
                Text(
                    text = "${report.shopkeeperBreakdowns.size} Active Shopkeepers",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariantDark
                )
            }
        }

        // Shopkeeper Settlement Cards List
        items(report.shopkeeperBreakdowns) { sk ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = sk.shopkeeperName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnSurfaceDark
                            )
                        }

                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Total Sales: ₹${sk.totalSales.toInt()}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Cash Sales", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantDark)
                            Text(
                                text = "₹${sk.cashSales.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            )
                        }
                        Column {
                            Text(text = "Udhar Sales", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantDark)
                            Text(
                                text = "₹${sk.udharSales.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RoseError)
                            )
                        }
                        Column {
                            Text(text = "Expenses", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantDark)
                            Text(
                                text = "₹${sk.totalExpenses.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RoseError)
                            )
                        }
                        Column {
                            Text(text = "Net Cash Hand", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantDark)
                            Text(
                                text = "₹${sk.netCashInHand.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (sk.netCashInHand >= 0) EmeraldSuccess else RoseError
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Mathematical Formula Info Dialog
    if (showFormulaDialog) {
        AlertDialog(
            onDismissRequest = { showFormulaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Functions, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settlement Formula", fontWeight = FontWeight.Bold, color = OnSurfaceDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Daily Net Cash Formula:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
                            Text(
                                text = "Net Cash Account = (Total Cash Sales) - (Shopkeeper Expenses)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OnSurfaceDark
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Calculation Breakdown for $selectedDate:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
                            Text(
                                text = "₹${report.totalCashSales.toInt()} (Cash) - ₹${report.totalExpenses.toInt()} (Expenses) = ₹${report.netCashAccount.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.netCashAccount >= 0) EmeraldSuccess else RoseError
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFormulaDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
