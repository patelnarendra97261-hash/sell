package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.MonthlyAuditReport
import com.example.model.MonthlyItemSummary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthlyAuditScreen(
    report: MonthlyAuditReport,
    selectedMonthYear: String,
    onMonthSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month Switcher Header
        MonthSwitcherHeader(
            displayMonth = report.displayMonth,
            currentMonthYear = selectedMonthYear,
            onMonthChanged = onMonthSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Financial Profit / Loss Banner
            item {
                NetProfitBanner(report = report)
            }

            // Financial Summary Grid Cards
            item {
                FinancialGrid(report = report)
            }

            // Unsold Remaining Stock Valuation & Carry Forward Card
            item {
                StockValuationCard(report = report)
            }

            // Itemized Liquors Audit Section Title
            item {
                Text(
                    text = "Itemized Inventory & Stock Audit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // List of Item Summaries
            items(report.itemBreakdowns) { itemSummary ->
                ItemAuditCard(itemSummary = itemSummary)
            }
        }
    }
}

@Composable
fun MonthSwitcherHeader(
    displayMonth: String,
    currentMonthYear: String,
    onMonthChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newMonth = navigateMonth(currentMonthYear, -1)
                    onMonthChanged(newMonth)
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month", tint = GoldAccent)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayMonth,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent
                )
                Text(
                    text = "Monthly Financial & Stock Audit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    val newMonth = navigateMonth(currentMonthYear, 1)
                    onMonthChanged(newMonth)
                }
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month", tint = GoldAccent)
            }
        }
    }
}

@Composable
fun NetProfitBanner(report: MonthlyAuditReport) {
    val isProfit = report.netProfitLoss >= 0
    val bannerColor = if (isProfit) EmeraldSuccess else MaterialTheme.colorScheme.error
    var showFormulaDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.4f))
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(bannerColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = bannerColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isProfit) "Net Monthly Profit" else "Net Monthly Loss",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariantDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showFormulaDialog = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View Audit Formula",
                                    tint = bannerColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "₹${report.netProfitLoss.toInt()}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = bannerColor
                            )
                        )
                    }
                }

                Surface(color = bannerColor.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = if (isProfit) "PROFITABLE" else "LOSS",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, color = bannerColor),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showFormulaDialog) {
        AlertDialog(
            onDismissRequest = { showFormulaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Functions, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Monthly Audit Formula", fontWeight = FontWeight.Bold, color = OnSurfaceDark)
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
                            Text(text = "Net Profit Calculation Formula:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
                            Text(
                                text = "Net Profit/Loss = Gross Sales - [ Stock Purchase Cost + Expenses ]",
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
                            Text(text = "Monthly Calculation Breakdown:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
                            Text(
                                text = "Gross Sales: ₹${report.totalGrossSales.toInt()}\nPurchases: ₹${report.totalPurchaseCost.toInt()}\nExpenses: ₹${report.totalExpenses.toInt()}\nNet Profit: ₹${report.netProfitLoss.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = bannerColor)
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
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun FinancialGrid(report: MonthlyAuditReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuditMetricCard(
                title = "Total Gross Sales",
                value = "₹${report.totalGrossSales.toInt()}",
                subtitle = "Cash: ₹${report.totalCashSales.toInt()} | Udhar: ₹${report.totalUdharSales.toInt()}",
                icon = Icons.Default.Payments,
                iconColor = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )

            AuditMetricCard(
                title = "Stock Purchase Cost",
                value = "₹${report.totalPurchaseCost.toInt()}",
                subtitle = "${report.purchasesList.sumOf { it.quantity }.toInt()} Pcs Bought",
                icon = Icons.Default.ShoppingBag,
                iconColor = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuditMetricCard(
                title = "Shopkeeper Expenses",
                value = "₹${report.totalExpenses.toInt()}",
                subtitle = "Daily Shop Expenses",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )

            AuditMetricCard(
                title = "Pending Udhar Sales",
                value = "₹${report.totalUdharSales.toInt()}",
                subtitle = "Customer Udhar Balance",
                icon = Icons.Default.Assessment,
                iconColor = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AuditMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StockValuationCard(report: MonthlyAuditReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unsold Stock Valuation & Carry Forward",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(color = GoldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "AUTOCARRY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GoldAccent),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Unsold Units", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${report.totalEndingStockUnits.toInt()} Pcs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent)
                    )
                    Text(
                        "Active: ${report.endingActiveStockUnits.toInt()} | Reserve: ${report.endingReserveStockUnits.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Ending Stock Valuation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₹${report.pendingSalesValue.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = EmeraldSuccess)
                    )
                    Text(
                        "Expected Sales Revenue",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "✓ Opening Stock of ${report.displayMonth}: Carried forward seamlessly (${report.openingStockUnits.toInt()} Pcs valued at ₹${report.openingStockValuation.toInt()}).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ItemAuditCard(itemSummary: MonthlyItemSummary) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = itemSummary.itemName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Unit Price: ₹${itemSummary.unitPrice.toInt()} / Pc",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = EmeraldSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "Active: ${itemSummary.activeStockUnits.toInt()} Pcs",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = EmeraldSuccess),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(color = GoldAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "Reserve: ${itemSummary.reserveStockUnits.toInt()} Pcs",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GoldAccent),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${itemSummary.unsoldValuation.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                )
                Text(
                    text = "${itemSummary.totalUnits.toInt()} Total Pcs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun navigateMonth(currentMonthYear: String, delta: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(currentMonthYear) ?: return currentMonthYear
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, delta)
        sdf.format(cal.time)
    } catch (e: Exception) {
        currentMonthYear
    }
}
