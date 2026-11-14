package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class MonthYear(val year: Int, val month: Int) {
    fun key(): String = String.format(Locale.US, "%04d-%02d", year, month)

    fun displayLabel(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
    }

    fun plusMonths(delta: Int): MonthYear {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        return MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    companion object {
        fun current(): MonthYear {
            val cal = Calendar.getInstance()
            return MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }

        fun fromMillis(millis: Long): MonthYear {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            return MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}

object DateUtils {
    fun dayMillis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun dayStartKey(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDateHeader(millis: Long): String {
        val fmt = SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH)
        return fmt.format(Date(millis))
    }

    fun formatIsoDate(millis: Long): String {
        val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        return fmt.format(Date(millis))
    }
}

/**
 * Formats an amount in Indian Rupees (₹) using the Indian numbering system
 * (e.g., ₹1,23,456.00).
 */
fun formatCurrency(amount: Double, hidden: Boolean = false): String {
    if (hidden) return "₹ ••••"
    val negative = amount < 0.0
    val roundedPaise = round(abs(amount) * 100.0).toLong()
    val integerPart = roundedPaise / 100
    val decimalPart = roundedPaise % 100

    val intStr = integerPart.toString()
    val groupedInt = if (intStr.length <= 3) {
        intStr
    } else {
        val lastThree = intStr.takeLast(3)
        val prefixDigits = intStr.dropLast(3)
        val groupedPrefix = prefixDigits
            .reversed()
            .chunked(2)
            .joinToString(",")
            .reversed()
        "$groupedPrefix,$lastThree"
    }
    val decStr = String.format(Locale.US, "%02d", decimalPart)
    val sign = if (negative) "-" else ""
    return "${sign}₹$groupedInt.$decStr"
}
