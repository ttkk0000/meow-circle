package com.ttkk0000.meowcircle

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun createPlatformHttpClient(json: Json): HttpClient =
    HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(json)
        }
    }
