package com.ttkk0000.meowcircle.client

/** Mirrors phoneotp.NormalizePhone/ValidPhone; formatting alone grants no verification. */
fun validLoginPhone(value: String): Boolean = value.count { it in '0'..'9' } in 10..15
fun validAuthCode(value: String): Boolean = value.length == 6 && value.all { it in '0'..'9' }

/** Receipt presentation metadata only; the key never authorizes a request. */
data class OtpDelivery(val key: String, val expiresAtMillis: Long, val expired: Boolean)
