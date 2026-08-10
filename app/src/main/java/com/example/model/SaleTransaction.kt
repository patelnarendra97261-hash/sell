package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SaleItem(
    val itemId: String = "",
    val itemName: String = "",
    val unitPrice: Double = 0.0,
    val fraction: Double = 1.0, // 1.0, 0.5, 0.25
    val fractionLabel: String = "Full",
    val quantity: Int = 1,
    val itemTotal: Double = 0.0
)

enum class PaymentMode(val label: String) {
    CASH("Cash"),
    UDHAR("Udhar (Credit)"),
    ONLINE("Online / UPI")
}

data class SaleTransaction(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val shopkeeperId: String = "",
    val shopkeeperName: String = "",
    val items: List<SaleItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val customerName: String? = null,
    val notes: String? = null,
    val status: String = "COMPLETED"
) {
    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
