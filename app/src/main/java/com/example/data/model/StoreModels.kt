package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.ceil

enum class StoreCategory(
    val id: String,
    val label: String,
) {
    GAMING("gaming", "Coins & Jeux"),
    STREAMING("streaming", "Streaming & TV"),
    TELECOM("telecom", "Starlink & Télécom"),
    CARDS("cards", "Cartes & VCC"),
    AI_PRO("ai_pro", "IA & Outils Pro"),
    WEB_TRAVEL("web_travel", "Web & Réservations");

    companion object {
        fun fromId(id: String): StoreCategory = when (id.lowercase()) {
            "netflix", "streaming" -> STREAMING
            "starlink", "telecom" -> TELECOM
            else -> entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GAMING
        }
    }
}

enum class StoreDeliveryType(
    val id: String,
    val label: String,
    val shortBadge: String,
) {
    TOPUP("topup", "API Reloadly Top-Up", "API DIRECT"),
    PIN("pin", "Code PIN Instantané", "PIN INSTANT"),
    NETFLIX_PROFILE("netflix_profile", "Stock Auto / Profil 4K", "STOCK AUTO"),
    STARLINK("starlink", "Règlement via Carte Meru VCC", "MERU VCC");

    companion object {
        fun fromId(id: String): StoreDeliveryType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TOPUP
    }
}

enum class StoreOrderStatus(val label: String) {
    PENDING("En attente"),
    SUBMITTED("Preuve soumise"),
    PAID("Payé (En cours API)"),
    DELIVERED("Livré"),
    REJECTED("Refusé"),
    EXPIRED("Expiré");

