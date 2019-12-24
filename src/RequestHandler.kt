package net.toomuchram

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText

class RequestHandler(private val userManager: UserManager) {


    suspend fun registerUser(call: ApplicationCall) {

        val postParameters: Parameters = call.receiveParameters()
        if (
            !postParameters.contains("username") ||
                    !postParameters.contains("password") ||
                    !postParameters.contains("emailaddress")
        ) {
            call.respond(HttpStatusCode.BadRequest, "ERR_MISSING_VARIABLES")
            return
        }

        val username = postParameters["username"]
        val password = postParameters["password"]
        val emailaddress = postParameters["emailaddress"]
        if (username == null || username == "") {
            call.respond(HttpStatusCode.BadRequest, "ERR_NO_USERNAME")
            return
        } else if (password == null || password == "") {
            call.respond(HttpStatusCode.BadRequest, "ERR_NO_PASSWORD")
            return
        } else if (emailaddress == null || emailaddress == "") {
            call.respond(HttpStatusCode.BadRequest, "ERR_NO_PASSWORD")
            return
        }

        try {
            userManager.register(
                UserManager.User(
                    username,
                    password,
                    emailaddress
                )
            )
            call.respond(HttpStatusCode.OK, "SUCCESS")
        } catch(e: UsernameTakenException) {
            call.respond(HttpStatusCode.Forbidden, "ERR_USERNAME_TAKEN")
        } catch (e: EmailAddressTakenException) {
            call.respond(HttpStatusCode.Forbidden, "ERR_EMAIL_ADDRESS_TAKEN")
        }
    }

    suspend fun loginUser(call: ApplicationCall) {
        val postParameters: Parameters = call.receiveParameters()
        if (
            !postParameters.contains("username") ||
            !postParameters.contains("password")
        ) {
            call.respond(HttpStatusCode.BadRequest, "ERR_MISSING_VARIABLES")
            return
        }

        val username = postParameters["username"]
        val password = postParameters["password"]
        if (username == null || username == "") {
            call.respond(HttpStatusCode.BadRequest, "ERR_NO_USERNAME")
            return
        } else if (password == null || password == "") {
            call.respond(HttpStatusCode.BadRequest, "ERR_NO_PASSWORD")
            return
        }

        try {
            val uid = userManager.login(
                UserManager.User(
                    username,
                    password,
                    "" /* No need to pass an email address since we're just logging in */
                )
            )
            call.request.header("Content-type: application/json")
            call.respond(uid)
        } catch(e: NoSuchUserException) {
            call.respond(HttpStatusCode.Unauthorized, "ERR_NO_SUCH_USER")
        } catch (e: WrongPasswordException) {
            call.respond(HttpStatusCode.Unauthorized, "ERR_WRONG_PASSWORD")
        }
    }
}