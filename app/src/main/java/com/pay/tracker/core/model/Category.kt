package com.pay.tracker.core.model

/**
 * Domain model representing an expense category with associated keywords.
 */
data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val defaultKeywords: List<String> = emptyList()
) {
    companion object {
        val FOOD = Category(
            id = "food",
            name = "Food",
            iconName = "Restaurant",
            defaultKeywords = listOf(
                "swiggy", "zomato", "restaurant", "cafe", "mcdonalds", "kfc",
                "burger", "pizza", "starbucks", "barbeque", "eats", "dining"
            )
        )

        val GROCERIES = Category(
            id = "groceries",
            name = "Groceries",
            iconName = "ShoppingCart",
            defaultKeywords = listOf(
                "blinkit", "zepto", "instamart", "bigbasket", "dmart", "supermarket",
                "grocery", "nature's basket", "spencer", "reliance fresh"
            )
        )

        val SHOPPING = Category(
            id = "shopping",
            name = "Shopping",
            iconName = "ShoppingBag",
            defaultKeywords = listOf(
                "amazon", "flipkart", "myntra", "ajio", "zara", "h&m", "retail",
                "clothing", "electronics", "croma", "meesho", "nykaa"
            )
        )

        val TRAVEL = Category(
            id = "travel",
            name = "Travel",
            iconName = "DirectionsCar",
            defaultKeywords = listOf(
                "uber", "ola", "rapido", "irctc", "makemytrip", "flight", "metro",
                "fuel", "petrol", "diesel", "hpcl", "bpcl", "iocl", "toll", "fastag", "goibibo"
            )
        )

        val BILLS = Category(
            id = "bills",
            name = "Bills",
            iconName = "Receipt",
            defaultKeywords = listOf(
                "bescom", "electricity", "water", "gas", "airtel", "jio", "vi",
                "broadband", "wifi", "recharge", "utility", "postpaid", "bill"
            )
        )

        val TRANSFER = Category(
            id = "transfer",
            name = "Transfer",
            iconName = "SwapHoriz",
            defaultKeywords = listOf(
                "sent to", "transfer", "upi", "self", "friend", "family", "rent"
            )
        )

        val ENTERTAINMENT = Category(
            id = "entertainment",
            name = "Entertainment",
            iconName = "Movie",
            defaultKeywords = listOf(
                "bookmyshow", "netflix", "prime", "hotstar", "spotify", "youtube",
                "pvr", "inox", "cinema", "theatre", "game", "steam"
            )
        )

        val HEALTH = Category(
            id = "health",
            name = "Health",
            iconName = "LocalHospital",
            defaultKeywords = listOf(
                "apollo", "pharmacy", "medplus", "1mg", "pharmeasy", "hospital",
                "clinic", "doctor", "diagnostic", "lab", "medicine"
            )
        )

        val DEFAULT_CATEGORIES = listOf(
            FOOD,
            GROCERIES,
            SHOPPING,
            TRAVEL,
            BILLS,
            TRANSFER,
            ENTERTAINMENT,
            HEALTH
        )
    }
}
