package com.shishir.boycottbd

data class Products(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val origin: String = "",
    val totalValue: Double = 0.0
) {
    override fun toString(): String {
        return name
    }
}
