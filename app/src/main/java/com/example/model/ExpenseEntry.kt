package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExpenseEntry(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val shopkeeperName: String = "",
    val amount: Double = 0.0,
    val reason: String = "",
    val notes: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
