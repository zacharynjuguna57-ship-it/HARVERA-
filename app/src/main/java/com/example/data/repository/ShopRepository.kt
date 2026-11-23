package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.CustomProduct
import com.example.data.model.DeliveryAddress
import com.example.data.model.FavoriteItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.StoreType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class ShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        require(cleaned.isNotBlank()) { "Document ID must not be blank" }
        return cleaned
    }

    private fun <T> Flow<T>.logStreamErrors(userId: String, path: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            }
            throw error
        }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        require(userId.isNotBlank()) { "User ID required to observe cart" }
        val path = "users/$userId/cart"
        emitAll(
            db.collection("users").document(userId).collection("cart")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.productName }
                }
                .logStreamErrors(userId, path)
        )
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteItem>> = flow {
        require(userId.isNotBlank()) { "User ID required to observe favorites" }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FavoriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logStreamErrors(userId, path)
        )
    }

    fun observeAddresses(userId: String): Flow<List<DeliveryAddress>> = flow {
        require(userId.isNotBlank()) { "User ID required to observe addresses" }
        val path = "users/$userId/addresses"
        emitAll(
            db.collection("users").document(userId).collection("addresses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DeliveryAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.defaultSelected }
                }
                .logStreamErrors(userId, path)
        )
    }

    fun observeCustomProducts(userId: String): Flow<List<CustomProduct>> = flow {
        require(userId.isNotBlank()) { "User ID required to observe custom products" }
        val path = "users/$userId/custom_products"
        emitAll(
            db.collection("users").document(userId).collection("custom_products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CustomProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logStreamErrors(userId, path)
        )
    }

    fun observeOrders(userId: String): Flow<List<OrderRecord>> = flow {
        require(userId.isNotBlank()) { "User ID required to observe orders" }
        val path = "users/$userId/orders"
        emitAll(
            db.collection("users").document(userId).collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(OrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logStreamErrors(userId, path)
        )
    }

    suspend fun addOrIncrementCart(product: Product, delta: Int = 1): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/cart"
        return try {
            val uid = requireUserId()
            require(delta in 1..99) { "Quantity delta must be between 1 and 99" }
            require(product.price > 0.0 && product.price <= 100000.0) { "Invalid product price" }
            val safeId = sanitizeId(product.id)
            val docRef = db.collection("users").document(uid).collection("cart").document(safeId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                val currentQty = (existing.getLong("quantity") ?: 1L).toInt()
                val nextQty = (currentQty + delta).coerceIn(1, 99)
                docRef.update(
                    mapOf(
                        "quantity" to nextQty,
                        "unitPrice" to roundCents(product.price),
                        "productLocation" to product.locationName.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "productId" to safeId,
                    "productName" to product.name.trim().take(120),
                    "category" to product.category.trim().take(50),
                    "storeType" to product.storeType.name,
                    "unitPrice" to roundCents(product.price),
                    "unitLabel" to product.unitLabel.trim().take(40).ifBlank { "each" },
                    "productLocation" to product.locationName.trim().take(120),
                    "quantity" to delta,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQty: Int): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(cartItemId)
            val docRef = db.collection("users").document(uid).collection("cart").document(safeId)
            docPath = docRef.path
            if (newQty <= 0) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQty.coerceIn(1, 99),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(cartItemId)
            val docRef = db.collection("users").document(uid).collection("cart").document(safeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(product: Product, isCurrentlyFavorite: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/favorites/${product.id}"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(product.id)
            val docRef = db.collection("users").document(uid).collection("favorites").document(safeId)
            docPath = docRef.path
            if (isCurrentlyFavorite) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "productId" to safeId,
                    "productName" to product.name.trim().take(120),
                    "unitPrice" to roundCents(product.price),
                    "productLocation" to product.locationName.trim().take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAddress(
        label: String,
        streetAddress: String,
        city: String,
        instructions: String?,
        latitude: Double?,
        longitude: Double?,
        makeDefault: Boolean,
        existingAddresses: List<DeliveryAddress>
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/addresses"
        return try {
            val uid = requireUserId()
            val cleanLabel = label.trim()
            val cleanStreet = streetAddress.trim()
            val cleanCity = city.trim()
            require(cleanLabel.length in 1..40) { "Address label must be 1-40 characters" }
            require(cleanStreet.length in 3..200) { "Street address must be 3-200 characters" }
            require(cleanCity.length in 2..80) { "City must be 2-80 characters" }

            val colRef = db.collection("users").document(uid).collection("addresses")
            val newDocRef = colRef.document()
            docPath = newDocRef.path

            val batch = db.batch()
            if (makeDefault) {
                existingAddresses.filter { it.defaultSelected && it.id.isNotBlank() }.forEach { oldDefault ->
                    batch.update(
                        colRef.document(oldDefault.id),
                        mapOf(
                            "defaultSelected" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val cleanInstructions = instructions?.trim()?.takeIf { it.isNotEmpty() }?.take(160)
            val payload = mapOf(
                "userId" to uid,
                "label" to cleanLabel,
                "streetAddress" to cleanStreet,
                "city" to cleanCity,
                "instructions" to cleanInstructions,
                "latitude" to latitude?.coerceIn(-90.0, 90.0),
                "longitude" to longitude?.coerceIn(-180.0, 180.0),
                "defaultSelected" to makeDefault,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            batch.set(newDocRef, payload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setDefaultAddress(addressId: String, existingAddresses: List<DeliveryAddress>): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/addresses/$addressId"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(addressId)
            val colRef = db.collection("users").document(uid).collection("addresses")
            docPath = colRef.document(safeId).path

            val batch = db.batch()
            existingAddresses.filter { it.defaultSelected && it.id != safeId && it.id.isNotBlank() }.forEach { addr ->
                batch.update(
                    colRef.document(addr.id),
                    mapOf("defaultSelected" to false, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.update(
                colRef.document(safeId),
                mapOf("defaultSelected" to true, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/addresses/$addressId"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(addressId)
            val docRef = db.collection("users").document(uid).collection("addresses").document(safeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCustomProduct(
        existingId: String?,
        name: String,
        description: String?,
        storeType: StoreType,
        category: String,
        price: Double,
        unitLabel: String,
        locationName: String,
        originCity: String,
        latitude: Double?,
        longitude: Double?,
        inStock: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/custom_products"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanCategory = category.trim()
            val cleanUnit = unitLabel.trim()
            val cleanLocation = locationName.trim()
            val cleanOrigin = originCity.trim()
            require(cleanName.length in 2..100) { "Product name must be 2-100 characters" }
            require(cleanCategory.length in 1..50) { "Category must be 1-50 characters" }
            require(price > 0.0 && price <= 100000.0) { "Cost must be greater than $0 and up to $100,000" }
            require(cleanUnit.length in 1..40) { "Unit label must be 1-40 characters" }
            require(cleanLocation.length in 2..120) { "Store/aisle location must be 2-120 characters" }
            require(cleanOrigin.length in 2..80) { "Origin city/region must be 2-80 characters" }

            val colRef = db.collection("users").document(uid).collection("custom_products")
            val cleanDesc = description?.trim()?.take(300)

            if (existingId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "description" to cleanDesc?.takeIf { it.isNotEmpty() },
                    "storeType" to storeType.name,
                    "category" to cleanCategory,
                    "price" to roundCents(price),
                    "unitLabel" to cleanUnit,
                    "locationName" to cleanLocation,
                    "originCity" to cleanOrigin,
                    "latitude" to latitude?.coerceIn(-90.0, 90.0),
                    "longitude" to longitude?.coerceIn(-180.0, 180.0),
                    "inStock" to inStock,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            } else {
                val safeId = sanitizeId(existingId)
                val docRef = colRef.document(safeId)
                docPath = docRef.path
                val updatePayload = mapOf(
                    "name" to cleanName,
                    "description" to (cleanDesc ?: ""),
                    "storeType" to storeType.name,
                    "category" to cleanCategory,
                    "price" to roundCents(price),
                    "unitLabel" to cleanUnit,
                    "locationName" to cleanLocation,
                    "originCity" to cleanOrigin,
                    "latitude" to latitude?.coerceIn(-90.0, 90.0),
                    "longitude" to longitude?.coerceIn(-180.0, 180.0),
                    "inStock" to inStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomProduct(productId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/custom_products/$productId"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(productId)
            val docRef = db.collection("users").document(uid).collection("custom_products").document(safeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        cartItems: List<CartItem>,
        deliveryAddress: String,
        deliveryCity: String,
        fulfillmentHub: String,
        subtotal: Double,
        deliveryFee: Double,
        taxAmount: Double,
        totalCost: Double
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/orders"
        return try {
            val uid = requireUserId()
            require(cartItems.isNotEmpty()) { "Cart cannot be empty when placing an order" }
            val cleanAddress = deliveryAddress.trim().take(240)
            val cleanCity = deliveryCity.trim().take(80)
            val cleanHub = fulfillmentHub.trim().take(120)
            require(cleanAddress.length >= 3) { "Valid delivery address is required" }
            require(cleanCity.length >= 2) { "Valid delivery city is required" }
            require(totalCost > 0.0) { "Order total must be positive" }

            val ordersCol = db.collection("users").document(uid).collection("orders")
            val cartCol = db.collection("users").document(uid).collection("cart")
            val orderDoc = ordersCol.document()
            docPath = orderDoc.path

            val orderNumber = "FC-${(1000..9999).random()}"
            val itemCount = cartItems.sumOf { it.quantity.toInt().coerceAtLeast(1) }.coerceIn(1, 500)
            val summaryLines = cartItems.take(50).map { item ->
                val priceStr = String.format(java.util.Locale.US, "$%.2f", item.unitPrice)
                "${item.quantity}x ${item.productName} ($priceStr) · ${item.productLocation}".take(200)
            }

            val orderPayload = mapOf(
                "userId" to uid,
                "orderNumber" to orderNumber,
                "status" to OrderStatus.CONFIRMED.name,
                "deliveryAddress" to cleanAddress,
                "deliveryCity" to cleanCity,
                "fulfillmentHub" to cleanHub,
                "itemCount" to itemCount,
                "subtotal" to roundCents(subtotal),
                "deliveryFee" to roundCents(deliveryFee),
                "taxAmount" to roundCents(taxAmount),
                "totalCost" to roundCents(totalCost),
                "itemsSummary" to summaryLines,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(orderDoc, orderPayload)
            cartItems.forEach { cartItem ->
                if (cartItem.id.isNotBlank()) {
                    batch.delete(cartCol.document(cartItem.id))
                }
            }
            batch.commit().await()
            Result.success(orderNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, nextStatus: OrderStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/orders/$orderId"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(orderId)
            val docRef = db.collection("users").document(uid).collection("orders").document(safeId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to nextStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun roundCents(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
