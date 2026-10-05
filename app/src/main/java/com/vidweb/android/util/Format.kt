package com.vidweb.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {

    fun pad(value: Int): String = if (value < 10) "0$value" else value.toString()

    fun time(ms: Long): String {
        var value = ms
        if (value < 0) value = 0
        val total = value / 1000
        val h = total / 3600
        val m = (total / 60) % 60
        val s = total % 60
        return "$h:${pad(m.toInt())}:${pad(s.toInt())}"
    }

    fun timePrecise(ms: Long): String {
        var value = ms
        if (value < 0) value = 0
        val total = value
        val s = total / 1000
        val h = s / 3600
        val m = (s / 60) % 60
        val sec = s % 60
        val msec = total % 1000
        return "$h:${pad(m.toInt())}:${pad(sec.toInt())}.${msec.toString().padStart(3, '0')}"
    }

    fun bytes(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = size.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return if (unit == 0) "${value.toLong()} ${units[unit]}"
        else String.format(Locale.US, "%.1f %s", value, units[unit])
    }

    fun relative(secs: Long): String {
        if (secs <= 0) return "从未"
        val diff = System.currentTimeMillis() / 1000 - secs
        if (diff < 60) return "刚刚"
        if (diff < 3600) return "${diff / 60} 分钟前"
        if (diff < 86400) return "${diff / 3600} 小时前"
        if (diff < 86400 * 7) return "${diff / 86400} 天前"
        return dateTime(secs)
    }

    fun dateTime(secs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return fmt.format(Date(secs * 1000))
    }

    fun percent(current: Long, total: Long): Float {
        if (total <= 0) return 0f
        return (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}
