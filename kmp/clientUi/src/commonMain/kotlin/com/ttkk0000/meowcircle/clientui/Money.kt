package com.ttkk0000.meowcircle.clientui

/** 金额只在整数分上计算，不经Float／Double或平台BigDecimal丢失精度。 */
fun centsText(value: Long): String {
    val source = value.toString()
    val sign = if (value < 0) "-" else ""
    val digits = source.removePrefix("-").padStart(3, '0')
    return sign + digits.dropLast(2) + "." + digits.takeLast(2)
}
fun money(cents: Long): String = centsText(cents) + " 元"
fun cents(value: String): Long? {
    val source = value.trim()
    if (!Regex("^[+]?[0-9]+(?:\\.[0-9]{0,2})?$").matches(source)) return null
    val pieces = source.removePrefix("+").split('.')
    val whole = pieces[0].toLongOrNull()?.takeIf { it <= 1_000_000 } ?: return null
    val fraction = pieces.getOrElse(1) { "" }.padEnd(2, '0').toLong()
    return (whole * 100 + fraction).takeIf { it in 0..100_000_000 }
}
