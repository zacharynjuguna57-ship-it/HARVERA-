package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StoreType(val label: String) {
    GROCERY("Groceries"),
    SHOPPING("Shopping");

    companion object {
        fun fromRaw(raw: String?): StoreType =
            entries.firstOrNull { it.name == raw } ?: GROCERY
    }
}

enum class OrderStatus(val label: String) {
    CONFIRMED("Confirmed"),
    PACKING("Packing"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: CONFIRMED
    }
}

enum class ProductSort(val label: String) {
    FEATURED("Featured"),
    LOWEST_COST("Lowest Cost"),
    HIGHEST_COST("Highest Cost"),
    NEAREST_LOCATION("Nearest")
}

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val storeType: StoreType,
    val category: String,
    val price: Double,
    val originalPrice: Double? = null,
    val unitLabel: String,
    val locationName: String,
    val originCity: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val inStock: Boolean = true,
    val rating: Double = 4.8,
    val badge: String? = null,
    val isCustom: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val category: String = "",
    val storeType: String = StoreType.GROCERY.name,
    val unitPrice: Double = 0.0,
    val unitLabel: String = "",
    val productLocation: String = "",
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val unitPrice: Double = 0.0,
    val productLocation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val streetAddress: String = "",
    val city: String = "",
    val instructions: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val defaultSelected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomProduct(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val storeType: String = StoreType.GROCERY.name,
    val category: String = "",
    val price: Double = 0.0,
    val unitLabel: String = "each",
    val locationName: String = "",
    val originCity: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val inStock: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toProduct(): Product = Product(
        id = id,
        name = name,
        description = description.orEmpty().ifBlank { "Custom listing at $locationName ($originCity)" },
        storeType = StoreType.fromRaw(storeType),
        category = category,
        price = price,
        originalPrice = null,
        unitLabel = unitLabel,
        locationName = locationName,
        originCity = originCity,
        latitude = latitude,
        longitude = longitude,
        inStock = inStock,
        rating = 5.0,
        badge = "My Listing",
        isCustom = true
    )
}

@Keep
@IgnoreExtraProperties
data class OrderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val status: String = OrderStatus.CONFIRMED.name,
    val deliveryAddress: String = "",
    val deliveryCity: String = "",
    val fulfillmentHub: String = "",
    val itemCount: Long = 0L,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val itemsSummary: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class UserLocationState(
    val displayTitle: String = "San Francisco Bay Hub",
    val displayAddress: String = "Market St & 4th Ave",
    val city: String = "San Francisco",
    val latitude: Double = 37.7858,
    val longitude: Double = -122.4064,
    val isGpsDetected: Boolean = false
)