    companion object {
        fun fromString(value: String): StoreOrderStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

enum class StoreFulfillmentMode(val id: String, val label: String) {
    AUTO("AUTO", "Mode Automatique"),
    MANUAL("MANUAL", "Mode Manuel");

    companion object {
        fun fromId(id: String): StoreFulfillmentMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}

data class StoreProduct(
    val id: String,
    val category: StoreCategory,
    val deliveryType: StoreDeliveryType,
    val badge: String,
    val title: String,
    val subtitle: String,
    val usdCost: Double,
    val usdSell: Double,
    val requiresTargetAccount: Boolean,
    val targetAccountLabel: String = "",
    val targetAccountHint: String = "",
    val selectLabel: String = "",
    val selectOptions: List<String> = emptyList(),
    val stock: Int = 999,
    val isCustom: Boolean = false,
)

@Keep
@IgnoreExtraProperties
data class CustomStoreProduct(
    @DocumentId val id: String = "",
    val category: String = StoreCategory.GAMING.id,
    val deliveryType: String = StoreDeliveryType.STARLINK.id,
    val badge: String = "NOUVEAU",
    val title: String = "",
    val subtitle: String = "",
    val usdCost: Double = 0.0,
    val usdSell: Double = 0.0,
    val requiresTargetAccount: Boolean = true,
    val targetAccountLabel: String = "Email ou Identifiant de livraison",
    val targetAccountHint: String = "client@email.com / +509...",
    val selectLabel: String = "",
    val selectOptions: List<String> = emptyList(),
    val stock: Int = 100,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toStoreProduct(): StoreProduct = StoreProduct(
        id = id,
        category = StoreCategory.fromId(category),
        deliveryType = StoreDeliveryType.fromId(deliveryType),
        badge = badge,
        title = title,
        subtitle = subtitle,
        usdCost = usdCost,
        usdSell = usdSell,
        requiresTargetAccount = requiresTargetAccount,
        targetAccountLabel = targetAccountLabel,
        targetAccountHint = targetAccountHint,
        selectLabel = selectLabel,
        selectOptions = selectOptions,
        stock = stock,
        isCustom = true,
    )
}

object StoreCatalog {
    val DEFAULT_PRODUCTS: List<StoreProduct> = listOf(
        // --- 1. COINS & JEUX ---
        StoreProduct(
            id = "ff_100",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.TOPUP,
            badge = "FREE FIRE",
            title = "100 + 10 Diamants",
            subtitle = "Livraison Directe via Player ID",
            usdCost = 0.95,
            usdSell = 1.35,
            requiresTargetAccount = true,
            targetAccountLabel = "Player ID / UID Free Fire",
            targetAccountHint = "Ex: 2489104572",
        ),
        StoreProduct(
            id = "ff_520",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.TOPUP,
            badge = "FREE FIRE",
            title = "520 + 52 Diamants",
            subtitle = "Recharge API Reloadly Instantanée",
            usdCost = 4.65,
            usdSell = 6.20,
            requiresTargetAccount = true,
            targetAccountLabel = "Player ID / UID Free Fire",
            targetAccountHint = "Ex: 2489104572",
        ),
        StoreProduct(
            id = "pubg_325",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.PIN,
            badge = "PUBG MOBILE",
            title = "325 UC Global",
            subtitle = "Code PIN Global (Midasbuy / EZPin)",
            usdCost = 4.50,
            usdSell = 5.90,
            requiresTargetAccount = false,
            targetAccountLabel = "PUBG Player ID (optionnel)",
            targetAccountHint = "Ex: 5192837465",
            stock = 48,
        ),
        StoreProduct(
            id = "ml_257",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.TOPUP,
            badge = "MOBILE LEGENDS",
            title = "257 Diamants + Bonus",
            subtitle = "User ID + Zone ID Requis",
            usdCost = 4.20,
            usdSell = 5.60,
            requiresTargetAccount = true,
            targetAccountLabel = "User ID + Zone ID",
            targetAccountHint = "Ex: 12345678 (2045)",
        ),
        StoreProduct(
            id = "roblox_10",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.PIN,
            badge = "ROBLOX US",
            title = "Carte Cadeau $10",
            subtitle = "Code PIN Digital 800 Robux",
            usdCost = 9.40,
            usdSell = 11.50,
            requiresTargetAccount = false,
            targetAccountLabel = "Email ou WhatsApp (optionnel)",
            targetAccountHint = "client@email.com / +509...",
            stock = 32,
        ),
        StoreProduct(
            id = "psn_20",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.PIN,
            badge = "PLAYSTATION US",
            title = "Carte PSN $20 USD",
            subtitle = "Code PSN Compte USA",
            usdCost = 18.80,
            usdSell = 22.50,
            requiresTargetAccount = false,
            targetAccountLabel = "Email de livraison (optionnel)",
            targetAccountHint = "gamer@email.com",
            stock = 19,
        ),
        StoreProduct(
            id = "steam_xbox_25",
            category = StoreCategory.GAMING,
            deliveryType = StoreDeliveryType.PIN,
            badge = "STEAM / XBOX / RAZER",
            title = "Carte Gaming Multi-Store $25",
            subtitle = "Steam Wallet, Xbox Live ou Razer Gold PIN",
            usdCost = 23.00,
            usdSell = 27.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email de réception du code",
            targetAccountHint = "joueur@email.com",
            selectLabel = "Plateforme souhaitée",
            selectOptions = listOf(
                "Steam Wallet US ($25)",
                "Xbox Game Pass / Gift ($25)",
                "Razer Gold Global ($25)",
            ),
            stock = 25,
        ),

        // --- 2. STREAMING & TV ---
        StoreProduct(
            id = "nf_1",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.NETFLIX_PROFILE,
            badge = "NETFLIX 4K",
            title = "Netflix Ultra HD (1 Mois)",
            subtitle = "Profil Privé PIN ou Compte Complet • Meru VCC",
            usdCost = 3.50,
            usdSell = 5.50,
            requiresTargetAccount = false,
            targetAccountLabel = "WhatsApp ou Email (optionnel)",
            targetAccountHint = "+509 3700-0000 ou email@mail.com",
            selectLabel = "Formule Netflix",
            selectOptions = listOf(
                "1 Profil Privé PIN (4K UHD)",
                "Renouvellement Compte Existant",
            ),
            stock = 42,
        ),
        StoreProduct(
            id = "nf_card15",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.PIN,
            badge = "CARTE CADEAU",
            title = "Netflix Gift Card $15",
            subtitle = "Code Recharge Officiel (EZPin)",
            usdCost = 14.30,
            usdSell = 17.50,
            requiresTargetAccount = false,
        ),
        StoreProduct(
            id = "nf_card25",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.PIN,
            badge = "CARTE CADEAU",
            title = "Netflix Gift Card $25",
            subtitle = "Code Recharge Officiel USA",
            usdCost = 23.80,
            usdSell = 28.50,
            requiresTargetAccount = false,
        ),
        StoreProduct(
            id = "nf_full",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.NETFLIX_PROFILE,
            badge = "COMPTE COMPLET",
            title = "Netflix 4K (5 Écrans) 30J",
            subtitle = "Accès Email Complet Privé",
            usdCost = 17.50,
            usdSell = 24.00,
            requiresTargetAccount = false,
        ),
        StoreProduct(
            id = "iptv_pro",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.TOPUP,
            badge = "IPTV SPORT & TV",
            title = "IPTV Pro 4K — Ligue des Champions & Liga",
            subtitle = "+18,000 Chaînes (beIN, Canal+, Premier League, VOD)",
            usdCost = 8.00,
            usdSell = 12.00,
            requiresTargetAccount = true,
            targetAccountLabel = "WhatsApp / Email (Code Xtream)",
            targetAccountHint = "+509 ... ou client@email.com",
            selectLabel = "Durée & Application",
            selectOptions = listOf(
                "1 Mois (Xtream / M3U)",
                "3 Mois Pack Foot",
                "6 Mois VIP",
                "12 Mois Intégral",
            ),
            stock = 150,
        ),
        StoreProduct(
            id = "spotify_apple",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "MUSIQUE HD",
            title = "Spotify Premium / Apple Music (1 Mois)",
            subtitle = "Activation sur votre compte ou invitation Famille",
            usdCost = 3.20,
            usdSell = 4.90,
            requiresTargetAccount = true,
            targetAccountLabel = "Email du compte Spotify / Apple ID",
            targetAccountHint = "musique@email.com",
            selectLabel = "Service Musical",
            selectOptions = listOf(
                "Spotify Premium Individuel",
                "Spotify Duo / Famille",
                "Apple Music Individuel",
            ),
            stock = 80,
        ),
        StoreProduct(
            id = "prime_disney_yt",
            category = StoreCategory.STREAMING,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "PACK STREAMING",
            title = "Prime Video / Disney+ / YouTube Premium",
            subtitle = "Sans publicité • Paiement réglé via Carte Meru",
            usdCost = 4.50,
            usdSell = 6.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email du compte à activer",
            targetAccountHint = "client@gmail.com",
            selectLabel = "Plateforme choisie",
            selectOptions = listOf(
                "YouTube Premium + Music (1 Mois)",
                "Disney+ Premium 4K (1 Mois)",
                "Amazon Prime Video (1 Mois)",
            ),
            stock = 65,
        ),

        // --- 3. STARLINK & TÉLÉCOM / eSIM ---
        StoreProduct(
            id = "sl_res",
            category = StoreCategory.TELECOM,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "HAÏTI / DOM",
            title = "Starlink Résidentiel ($60)",
            subtitle = "Paiement Mensuel via Carte Meru USD",
            usdCost = 60.00,
            usdSell = 68.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Email Starlink ou Numéro KIT / ACC",
            targetAccountHint = "Ex: ACC-2948102-84 ou email@starlink.com",
        ),
        StoreProduct(
            id = "sl_roam100",
            category = StoreCategory.TELECOM,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "VOYAGE",
            title = "Starlink Roam 100 Go ($55)",
            subtitle = "Facture Roam Réglée via Meru",
            usdCost = 55.00,
            usdSell = 63.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Email Starlink ou Numéro KIT / ACC",
            targetAccountHint = "Ex: ACC-2948102-84",
        ),
        StoreProduct(
            id = "sl_roam_unlim",
            category = StoreCategory.TELECOM,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "POPULAIRE",
            title = "Starlink Roam Illimité ($100)",
            subtitle = "Renouvellement Global via Carte Meru",
            usdCost = 100.00,
            usdSell = 110.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Email Starlink ou Numéro KIT / ACC",
            targetAccountHint = "Ex: ACC-2948102-84",
        ),
        StoreProduct(
            id = "sl_priority",
            category = StoreCategory.TELECOM,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "ENTREPRISE",
            title = "Starlink Priority ($140)",
            subtitle = "Règlement Express B2B via Meru",
            usdCost = 140.00,
            usdSell = 152.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Email Starlink ou Numéro KIT / ACC",
            targetAccountHint = "Ex: ACC-2948102-84",
        ),
        StoreProduct(
            id = "topup_haiti",
            category = StoreCategory.TELECOM,
            deliveryType = StoreDeliveryType.TOPUP,
            badge = "DIGICEL / NATCOM",
            title = "Recharge Mobile & Plan Data Haïti",
            subtitle = "Top-up instantané Digicel & Natcom (+509)",
            usdCost = 4.30,
            usdSell = 5.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Numéro de téléphone Haïti (+509)",
            targetAccountHint = "+509 37 12 3456",
            selectLabel = "Opérateur & Forfait",
            selectOptions = listOf(
                "Digicel — Recharge Crédit ($5 USD eq.)",
                "Digicel — Plan Internet Illimité 7j",
                "Natcom — Recharge Crédit ($5 USD eq.)",
                "Natcom — Plan Data Giga+",
            ),
        ),
        StoreProduct(
            id = "esim_travel",
            category = StoreCategory.TELECOM,
            deliveryType = StoreDeliveryType.PIN,
            badge = "eSIM VOYAGE",
            title = "eSIM Internationale (USA / RD / Canada)",
            subtitle = "Airalo & Holafly • QR Code immédiat avant décollage",
            usdCost = 10.50,
            usdSell = 14.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email pour recevoir le QR Code eSIM",
            targetAccountHint = "voyageur@email.com",
            selectLabel = "Destination & Data",
            selectOptions = listOf(
                "République Dominicaine — 5 Go / 15 Jours",
                "États-Unis (USA) — 10 Go / 30 Jours",
                "Canada — 5 Go / 15 Jours",
                "Multi-Pays Amériques — 10 Go",
            ),
            stock = 120,
        ),

        // --- 4. CARTES & VCC ---
        StoreProduct(
            id = "meru_vcc_10",
            category = StoreCategory.CARDS,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "MERU VCC VISA",
            title = "Carte Virtuelle Préchargée $10 USD",
            subtitle = "Compatible Shein, Temu, Amazon, PayPal, Netflix",
            usdCost = 10.20,
            usdSell = 12.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email / WhatsApp pour détails de la carte",
            targetAccountHint = "acheteur@email.com",
            stock = 60,
        ),
        StoreProduct(
            id = "meru_vcc_25",
            category = StoreCategory.CARDS,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "MERU VCC VISA",
            title = "Carte Virtuelle Préchargée $25 USD",
            subtitle = "Numéro Visa/Mastercard + CVV + Adresse US",
            usdCost = 25.40,
            usdSell = 28.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email / WhatsApp de livraison sécurisée",
            targetAccountHint = "acheteur@email.com",
            stock = 45,
        ),
        StoreProduct(
            id = "meru_vcc_50",
            category = StoreCategory.CARDS,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "MERU VCC PRO",
            title = "Carte Virtuelle Préchargée $50 USD",
            subtitle = "Idéal achats en gros, Ads Facebook/TikTok & vols",
            usdCost = 50.50,
            usdSell = 55.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Email / WhatsApp de livraison sécurisée",
            targetAccountHint = "pro@email.com",
            stock = 30,
        ),
        StoreProduct(
            id = "apple_google_15",
            category = StoreCategory.CARDS,
            deliveryType = StoreDeliveryType.PIN,
            badge = "APP STORE / PLAY",
            title = "Carte Cadeau Apple iTunes / Google Play $15",
            subtitle = "Code US officiel pour iCloud, apps & jeux",
            usdCost = 14.20,
            usdSell = 17.20,
            requiresTargetAccount = true,
            targetAccountLabel = "Email de réception du code",
            targetAccountHint = "user@icloud.com",
            selectLabel = "Boutique d'application",
            selectOptions = listOf(
                "Apple App Store & iTunes US ($15)",
                "Google Play Store US ($15)",
                "Amazon.com Gift Card ($15)",
            ),
            stock = 75,
        ),

        // --- 5. IA & OUTILS PRO ---
        StoreProduct(
            id = "chatgpt_claude",
            category = StoreCategory.AI_PRO,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "INTELLIGENCE ARTIFICIELLE",
            title = "ChatGPT Plus (GPT-4o) / Claude Pro (1 Mois)",
            subtitle = "Abonnement officiel réglé par carte internationale",
            usdCost = 20.00,
            usdSell = 23.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email du compte OpenAI / Claude",
            targetAccountHint = "createur@email.com",
            selectLabel = "Modèle IA choisi",
            selectOptions = listOf(
                "ChatGPT Plus (OpenAI — 1 Mois)",
                "Claude Pro (Anthropic — 1 Mois)",
                "Midjourney Basic Plan (1 Mois)",
            ),
        ),
        StoreProduct(
            id = "canva_capcut",
            category = StoreCategory.AI_PRO,
            deliveryType = StoreDeliveryType.NETFLIX_PROFILE,
            badge = "CRÉATION & DESIGN",
            title = "Canva Pro / CapCut Pro (1 Mois)",
            subtitle = "Export 4K sans filigrane & outils IA débloqués",
            usdCost = 4.50,
            usdSell = 7.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email du compte Canva ou CapCut",
            targetAccountHint = "designer@email.com",
            selectLabel = "Logiciel créatif",
            selectOptions = listOf(
                "Canva Pro Équipe/Individuel (1 Mois)",
                "CapCut Pro Mobile & Desktop (1 Mois)",
                "InShot Pro / Picsart Gold",
            ),
            stock = 110,
        ),
        StoreProduct(
            id = "ms365_adobe",
            category = StoreCategory.AI_PRO,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "PRODUCTIVITÉ PRO",
            title = "Microsoft 365 / Google Workspace / Adobe CC",
            subtitle = "Licences officielles entreprises, graphistes & étudiants",
            usdCost = 10.00,
            usdSell = 14.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Email professionnel de licence",
            targetAccountHint = "contact@entreprise.ht",
            selectLabel = "Suite logicielle",
            selectOptions = listOf(
                "Microsoft 365 Personnel (Word, Excel, 1To)",
                "Google Workspace Business Starter",
                "Adobe Creative Cloud (Photoshop + Premiere)",
            ),
            stock = 50,
        ),
        StoreProduct(
            id = "vpn_verified",
            category = StoreCategory.AI_PRO,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "SÉCURITÉ & RÉSEAUX",
            title = "NordVPN / ExpressVPN / Meta Verified",
            subtitle = "Protection IP dédiée ou badge certifié Instagram/FB",
            usdCost = 12.00,
            usdSell = 16.50,
            requiresTargetAccount = true,
            targetAccountLabel = "Email ou @NomUtilisateur à activer",
            targetAccountHint = "@moncompte ou email@mail.com",
            selectLabel = "Service souhaité",
            selectOptions = listOf(
                "NordVPN / Surfshark (1 Mois)",
                "ExpressVPN Premium (1 Mois)",
                "Meta Verified (Instagram / Facebook)",
                "LinkedIn Premium Career",
            ),
            stock = 70,
        ),

        // --- 6. WEB & RÉSERVATIONS ---
        StoreProduct(
            id = "domain_hosting",
            category = StoreCategory.WEB_TRAVEL,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "WEB & CLOUD",
            title = "Nom de Domaine (.com/.net) + Hébergement",
            subtitle = "Enregistrement Namecheap / Hostinger payé en HTG",
            usdCost = 13.00,
            usdSell = 18.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Nom de domaine souhaité & Email admin",
            targetAccountHint = "Ex: monentreprise.com — admin@mail.com",
        ),
        StoreProduct(
            id = "exams_bookings",
            category = StoreCategory.WEB_TRAVEL,
            deliveryType = StoreDeliveryType.STARLINK,
            badge = "VOYAGES & EXAMENS",
            title = "Paiement Billets, Hôtels & Examens (TOEFL/GRE)",
            subtitle = "Règlement en USD sur Booking, Airbnb, ETS.org",
            usdCost = 45.00,
            usdSell = 50.00,
            requiresTargetAccount = true,
            targetAccountLabel = "Lien de paiement ou référence dossier + Email",
            targetAccountHint = "Ex: Inscription TOEFL #84920 — nom@mail.com",
        ),
    )

    fun mergeCatalog(customProducts: List<StoreProduct>): List<StoreProduct> {
        val customIds = customProducts.mapTo(HashSet()) { it.id }
        return customProducts + DEFAULT_PRODUCTS.filterNot { it.id in customIds }
    }

    fun findById(id: String, catalog: List<StoreProduct> = DEFAULT_PRODUCTS): StoreProduct? =
        catalog.firstOrNull { it.id == id } ?: DEFAULT_PRODUCTS.firstOrNull { it.id == id }
}

@Keep
@IgnoreExtraProperties
data class StoreConfig(
    val baseRateHtg: Double = 132.50,
    val markupPercent: Double = 7.0,
    val reloadlyBalanceUsd: Double = 420.50,
    val meruBalanceUsd: Double = 185.00,
    val reloadlyActive: Boolean = true,
    val ezpinActive: Boolean = true,
    val moncashActive: Boolean = true,
    val natcashActive: Boolean = true,
    val binanceActive: Boolean = true,
    val nowpaymentsActive: Boolean = true,
    val reloadlyLatencyMs: Int = 28,
    val ezpinLatencyMs: Int = 42,
    val availableNetflixCount: Int = 0,
    val pendingStarlinkCount: Int = 0,
    val fulfillmentMode: String = StoreFulfillmentMode.AUTO.id,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun effectiveRate(): Double = baseRateHtg * (1.0 + markupPercent / 100.0)

    fun calculatePriceHtg(usdSell: Double): Double = ceil(usdSell * effectiveRate())

    fun isAutoFulfillment(): Boolean =
        StoreFulfillmentMode.fromId(fulfillmentMode) == StoreFulfillmentMode.AUTO

    fun isGatewayEnabled(method: PaymentMethod): Boolean = when (method) {
        PaymentMethod.MONCASH -> moncashActive
        PaymentMethod.NATCASH -> natcashActive
        PaymentMethod.BINANCE -> binanceActive
        PaymentMethod.NOWPAYMENTS -> nowpaymentsActive
        PaymentMethod.MERU -> false
    }
}

@Keep
@IgnoreExtraProperties
data class StoreOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val orderCode: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val category: String = "gaming",
    val deliveryType: String = "topup",
    val usdCost: Double = 0.0,
    val usdSell: Double = 0.0,
    val amountHtg: Double = 0.0,
    val effectiveRate: Double = 0.0,
    val gateway: String = "moncash",
    val targetAccount: String = "",
    val externalTxRef: String = "",
    val gatewayOrderId: String = "",
    val checkoutUrl: String = "",
    val gatewayStatus: String = "NONE",
    val status: String = "PENDING",
    val deliverable: String = "",
    val adminNote: String = "",
    val logs: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class NetflixInventoryItem(
    @DocumentId val id: String = "",
    val accountEmail: String = "",
    val accountPassword: String = "",
    val profileName: String = "",
    val profilePin: String = "",
    val status: String = "AVAILABLE",
    val assignedOrderId: String = "",
    val assignedUserId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class StoreProviderHealth(
    val provider: String,
    val mode: String,
    val configured: Boolean,
    val latencyMs: Int,
    val status: String,
)
