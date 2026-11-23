package com.example.data.catalog

import com.example.data.model.Product
import com.example.data.model.StoreType

object MarketplaceCatalog {

    val groceryCategories = listOf(
        "Produce",
        "Dairy & Eggs",
        "Bakery",
        "Pantry"
    )

    val shoppingCategories = listOf(
        "Home & Kitchen",
        "Personal Care",
        "Electronics",
        "Lifestyle"
    )

    val allCategories = groceryCategories + shoppingCategories

    val curatedProducts: List<Product> = listOf(
        // GROCERIES — Produce
        Product(
            id = "groc_avocado",
            name = "Organic Hass Avocados",
            description = "Creamy, ripe-ready organic avocados harvested at peak oil content for toast, salads, and guacamole.",
            storeType = StoreType.GROCERY,
            category = "Produce",
            price = 4.49,
            originalPrice = 5.49,
            unitLabel = "4-pack (680g)",
            locationName = "Westside Market Hub · Aisle 1A",
            originCity = "Salinas Valley, CA",
            latitude = 37.7825,
            longitude = -122.4180,
            rating = 4.9,
            badge = "Save 18%"
        ),
        Product(
            id = "groc_heirloom_tomatoes",
            name = "Heirloom Mixed Tomatoes",
            description = "Sun-ripened red, golden, and green-stripe tomatoes bursting with sweet summer acidity.",
            storeType = StoreType.GROCERY,
            category = "Produce",
            price = 5.29,
            unitLabel = "1.5 lb basket",
            locationName = "Westside Market Hub · Aisle 1B",
            originCity = "Petaluma Farms, CA",
            latitude = 37.7825,
            longitude = -122.4180,
            rating = 4.8,
            badge = "Farm Direct"
        ),
        Product(
            id = "groc_baby_spinach",
            name = "Triple-Washed Baby Spinach",
            description = "Tender organic spinach leaves crisp-chilled within hours of harvest.",
            storeType = StoreType.GROCERY,
            category = "Produce",
            price = 3.79,
            unitLabel = "280g clamshell",
            locationName = "Downtown Fresh Depot · Cold Bay 2",
            originCity = "Watsonville, CA",
            latitude = 37.7890,
            longitude = -122.4015,
            rating = 4.7
        ),
        Product(
            id = "groc_honeycrisp",
            name = "Honeycrisp Mountain Apples",
            description = "Snappy, honey-sweet apples grown in cool mountain orchards.",
            storeType = StoreType.GROCERY,
            category = "Produce",
            price = 6.49,
            originalPrice = 7.49,
            unitLabel = "2 lb bag",
            locationName = "Downtown Fresh Depot · Aisle 2A",
            originCity = "Yakima Valley, WA",
            latitude = 37.7890,
            longitude = -122.4015,
            rating = 4.9,
            badge = "Best Seller"
        ),

        // GROCERIES — Dairy & Eggs
        Product(
            id = "groc_pasture_eggs",
            name = "Pasture-Raised Brown Eggs",
            description = "Rich amber-yolk Grade A large eggs from free-roaming hens on open clover pastures.",
            storeType = StoreType.GROCERY,
            category = "Dairy & Eggs",
            price = 6.99,
            unitLabel = "12 count",
            locationName = "Northgate Cold Room · Dairy 4",
            originCity = "Sonoma County, CA",
            latitude = 37.7985,
            longitude = -122.4090,
            rating = 4.9,
            badge = "Local"
        ),
        Product(
            id = "groc_greek_yogurt",
            name = "Whole Milk Greek Yogurt",
            description = "Thick, velvety strained yogurt with live probiotic cultures and zero added sugar.",
            storeType = StoreType.GROCERY,
            category = "Dairy & Eggs",
            price = 4.99,
            unitLabel = "900g tub",
            locationName = "Northgate Cold Room · Dairy 2",
            originCity = "Modesto, CA",
            latitude = 37.7985,
            longitude = -122.4090,
            rating = 4.8
        ),
        Product(
            id = "groc_oat_barista",
            name = "Barista Oat Milk Blend",
            description = "Silky micro-foamable oat milk crafted for espresso lattes, matcha, and cereal.",
            storeType = StoreType.GROCERY,
            category = "Dairy & Eggs",
            price = 4.29,
            originalPrice = 4.99,
            unitLabel = "1L carton",
            locationName = "Westside Market Hub · Chilled 5",
            originCity = "Portland, OR",
            latitude = 37.7825,
            longitude = -122.4180,
            rating = 4.7
        ),

        // GROCERIES — Bakery
        Product(
            id = "groc_sourdough",
            name = "Country Levain Sourdough",
            description = "36-hour slow-fermented artisan loaf with blistered golden crust and open tender crumb.",
            storeType = StoreType.GROCERY,
            category = "Bakery",
            price = 6.50,
            unitLabel = "750g loaf",
            locationName = "Mission Bakehouse · Oven Counter",
            originCity = "San Francisco, CA",
            latitude = 37.7640,
            longitude = -122.4210,
            rating = 5.0,
            badge = "Baked Today"
        ),
        Product(
            id = "groc_croissants",
            name = "French Butter Croissants",
            description = "Flaky laminated pastry layers made with cultured Normandy-style butter.",
            storeType = StoreType.GROCERY,
            category = "Bakery",
            price = 7.99,
            unitLabel = "4-pack box",
            locationName = "Mission Bakehouse · Pastry Rack 1",
            originCity = "San Francisco, CA",
            latitude = 37.7640,
            longitude = -122.4210,
            rating = 4.9
        ),

        // GROCERIES — Pantry
        Product(
            id = "groc_olive_oil",
            name = "Cold-Pressed Extra Virgin Olive Oil",
            description = "First-harvest single-estate olive oil with peppery green almond finish.",
            storeType = StoreType.GROCERY,
            category = "Pantry",
            price = 16.99,
            originalPrice = 19.99,
            unitLabel = "500ml glass",
            locationName = "Downtown Fresh Depot · Aisle 6C",
            originCity = "Napa Valley, CA",
            latitude = 37.7890,
            longitude = -122.4015,
            rating = 4.9,
            badge = "Save $3"
        ),
        Product(
            id = "groc_espresso_beans",
            name = "Single-Origin Arabica Whole Beans",
            description = "Medium-roast washed beans with notes of caramel, cocoa nib, and bergamot.",
            storeType = StoreType.GROCERY,
            category = "Pantry",
            price = 14.50,
            unitLabel = "340g bag",
            locationName = "Mission Bakehouse · Roastery Wall",
            originCity = "Nyeri Highlands",
            latitude = 37.7640,
            longitude = -122.4210,
            rating = 4.9,
            badge = "Top Rated"
        ),
        Product(
            id = "groc_raw_honey",
            name = "Wildflower Raw Comb Honey",
            description = "Unfiltered seasonal wildflower honey bottled straight from local apiaries.",
            storeType = StoreType.GROCERY,
            category = "Pantry",
            price = 11.49,
            unitLabel = "450g jar",
            locationName = "Westside Market Hub · Aisle 7A",
            originCity = "Marin County, CA",
            latitude = 37.7825,
            longitude = -122.4180,
            rating = 4.8
        ),

        // ONLINE SHOPPING — Home & Kitchen
        Product(
            id = "shop_kettle",
            name = "Matte Ceramic Gooseneck Kettle",
            description = "Precision pour-over kettle with walnut handle, balanced spout, and heat-retaining stoneware body.",
            storeType = StoreType.SHOPPING,
            category = "Home & Kitchen",
            price = 48.00,
            originalPrice = 58.00,
            unitLabel = "900ml kettle",
            locationName = "SoMa Lifestyle Hub · Zone H2",
            originCity = "Portland, OR",
            latitude = 37.7785,
            longitude = -122.3980,
            rating = 4.9,
            badge = "17% Off"
        ),
        Product(
            id = "shop_linen_towels",
            name = "Waffle-Weave Organic Kitchen Towels",
            description = "Ultra-absorbent European flax linen towels pre-washed for soft everyday drape.",
            storeType = StoreType.SHOPPING,
            category = "Home & Kitchen",
            price = 22.50,
            unitLabel = "Set of 2",
            locationName = "SoMa Lifestyle Hub · Shelf 4D",
            originCity = "Porto, Portugal",
            latitude = 37.7785,
            longitude = -122.3980,
            rating = 4.8
        ),
        Product(
            id = "shop_soy_candle",
            name = "Sandalwood & Amber Soy Candle",
            description = "Clean-burning hand-poured coconut-soy wax candle in reusable amber apothecary glass.",
            storeType = StoreType.SHOPPING,
            category = "Home & Kitchen",
            price = 24.00,
            unitLabel = "220g (50h burn)",
            locationName = "SoMa Lifestyle Hub · Shelf 3A",
            originCity = "Brooklyn, NY",
            latitude = 37.7785,
            longitude = -122.3980,
            rating = 4.7
        ),

        // ONLINE SHOPPING — Personal Care
        Product(
            id = "shop_hand_wash",
            name = "Botanical Cedar & Sage Hand Wash",
            description = "Gentle pH-balanced botanical cleanser with aloe vera and essential cedarwood oil.",
            storeType = StoreType.SHOPPING,
            category = "Personal Care",
            price = 15.00,
            unitLabel = "400ml pump",
            locationName = "Marina Wellness Hub · Aisle C1",
            originCity = "Los Angeles, CA",
            latitude = 37.8010,
            longitude = -122.4360,
            rating = 4.8
        ),
        Product(
            id = "shop_mineral_spf",
            name = "Sheer Daily Mineral Sunscreen SPF 50",
            description = "Weightless non-nano zinc formula that blends cleanly without white cast.",
            storeType = StoreType.SHOPPING,
            category = "Personal Care",
            price = 26.00,
            originalPrice = 30.00,
            unitLabel = "60ml tube",
            locationName = "Marina Wellness Hub · Aisle C3",
            originCity = "Seoul, KR",
            latitude = 37.8010,
            longitude = -122.4360,
            rating = 4.9,
            badge = "Best Seller"
        ),

        // ONLINE SHOPPING — Electronics
        Product(
            id = "shop_power_bank",
            name = "MagSlim 10,000mAh USB-C Power Bank",
            description = "Pocket-sized anodized aluminum battery pack with 30W fast PD charging and LED percentage readout.",
            storeType = StoreType.SHOPPING,
            category = "Electronics",
            price = 39.99,
            originalPrice = 49.99,
            unitLabel = "1 unit",
            locationName = "Bayfront Tech Locker · Bay T4",
            originCity = "Austin, TX",
            latitude = 37.7915,
            longitude = -122.3940,
            rating = 4.8,
            badge = "Flash Deal"
        ),
        Product(
            id = "shop_anc_earbuds",
            name = "Acoustic Pure Wireless ANC Earbuds",
            description = "Hybrid active noise cancellation, 32-hour pocket case, and crystal-clear call mics.",
            storeType = StoreType.SHOPPING,
            category = "Electronics",
            price = 79.00,
            unitLabel = "1 pair + case",
            locationName = "Bayfront Tech Locker · Bay T1",
            originCity = "San Jose, CA",
            latitude = 37.7915,
            longitude = -122.3940,
            rating = 4.7
        ),

        // ONLINE SHOPPING — Lifestyle
        Product(
            id = "shop_canvas_tote",
            name = "Heavyweight Organic Market Tote",
            description = "16oz waxed-base organic canvas carryall with interior bottle pockets for grocery runs.",
            storeType = StoreType.SHOPPING,
            category = "Lifestyle",
            price = 28.00,
            unitLabel = "18L tote",
            locationName = "SoMa Lifestyle Hub · Rack L2",
            originCity = "Oakland, CA",
            latitude = 37.7785,
            longitude = -122.3980,
            rating = 4.9,
            badge = "Eco Pick"
        ),
        Product(
            id = "shop_insulated_flask",
            name = "Trail Insulated Stainless Bottle",
            description = "Triple-wall vacuum bottle keeping drinks ice-cold for 24h or piping hot for 12h.",
            storeType = StoreType.SHOPPING,
            category = "Lifestyle",
            price = 32.00,
            originalPrice = 36.00,
            unitLabel = "750ml bottle",
            locationName = "Marina Wellness Hub · Rack L5",
            originCity = "Bend, OR",
            latitude = 37.8010,
            longitude = -122.4360,
            rating = 4.8
        )
    )
}
