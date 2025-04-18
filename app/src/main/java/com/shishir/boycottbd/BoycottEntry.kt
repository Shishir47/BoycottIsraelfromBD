package com.shishir.boycottbd

data class BoycottEntry(
    val productId: String = "",
    val productName: String = "",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
