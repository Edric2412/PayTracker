package com.pay.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pay.tracker.ui.theme.BillsColor
import com.pay.tracker.ui.theme.EntertainmentColor
import com.pay.tracker.ui.theme.FoodColor
import com.pay.tracker.ui.theme.GroceriesColor
import com.pay.tracker.ui.theme.HealthColor
import com.pay.tracker.ui.theme.OtherColor
import com.pay.tracker.ui.theme.ShoppingColor
import com.pay.tracker.ui.theme.TransferColor
import com.pay.tracker.ui.theme.TravelColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CategoryUiUtils {

    fun getCategoryIcon(categoryName: String): ImageVector {
        return when (categoryName.lowercase().trim()) {
            "food", "food & dining", "dining" -> Icons.Default.Restaurant
            "groceries", "grocery" -> Icons.Default.ShoppingCart
            "shopping" -> Icons.Default.ShoppingBag
            "travel", "travel & commute", "commute", "transport" -> Icons.Default.DirectionsCar
            "bills", "bills & utilities", "utilities" -> Icons.Default.Receipt
            "transfer", "transfers" -> Icons.Default.SwapHoriz
            "entertainment", "movies" -> Icons.Default.Movie
            "health", "medical" -> Icons.Default.LocalHospital
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName.lowercase().trim()) {
            "food", "food & dining", "dining" -> FoodColor
            "groceries", "grocery" -> GroceriesColor
            "shopping" -> ShoppingColor
            "travel", "travel & commute", "commute", "transport" -> TravelColor
            "bills", "bills & utilities", "utilities" -> BillsColor
            "transfer", "transfers" -> TransferColor
            "entertainment", "movies" -> EntertainmentColor
            "health", "medical" -> HealthColor
            else -> OtherColor
        }
    }

    @Composable
    fun CategorySquircleIcon(
        category: String,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp,
        iconSize: Dp = 20.dp,
        shape: Shape = RoundedCornerShape(12.dp)
    ) {
        val color = getCategoryColor(category)
        val icon = getCategoryIcon(category)
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }
    }

    @Composable
    fun CategoryPill(
        category: String,
        modifier: Modifier = Modifier,
        showIcon: Boolean = false
    ) {
        val color = getCategoryColor(category)
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }

    @Composable
    fun StatusPill(
        status: String,
        modifier: Modifier = Modifier
    ) {
        val isConfirmed = status.equals("CONFIRMED", ignoreCase = true)
        val backgroundColor = if (isConfirmed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            com.pay.tracker.ui.theme.PendingAmber.copy(alpha = 0.15f)
        }
        val contentColor = if (isConfirmed) {
            MaterialTheme.colorScheme.primary
        } else {
            com.pay.tracker.ui.theme.PendingAmber
        }
        val label = if (isConfirmed) "Confirmed" else "Pending Review"

        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }

    fun formatCurrency(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "₹%,d".format(Locale.getDefault(), amount.toLong())
        } else {
            "₹%,.2f".format(Locale.getDefault(), amount)
        }
    }

    fun formatTimestamp(epochMillis: Long): String {
        val date = Date(epochMillis)
        val now = System.currentTimeMillis()
        val diffDays = (now - epochMillis) / (1000 * 60 * 60 * 24)

        return when {
            diffDays == 0L -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Today, ${timeFormat.format(date)}"
            }
            diffDays == 1L -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Yesterday, ${timeFormat.format(date)}"
            }
            else -> {
                val fullFormat = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
                fullFormat.format(date)
            }
        }
    }

    fun formatTimeOnly(epochMillis: Long): String {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return timeFormat.format(Date(epochMillis))
    }

    fun formatDateHeader(epochMillis: Long): String {
        val date = Date(epochMillis)
        val now = java.util.Calendar.getInstance()
        val txnCal = java.util.Calendar.getInstance().apply { time = date }

        val isToday = now.get(java.util.Calendar.YEAR) == txnCal.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == txnCal.get(java.util.Calendar.DAY_OF_YEAR)
        if (isToday) return "Today"

        val yesterdayCal = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterdayCal.get(java.util.Calendar.YEAR) == txnCal.get(java.util.Calendar.YEAR) &&
                yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) == txnCal.get(java.util.Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Yesterday"

        val headerFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        return headerFormat.format(date)
    }

    fun formatSourceApp(sourceApp: String): String {
        val lower = sourceApp.lowercase()
        return when {
            lower.contains("paisa") || lower.contains("gpay") || lower.contains("google.android.apps.nbu") -> "Google Pay"
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("messaging") || lower.contains("mms") || lower.contains("sms") -> "SMS"
            lower.contains("axis") -> "Axis Bank"
            lower.contains("hdfc") -> "HDFC Bank"
            lower.contains("icici") -> "ICICI Bank"
            lower.contains("sbi") -> "SBI"
            sourceApp.isBlank() -> "UPI"
            sourceApp.contains(".") -> sourceApp.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            else -> sourceApp
        }
    }
}
