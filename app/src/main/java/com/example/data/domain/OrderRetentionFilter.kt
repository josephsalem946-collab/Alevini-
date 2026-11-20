package com.example.data.domain

import com.example.data.model.ExchangeOrder
import java.util.Calendar

object OrderRetentionFilter {
    const val RETENTION_MONTHS = 3

    fun threeMonthsCutoffMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.add(Calendar.MONTH, -RETENTION_MONTHS)
        return cal.timeInMillis
    }

    fun isWithinLastThreeMonths(
        createdAtMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (createdAtMillis == null) return true
        return createdAtMillis >= threeMonthsCutoffMillis(nowMillis)
    }

    fun filterRecentOrders(
        orders: List<ExchangeOrder>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ExchangeOrder> {
        val cutoff = threeMonthsCutoffMillis(nowMillis)
        return orders.filter { order ->
            val createdMs = order.createdAt?.toDate()?.time
            createdMs == null || createdMs >= cutoff
        }
    }
}
