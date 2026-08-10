package com.example.model

data class MonthlyAuditReport(
    val monthYear: String = "", // e.g. "2026-08"
    val displayMonth: String = "", // e.g. "August 2026"
    val totalGrossSales: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val totalUdharSales: Double = 0.0,
    val totalOnlineSales: Double = 0.0,
    val totalPurchaseCost: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfitLoss: Double = 0.0, // totalGrossSales - (totalPurchaseCost + totalExpenses)
    val openingStockUnits: Double = 0.0,
    val openingStockValuation: Double = 0.0,
    val endingActiveStockUnits: Double = 0.0,
    val endingReserveStockUnits: Double = 0.0,
    val totalEndingStockUnits: Double = 0.0,
    val pendingSalesValue: Double = 0.0, // Revenue expected if unsold stock is sold at unit prices
    val itemBreakdowns: List<MonthlyItemSummary> = emptyList(),
    val purchasesList: List<StockPurchase> = emptyList()
)

data class MonthlyItemSummary(
    val itemId: String = "",
    val itemName: String = "",
    val unitPrice: Double = 0.0,
    val activeStockUnits: Double = 0.0,
    val reserveStockUnits: Double = 0.0,
    val totalUnits: Double = 0.0,
    val unsoldValuation: Double = 0.0
)
