package com.example

import com.example.misc.configureDatabases
import com.example.misc.configureMonitoring
import com.example.misc.configureSerialization
import com.example.routes.configureRouting
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
//    io.ktor.server.netty.EngineMain.main(args)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting()
}
