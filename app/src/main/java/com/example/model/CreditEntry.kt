package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UdharStatus {
    PENDING,
    SETTLED
}

data class CreditEntry(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val customerName: String = "",
    val customerPhone: String = "",
    val amount: Double = 0.0,
    val shopkeeperName: String = "",
    val saleId: String? = null,
    val itemSummary: String = "",
    val status: UdharStatus = UdharStatus.PENDING,
    val settledTimestamp: Long? = null,
    val notes: String? = null
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
