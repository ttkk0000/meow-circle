package com.ttkk0000.meowcircle

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

internal expect fun createPlatformHttpClient(json: Json): HttpClient
