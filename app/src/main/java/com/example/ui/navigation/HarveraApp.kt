package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.Product
import com.example.data.repository.ShopRepository
import com.example.ui.screens.AddEditProductSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocationsProfileScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailSheet
import com.example.ui.viewmodel.ProductWithDistance
import com.example.ui.viewmodel.ShopViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private sealed class HarveraDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Shop : HarveraDestination("shop", "Shop", Icons.Filled.Storefront, Icons.Outlined.Storefront)
    data object Cart : HarveraDestination("cart", "Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
    data object Orders : HarveraDestination("orders", "Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    data object Locations : HarveraDestination("locations", "Locations", Icons.Filled.LocationOn, Icons.Outlined.LocationOn)
}

private val navDestinations = listOf(
    HarveraDestination.Shop,
    HarveraDestination.Cart,
    HarveraDestination.Orders,
    HarveraDestination.Locations
)

@Composable
fun HarveraApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedHarveraShell(
                currentUserId = uid,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedHarveraShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ShopViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ShopViewModel(ShopRepository(db), currentUserId)
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val catalogItems by viewModel.catalogItems.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val activeLocation by viewModel.activeLocation.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val cartTotals by viewModel.cartTotals.collectAsStateWithLifecycle()
    val addressesState by viewModel.addressesState.collectAsStateWithLifecycle()
    val customProductsState by viewModel.customProductsState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
    val isCheckingOut by viewModel.isCheckingOut.collectAsStateWithLifecycle()

    var selectedProduct by remember { mutableStateOf<ProductWithDistance?>(null) }
    var showAddProductSheet by remember { mutableStateOf(false) }
    var editingCustomProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateToTab: (HarveraDestination) -> Unit = { dest ->
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentRoute = currentDestination?.route

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentRoute == HarveraDestination.Shop.route) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingCustomProduct = null
                            showAddProductSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Add Product") },
                        modifier = Modifier.testTag("add_product_fab")
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest) },
                                icon = {
                                    DestinationIcon(
                                        dest = dest,
                                        selected = selected,
                                        cartCount = cartTotals.itemCount
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest) },
                                icon = {
                                    DestinationIcon(
                                        dest = dest,
                                        selected = selected,
                                        cartCount = cartTotals.itemCount
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = HarveraDestination.Shop.route
                    ) {
                        composable(HarveraDestination.Shop.route) {
                            HomeScreen(
                                products = catalogItems,
                                filterState = filterState,
                                activeLocation = activeLocation,
                                onSearchChange = viewModel::updateSearchQuery,
                                onStoreFilterChange = viewModel::selectStoreFilter,
                                onCategoryChange = viewModel::selectCategory,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                onSortChange = viewModel::selectSort,
                                onClearFilters = viewModel::clearFilters,
                                onOpenLocationTab = { navigateToTab(HarveraDestination.Locations) },
                                onProductClick = { selectedProduct = it },
                                onAddToCart = { viewModel.addToCart(it.product, 1) },
                                onDecrementCart = { item ->
                                    viewModel.updateCartQuantity(item.product.id, item.cartQuantity - 1)
                                },
                                onToggleFavorite = { item ->
                                    viewModel.toggleFavorite(item.product, item.isFavorite)
                                }
                            )
                        }

                        composable(HarveraDestination.Cart.route) {
                            CartScreen(
                                cartState = cartState,
                                totals = cartTotals,
                                activeLocation = activeLocation,
                                isCheckingOut = isCheckingOut,
                                onUpdateQuantity = viewModel::updateCartQuantity,
                                onRemoveItem = viewModel::removeFromCart,
                                onChangeLocation = { navigateToTab(HarveraDestination.Locations) },
                                onPlaceOrder = {
                                    viewModel.placeOrder {
                                        navigateToTab(HarveraDestination.Orders)
                                    }
                                },
                                onBrowseMarket = { navigateToTab(HarveraDestination.Shop) }
                            )
                        }

                        composable(HarveraDestination.Orders.route) {
                            OrdersScreen(
                                ordersState = ordersState,
                                onAdvanceStatus = viewModel::advanceOrderStatus,
                                onCancelOrder = viewModel::cancelOrder,
                                onBrowseMarket = { navigateToTab(HarveraDestination.Shop) }
                            )
                        }

                        composable(HarveraDestination.Locations.route) {
                            LocationsProfileScreen(
                                activeLocation = activeLocation,
                                addressesState = addressesState,
                                customProductsState = customProductsState,
                                isDetectingLocation = isDetectingLocation,
                                onDetectGpsLocation = { viewModel.detectCurrentLocation(context) },
                                onSaveAddress = { label, street, city, notes ->
                                    viewModel.saveAddress(label, street, city, notes)
                                },
                                onSelectAddress = viewModel::setDefaultAddress,
                                onDeleteAddress = viewModel::deleteAddress,
                                onAddCustomProduct = {
                                    editingCustomProduct = null
                                    showAddProductSheet = true
                                },
                                onEditCustomProduct = { product ->
                                    editingCustomProduct = product
                                    showAddProductSheet = true
                                },
                                onDeleteCustomProduct = viewModel::deleteCustomProduct,
                                onSignedOut = onSignedOut
                            )
                        }
                    }
                }
            }
        }
    }

    val productInDetail = selectedProduct?.let { sel ->
        catalogItems.firstOrNull { it.product.id == sel.product.id } ?: sel
    }
    if (productInDetail != null) {
        ProductDetailSheet(
            item = productInDetail,
            onDismiss = { selectedProduct = null },
            onAddToCart = { qty -> viewModel.addToCart(productInDetail.product, qty) },
            onToggleFavorite = { viewModel.toggleFavorite(productInDetail.product, productInDetail.isFavorite) },
            onEditCustomProduct = { prod ->
                selectedProduct = null
                editingCustomProduct = prod
                showAddProductSheet = true
            },
            onDeleteCustomProduct = { prodId ->
                selectedProduct = null
                viewModel.deleteCustomProduct(prodId)
            }
        )
    }

    if (showAddProductSheet) {
        AddEditProductSheet(
            editingProduct = editingCustomProduct,
            activeCity = activeLocation.city,
            onDismiss = {
                showAddProductSheet = false
                editingCustomProduct = null
            },
            onSave = { existingId, name, desc, storeType, category, price, unit, locName, origin, useGps ->
                viewModel.saveCustomProduct(
                    existingId = existingId,
                    name = name,
                    description = desc,
                    storeType = storeType,
                    category = category,
                    price = price,
                    unitLabel = unit,
                    locationName = locName,
                    originCity = origin,
                    useCurrentGpsCoords = useGps
                )
            }
        )
    }
}

@Composable
private fun DestinationIcon(
    dest: HarveraDestination,
    selected: Boolean,
    cartCount: Int
) {
    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest == HarveraDestination.Cart && cartCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (cartCount > 99) "99+" else "$cartCount")
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = dest.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = dest.label)
    }
}
