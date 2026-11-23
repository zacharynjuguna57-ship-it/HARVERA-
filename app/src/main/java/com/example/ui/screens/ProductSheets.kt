package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.catalog.MarketplaceCatalog
import com.example.data.model.Product
import com.example.data.model.StoreType
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryTintColors
import com.example.ui.components.formatCost
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.HarvestTangerine
import com.example.ui.viewmodel.ProductWithDistance
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    item: ProductWithDistance,
    onDismiss: () -> Unit,
    onAddToCart: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onEditCustomProduct: (Product) -> Unit,
    onDeleteCustomProduct: (String) -> Unit
) {
    val product = item.product
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedQty by remember(product.id) { mutableIntStateOf(1) }
    val (bgTint, iconTint) = categoryTintColors(product.category, product.storeType)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgTint)
            ) {
                Icon(
                    imageVector = categoryIcon(product.category, product.storeType),
                    contentDescription = product.category,
                    tint = iconTint,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.Center)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${product.storeType.label} · ${product.category}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = iconTint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (item.isFavorite) HarvestTangerine else iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.unitLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = GoldAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", product.rating),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cost & Location Info Card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Product Cost",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatCost(product.price)} / ${product.unitLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (product.originalPrice != null && product.originalPrice > product.price) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatCost(product.originalPrice),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Store / Fulfillment Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = product.locationName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Origin & Distance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val distText = item.distanceLabel?.let { " · $it" } ?: ""
                            Text(
                                text = "${product.originCity}$distText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (product.isCustom) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditCustomProduct(product) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Listing")
                    }
                    OutlinedButton(
                        onClick = { onDeleteCustomProduct(product.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quantity & Add CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedQty > 1) selectedQty-- }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                        }
                        Text(
                            text = "$selectedQty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { if (selectedQty < 99) selectedQty++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                        }
                    }
                }

                Button(
                    onClick = {
                        onAddToCart(selectedQty)
                        onDismiss()
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("sheet_add_to_cart_button")
                ) {
                    Text(
                        text = "Add to Cart · ${formatCost(product.price * selectedQty)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductSheet(
    editingProduct: Product?,
    activeCity: String,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name.orEmpty()) }
    var description by remember(editingProduct) { mutableStateOf(editingProduct?.description.orEmpty()) }
    var storeType by remember(editingProduct) { mutableStateOf(editingProduct?.storeType ?: StoreType.GROCERY) }
    val categories = if (storeType == StoreType.GROCERY) {
        MarketplaceCatalog.groceryCategories
    } else {
        MarketplaceCatalog.shoppingCategories
    }
    var category by remember(editingProduct) {
        mutableStateOf(editingProduct?.category ?: categories.first())
    }
    var priceText by remember(editingProduct) {
        mutableStateOf(editingProduct?.price?.let { String.format(Locale.US, "%.2f", it) }.orEmpty())
    }
    var unitLabel by remember(editingProduct) { mutableStateOf(editingProduct?.unitLabel ?: "each") }
    var locationName by remember(editingProduct) {
        mutableStateOf(editingProduct?.locationName.orEmpty())
    }
    var originCity by remember(editingProduct) {
        mutableStateOf(editingProduct?.originCity ?: activeCity)
    }
    var tagWithGps by remember(editingProduct) { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editingProduct == null) "Add Product with Location & Cost" else "Edit Product Listing",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Store Type Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreType.entries.forEach { type ->
                    FilterChip(
                        selected = storeType == type,
                        onClick = {
                            storeType = type
                            val nextCats = if (type == StoreType.GROCERY) {
                                MarketplaceCatalog.groceryCategories
                            } else {
                                MarketplaceCatalog.shoppingCategories
                            }
                            if (category !in nextCats) category = nextCats.first()
                        },
                        label = { Text(type.label) }
                    )
                }
            }

            // Category chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, maxLines = 1) }
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name *") },
                placeholder = { Text("e.g., Fresh Mangoes or Ceramic Mug") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_product_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Cost ($) *") },
                    placeholder = { Text("4.99") },
                    leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_product_cost_input")
                )
                OutlinedTextField(
                    value = unitLabel,
                    onValueChange = { unitLabel = it },
                    label = { Text("Unit *") },
                    placeholder = { Text("1 kg / each") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Store / Aisle Location *") },
                placeholder = { Text("e.g., Downtown Market · Aisle 4") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_product_location_input")
            )

            OutlinedTextField(
                value = originCity,
                onValueChange = { originCity = it },
                label = { Text("City / Origin *") },
                placeholder = { Text("e.g., San Francisco, CA") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pin to active location coordinates",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Calculates live distance from your delivery point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tagWithGps,
                    onCheckedChange = { tagWithGps = it }
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val parsedPrice = priceText.trim().toDoubleOrNull()
                        when {
                            name.trim().length < 2 -> validationError = "Enter at least 2 characters for the name"
                            parsedPrice == null || parsedPrice <= 0.0 -> validationError = "Enter a valid cost greater than $0.00"
                            locationName.trim().length < 2 -> validationError = "Enter the store or aisle location"
                            originCity.trim().length < 2 -> validationError = "Enter the origin city or region"
                            unitLabel.trim().isEmpty() -> validationError = "Enter a unit label (e.g. each, 1 lb)"
                            else -> {
                                onSave(
                                    editingProduct?.id,
                                    name.trim(),
                                    description.trim().ifBlank { null },
                                    storeType,
                                    category,
                                    parsedPrice,
                                    unitLabel.trim(),
                                    locationName.trim(),
                                    originCity.trim(),
                                    tagWithGps
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_custom_product_button")
                ) {
                    Text(if (editingProduct == null) "Save Product" else "Update Product")
                }
            }
        }
    }
}
