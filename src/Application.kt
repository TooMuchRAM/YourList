package net.toomuchram

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.text.DateFormat

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    if (testing) {
        val path = System.getProperty("user.dir")
        println("Working Directory: $path")
    }

    val db = initialiseDatabase()

    val userManager = UserManager(db)
    val requestHandler = RequestHandler(userManager)

    install(Sessions) {
        cookie<UserSession>("SESSION") {
        }
    }

    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }


    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Authentication) {
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        post("/register") {
            requestHandler.registerUser(call)
        }
        post("/login") {
            requestHandler.loginUser(call)
        }

        webSocket("/myws/echo") {
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }
    }
}

fun initialiseDatabase(): Database {
    val mysqlUsername = Key("mysql.username", stringType)
    val mysqlPassword = Key("mysql.password", stringType)
    val mysqlHost = Key("mysql.host", stringType)
    val mysqlDatabase = Key("mysql.database", stringType)

    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromFile(File("config.properties"))

    return Database.connect(
        "jdbc:mysql://${config[mysqlHost]}/${config[mysqlDatabase]}",
        driver = "com.mysql.jdbc.Driver",
        user = config[mysqlUsername], password = config[mysqlPassword]
    )
}

data class UserSession(
    val userId: Int,
    val username: String
)

