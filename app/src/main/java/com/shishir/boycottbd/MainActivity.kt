package com.shishir.boycottbd

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var spinnerProducts: Spinner
    private lateinit var etAmount: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvTotalBoycottToday: TextView
    private lateinit var tvTotalBoycottLifetime: TextView
    private lateinit var tvTopValuedBoycotted: TextView
    private lateinit var pieChart: PieChart

    private var productList: List<Products> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        spinnerProducts = findViewById(R.id.spinnerProducts)
        etAmount = findViewById(R.id.etAmount)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvTotalBoycottToday = findViewById(R.id.tvTotalBoycottToday)
        tvTotalBoycottLifetime = findViewById(R.id.tvTotalBoycottLifetime)
        tvTopValuedBoycotted = findViewById(R.id.tvTopValuedBoycotted)
        pieChart = findViewById(R.id.pieChart)
        var topbar= findViewById<MaterialToolbar>(R.id.topAppBar)
        fetchProducts()

        btnSubmit.setOnClickListener {
            val selectedIndex = spinnerProducts.selectedItemPosition
            if (selectedIndex >= 0 && selectedIndex < productList.size) {
                val selectedProduct = productList[selectedIndex]
                val amount = etAmount.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    updateBoycottData(selectedProduct, amount)
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
        }
        topbar.setOnClickListener {
            finishAffinity()
        }
    }

    private fun fetchProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                productList = result.map { document ->
                    Products(
                        id = document.getString("id") ?: "",
                        name = document.getString("name") ?: "",
                        category = document.getString("category") ?: "",
                        description = document.getString("description") ?: "",
                        origin = document.getString("origin") ?: "",
                        totalValue = document.getDouble("totalValue") ?: 0.0
                    )
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productList.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerProducts.adapter = adapter

                fetchBoycottSummary()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBoycottData(product: Products, amount: Double) {
        val updatedValue = product.totalValue + amount
        db.collection("products").document(product.id)
            .update("totalValue", updatedValue)
            .addOnSuccessListener {
                Toast.makeText(this, "Boycott updated successfully", Toast.LENGTH_SHORT).show()
                fetchProducts() // Refresh data and chart
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchBoycottSummary() {
        db.collection("products").get().addOnSuccessListener { result ->
            var totalLifetime = 0.0
            var topProduct: Products? = null

            val entries = mutableListOf<PieEntry>()

            for (doc in result) {
                val value = doc.getDouble("totalValue") ?: 0.0
                totalLifetime += value
                if (value > 0) {
                    entries.add(PieEntry(value.toFloat(), doc.getString("name") ?: ""))
                }

                if (topProduct == null || value > topProduct.totalValue) {
                    topProduct = Products(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: "",
                        origin = doc.getString("origin") ?: "",
                        totalValue = value
                    )
                }
            }

            tvTotalBoycottToday.text = "Total Boycott Today: ৳$totalLifetime"
            tvTotalBoycottLifetime.text = "Lifetime Boycott: ৳$totalLifetime"
            tvTopValuedBoycotted.text = "Top Boycotted: ${topProduct?.name ?: "None"}"

            updateChart(entries)
        }
    }

    private fun updateChart(entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "Boycott Chart")
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        val data = PieData(dataSet)
        data.setValueTextSize(12f)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
