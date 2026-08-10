package com.example.model

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val unitPrice: Double = 0.0,
    val stockQuantity: Double = 0.0, // Main Active Stock quantity (visible/sellable by Shopkeepers)
    val reserveStockQuantity: Double = 0.0, // Warehouse / Reserve Stock quantity (managed by Admin)
    val lowActiveStockThreshold: Double = 10.0, // Custom alert limit for Active Stock
    val lowReserveStockThreshold: Double = 20.0, // Custom alert limit for Reserve Stock
    val category: String = "Liquor",
    val description: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Proportional calculation for fractions
    fun calculatePrice(fraction: Double, qty: Int): Double {
        return (unitPrice * fraction) * qty
    }

    fun totalStock(): Double = stockQuantity + reserveStockQuantity

    fun calculateUnsoldValuation(): Double = totalStock() * unitPrice

    fun isLowActiveStock(): Boolean = stockQuantity <= lowActiveStockThreshold

    fun isLowReserveStock(): Boolean = reserveStockQuantity <= lowReserveStockThreshold

    companion object {
        val DEFAULTS = listOf(
            InventoryItem("beer", "Beer", "BEER", 200.0, 0.0, 0.0, 15.0, 25.0, "Beer", "Standard Beer (1 Pc = ₹200)"),
            InventoryItem("lp", "LP", "LP", 200.0, 0.0, 0.0, 10.0, 20.0, "Liquor", "LP (1 Pc = ₹200)"),
            InventoryItem("kotar", "Kotar", "KOTAR", 100.0, 0.0, 0.0, 15.0, 30.0, "Liquor", "Kotar (1 Pc = ₹100)"),
            InventoryItem("rs", "RS", "RS", 300.0, 0.0, 0.0, 10.0, 20.0, "Whisky", "Royal Stag / RS (1 Pc = ₹300)")
        )
    }
}

enum class ItemFraction(val label: String, val factor: Double) {
    FULL("Full (1.0)", 1.0),
    HALF("Half / Adtha (0.5)", 0.5),
    QUARTER("Quarter / Pauaa (0.25)", 0.25)
}
