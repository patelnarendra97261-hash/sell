package com.example.model

data class StockPurchase(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "", // Format: "yyyy-MM-dd" e.g. "2026-08-08"
    val monthYear: String = "",  // Format: "yyyy-MM" e.g. "2026-08"
    val itemId: String = "",
    val itemName: String = "",
    val quantity: Double = 0.0, // Quantity purchased (e.g. 30.0 pcs)
    val totalPurchaseCost: Double = 0.0, // Total payment made (e.g. ₹6000.0)
    val supplierName: String = "",
    val recordedBy: String = "Admin Owner",
    val notes: String = ""
)
