package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.MarketplaceCatalog
import com.example.data.model.CartItem
import com.example.data.model.CustomProduct
import com.example.data.model.DeliveryAddress
import com.example.data.model.FavoriteItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductSort
import com.example.data.model.StoreType
import com.example.data.model.UserLocationState
import com.example.data.repository.ShopRepository
import com.example.location.LocationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ProductWithDistance(
    val product: Product,
    val distanceKm: Double?,
    val distanceLabel: String?,
    val cartQuantity: Int,
    val isFavorite: Boolean
)

data class FilterState(
    val searchQuery: String = "",
    val storeFilter: StoreType? = null,
    val selectedCategory: String = "All",
    val favoritesOnly: Boolean = false,
    val sortBy: ProductSort = ProductSort.FEATURED
)

data class CartTotals(
    val itemCount: Int = 0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val freeDeliveryRemaining: Double = 35.0
)

class ShopViewModel(
    private val repository: ShopRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _gpsOverrideLocation = MutableStateFlow<UserLocationState?>(null)
    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _isCheckingOut = MutableStateFlow(false)
    val isCheckingOut: StateFlow<Boolean> = _isCheckingOut.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val cartState: StateFlow<UiState<List<CartItem>>> =
        repository.observeCart(currentUserId).toUiState("cart")

    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> =
        repository.observeFavorites(currentUserId).toUiState("favorites")

    val addressesState: StateFlow<UiState<List<DeliveryAddress>>> =
        repository.observeAddresses(currentUserId).toUiState("addresses")

    val customProductsState: StateFlow<UiState<List<CustomProduct>>> =
        repository.observeCustomProducts(currentUserId).toUiState("custom products")

    val ordersState: StateFlow<UiState<List<OrderRecord>>> =
        repository.observeOrders(currentUserId).toUiState("orders")

    val activeLocation: StateFlow<UserLocationState> = combine(
        addressesState,
        _gpsOverrideLocation
    ) { addrUi, gpsOverride ->
        if (gpsOverride != null) {
            gpsOverride
        } else {
            val savedDefault = (addrUi as? UiState.Success)?.data?.firstOrNull { it.defaultSelected }
                ?: (addrUi as? UiState.Success)?.data?.firstOrNull()
            if (savedDefault != null) {
                UserLocationState(
                    displayTitle = savedDefault.label,
                    displayAddress = savedDefault.streetAddress,
                    city = savedDefault.city,
                    latitude = savedDefault.latitude ?: DEFAULT_LAT,
                    longitude = savedDefault.longitude ?: DEFAULT_LON,
                    isGpsDetected = false
                )
            } else {
                UserLocationState()
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UserLocationState()
    )

    val catalogItems: StateFlow<List<ProductWithDistance>> = combine(
        customProductsState,
        cartState,
        favoritesState,
        activeLocation,
        _filterState
    ) { customUi, cartUi, favUi, location, filter ->
        val customList = (customUi as? UiState.Success)?.data?.map { it.toProduct() }.orEmpty()
        val allProducts = customList + MarketplaceCatalog.curatedProducts
        val cartMap = (cartUi as? UiState.Success)?.data?.associateBy { it.productId }.orEmpty()
        val favSet = (favUi as? UiState.Success)?.data?.map { it.productId }?.toSet().orEmpty()

        val enriched = allProducts.map { product ->
            val distKm = LocationHelper.distanceKmBetween(
                location.latitude,
                location.longitude,
                product.latitude,
                product.longitude
            )
            ProductWithDistance(
                product = product,
                distanceKm = distKm,
                distanceLabel = LocationHelper.formatDistance(distKm),
                cartQuantity = cartMap[product.id]?.quantity?.toInt() ?: 0,
                isFavorite = favSet.contains(product.id)
            )
        }

        val query = filter.searchQuery.trim().lowercase()
        val filtered = enriched.filter { item ->
            val p = item.product
            val matchesStore = filter.storeFilter == null || p.storeType == filter.storeFilter
            val matchesCategory = when (filter.selectedCategory) {
                "All" -> true
                "My Listings" -> p.isCustom
                else -> p.category.equals(filter.selectedCategory, ignoreCase = true)
            }
            val matchesFav = !filter.favoritesOnly || item.isFavorite
            val matchesQuery = query.isBlank() ||
                p.name.lowercase().contains(query) ||
                p.category.lowercase().contains(query) ||
                p.locationName.lowercase().contains(query) ||
                p.originCity.lowercase().contains(query) ||
                p.description.lowercase().contains(query)
            matchesStore && matchesCategory && matchesFav && matchesQuery
        }

        when (filter.sortBy) {
            ProductSort.FEATURED -> filtered.sortedByDescending { it.product.isCustom }
            ProductSort.LOWEST_COST -> filtered.sortedBy { it.product.price }
            ProductSort.HIGHEST_COST -> filtered.sortedByDescending { it.product.price }
            ProductSort.NEAREST_LOCATION -> filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val cartTotals: StateFlow<CartTotals> = cartState.map { state ->
        val items = (state as? UiState.Success)?.data.orEmpty()
        if (items.isEmpty()) {
            CartTotals()
        } else {
            val count = items.sumOf { it.quantity.toInt().coerceAtLeast(1) }
            val subtotal = items.sumOf { it.unitPrice * it.quantity }
            val deliveryFee = if (subtotal >= FREE_DELIVERY_THRESHOLD) 0.0 else STANDARD_DELIVERY_FEE
            val tax = subtotal * TAX_RATE
            val total = subtotal + deliveryFee + tax
            CartTotals(
                itemCount = count,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                taxAmount = tax,
                totalCost = total,
                freeDeliveryRemaining = (FREE_DELIVERY_THRESHOLD - subtotal).coerceAtLeast(0.0)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CartTotals()
    )

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun selectStoreFilter(storeType: StoreType?) {
        _filterState.value = _filterState.value.copy(
            storeFilter = storeType,
            selectedCategory = "All"
        )
    }

    fun selectCategory(category: String) {
        _filterState.value = _filterState.value.copy(selectedCategory = category)
    }

    fun toggleFavoritesOnly() {
        _filterState.value = _filterState.value.copy(favoritesOnly = !_filterState.value.favoritesOnly)
    }

    fun selectSort(sort: ProductSort) {
        _filterState.value = _filterState.value.copy(sortBy = sort)
    }

    fun clearFilters() {
        _filterState.value = FilterState()
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val res = repository.addOrIncrementCart(product, quantity)
            res.onSuccess {
                _events.tryEmit("Added ${product.name} to cart")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't add item to cart")
            }
        }
    }

    fun updateCartQuantity(cartItemId: String, newQty: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, newQty).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't update cart")
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            repository.removeFromCart(cartItemId).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't remove item")
            }
        }
    }

    fun toggleFavorite(product: Product, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(product, currentlyFavorite).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't update saved item")
            }
        }
    }

    fun detectCurrentLocation(context: Context, saveToAddresses: Boolean = true) {
        if (_isDetectingLocation.value) return
        viewModelScope.launch {
            _isDetectingLocation.value = true
            val result = LocationHelper.fetchCurrentLocation(context)
            result.onSuccess { detected ->
                _gpsOverrideLocation.value = UserLocationState(
                    displayTitle = "GPS Location",
                    displayAddress = detected.streetAddress,
                    city = detected.city,
                    latitude = detected.latitude,
                    longitude = detected.longitude,
                    isGpsDetected = true
                )
                if (saveToAddresses) {
                    val existing = (addressesState.value as? UiState.Success)?.data.orEmpty()
                    repository.saveAddress(
                        label = "Current GPS",
                        streetAddress = detected.streetAddress,
                        city = detected.city,
                        instructions = "Detected via device GPS",
                        latitude = detected.latitude,
                        longitude = detected.longitude,
                        makeDefault = true,
                        existingAddresses = existing
                    )
                }
                _events.tryEmit("Delivery location set to ${detected.streetAddress}, ${detected.city}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not detect GPS location")
            }
            _isDetectingLocation.value = false
        }
    }

    fun saveAddress(
        label: String,
        streetAddress: String,
        city: String,
        instructions: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        makeDefault: Boolean = true
    ) {
        viewModelScope.launch {
            val existing = (addressesState.value as? UiState.Success)?.data.orEmpty()
            repository.saveAddress(
                label = label,
                streetAddress = streetAddress,
                city = city,
                instructions = instructions,
                latitude = latitude,
                longitude = longitude,
                makeDefault = makeDefault,
                existingAddresses = existing
            ).onSuccess {
                _gpsOverrideLocation.value = null
                _events.tryEmit("Saved delivery address \"$label\"")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save address")
            }
        }
    }

    fun setDefaultAddress(address: DeliveryAddress) {
        viewModelScope.launch {
            val existing = (addressesState.value as? UiState.Success)?.data.orEmpty()
            repository.setDefaultAddress(address.id, existing).onSuccess {
                _gpsOverrideLocation.value = null
                _events.tryEmit("Delivering to ${address.label} (${address.streetAddress})")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't switch delivery address")
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repository.deleteAddress(addressId).onSuccess {
                _events.tryEmit("Address removed")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't remove address")
            }
        }
    }

    fun saveCustomProduct(
        existingId: String?,
        name: String,
        description: String?,
        storeType: StoreType,
        category: String,
        price: Double,
        unitLabel: String,
        locationName: String,
        originCity: String,
        useCurrentGpsCoords: Boolean
    ) {
        viewModelScope.launch {
            val loc = activeLocation.value
            val lat = if (useCurrentGpsCoords) loc.latitude else null
            val lon = if (useCurrentGpsCoords) loc.longitude else null
            repository.saveCustomProduct(
                existingId = existingId,
                name = name,
                description = description,
                storeType = storeType,
                category = category,
                price = price,
                unitLabel = unitLabel,
                locationName = locationName,
                originCity = originCity,
                latitude = lat,
                longitude = lon,
                inStock = true
            ).onSuccess {
                val verb = if (existingId == null) "Added" else "Updated"
                _events.tryEmit("$verb $name with location & cost")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't save product")
            }
        }
    }

    fun deleteCustomProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteCustomProduct(productId).onSuccess {
                _events.tryEmit("Custom product removed")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't delete product")
            }
        }
    }

    fun placeOrder(onOrderPlaced: () -> Unit) {
        if (_isCheckingOut.value) return
        val cartItems = (cartState.value as? UiState.Success)?.data.orEmpty()
        if (cartItems.isEmpty()) {
            _events.tryEmit("Add groceries or shopping items before checking out")
            return
        }
        val totals = cartTotals.value
        val loc = activeLocation.value
        val primaryHub = cartItems.firstOrNull()?.productLocation ?: "FreshCart Regional Hub"

        viewModelScope.launch {
            _isCheckingOut.value = true
            repository.placeOrder(
                cartItems = cartItems,
                deliveryAddress = loc.displayAddress,
                deliveryCity = loc.city,
                fulfillmentHub = primaryHub,
                subtotal = totals.subtotal,
                deliveryFee = totals.deliveryFee,
                taxAmount = totals.taxAmount,
                totalCost = totals.totalCost
            ).onSuccess { orderNum ->
                _events.tryEmit("Order $orderNum confirmed!")
                onOrderPlaced()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Checkout failed")
            }
            _isCheckingOut.value = false
        }
    }

    fun advanceOrderStatus(order: OrderRecord) {
        val current = OrderStatus.fromRaw(order.status)
        val next = when (current) {
            OrderStatus.CONFIRMED -> OrderStatus.PACKING
            OrderStatus.PACKING -> OrderStatus.OUT_FOR_DELIVERY
            OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
            OrderStatus.DELIVERED, OrderStatus.CANCELLED -> return
        }
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, next).onSuccess {
                _events.tryEmit("Order ${order.orderNumber} is now ${next.label}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't update order status")
            }
        }
    }

    fun cancelOrder(order: OrderRecord) {
        val current = OrderStatus.fromRaw(order.status)
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) return
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, OrderStatus.CANCELLED).onSuccess {
                _events.tryEmit("Order ${order.orderNumber} cancelled")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Couldn't cancel order")
            }
        }
    }

    private companion object {
        const val TAG = "ShopVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val FREE_DELIVERY_THRESHOLD = 35.0
        const val STANDARD_DELIVERY_FEE = 3.99
        const val TAX_RATE = 0.0825
        const val DEFAULT_LAT = 37.7858
        const val DEFAULT_LON = -122.4064
    }
}
