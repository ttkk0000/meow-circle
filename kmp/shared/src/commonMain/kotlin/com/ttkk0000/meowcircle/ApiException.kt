package com.ttkk0000.meowcircle

class ApiException(
    val statusCode: Int,
    message: String,
) : Exception(message)
