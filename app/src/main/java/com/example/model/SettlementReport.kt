package com.example.model

data class ShopkeeperSettlement(
    val shopkeeperName: String,
    val totalSales: Double,
    val cashSales: Double,
    val udharSales: Double,
    val onlineSales: Double,
    val totalExpenses: Double,
    val netCashInHand: Double // cashSales - totalExpenses
)

data class DailySettlementReport(
    val dateString: String,
    val totalSales: Double,
    val totalCashSales: Double,
    val totalUdharSales: Double,
    val totalOnlineSales: Double,
    val totalExpenses: Double,
    val netCashAccount: Double, // totalCashSales - totalExpenses
    val shopkeeperBreakdowns: List<ShopkeeperSettlement>
)
