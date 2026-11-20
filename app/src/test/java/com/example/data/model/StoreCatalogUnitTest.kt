package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogUnitTest {

    @Test
    fun allSixCategoriesHaveDefaultProducts() {
        StoreCategory.entries.forEach { category ->
            val items = StoreCatalog.DEFAULT_PRODUCTS.filter { it.category == category }
            assertTrue("Category ${category.id} must have products", items.isNotEmpty())
        }
        assertEquals(6, StoreCategory.entries.size)
    }

    @Test
    fun legacyCategoryIdsMapCleanlyToEnrichedCategories() {
        assertEquals(StoreCategory.STREAMING, StoreCategory.fromId("netflix"))
        assertEquals(StoreCategory.TELECOM, StoreCategory.fromId("starlink"))
        assertEquals(StoreCategory.CARDS, StoreCategory.fromId("cards"))
        assertEquals(StoreCategory.AI_PRO, StoreCategory.fromId("ai_pro"))
        assertEquals(StoreCategory.WEB_TRAVEL, StoreCategory.fromId("web_travel"))
    }

    @Test
    fun mergeCatalogPrependsCustomProductsAndDeduplicatesById() {
        val customNew = StoreProduct(
            id = "custom_promo_1",
            category = StoreCategory.CARDS,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "VIP",
            title = "Carte Meru $100",
            subtitle = "Edition spéciale",
            usdCost = 100.0,
            usdSell = 109.0,
            requiresTargetAccount = true,
            isCustom = true,
        )
        val merged = StoreCatalog.mergeCatalog(listOf(customNew))
        assertEquals("custom_promo_1", merged.first().id)
        assertNotNull(StoreCatalog.findById("custom_promo_1", merged))
        assertNotNull(StoreCatalog.findById("ff_100", merged))
    }

    @Test
    fun fulfillmentModeHelpersReflectAutoAndManualModes() {
        val autoCfg = StoreConfig(fulfillmentMode = StoreFulfillmentMode.AUTO.id)
        val manualCfg = StoreConfig(fulfillmentMode = StoreFulfillmentMode.MANUAL.id)

        assertTrue(autoCfg.isAutoFulfillment())
        assertFalse(manualCfg.isAutoFulfillment())
        assertEquals(StoreFulfillmentMode.MANUAL, StoreFulfillmentMode.fromId("manual"))
        assertEquals(StoreFulfillmentMode.AUTO, StoreFulfillmentMode.fromId("unknown"))
    }
}
