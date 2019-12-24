package net.toomuchram

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.lang.Exception

class UserManager(private val db: Database) {
    data class User(
        val username: String,
        val password: String,
        val emailaddress: String
    )

    private object Users: IntIdTable() {
        val username: Column<String> = varchar("username", 50)
        val password: Column<String> = varchar("password", 255)
        val emailaddress: Column<String> = varchar("emailaddress", 255)
    }

    fun login(user: User): Int {

        return transaction {
            val matchingusers = Users.slice(Users.password, Users.id).select {
                Users.username eq user.username
            }
            when {
                matchingusers.count() == 1 -> {
                    var pwHash = ""
                    var userId = 1
                    for(matchinguser in matchingusers) {
                        pwHash = matchinguser[Users.password]
                        userId = matchinguser[Users.id].value
                    }
                    if(BCryptPasswordEncoder().matches(user.password, pwHash)) {
                        return@transaction userId
                    } else {
                        throw WrongPasswordException("Wrong password")
                    }
                }
                matchingusers.count() < 1 -> {
                    throw NoSuchUserException("No such user found")
                }
                else -> {
                    throw Exception("More than one user found")
                }
            }
        }


    }


    fun register(user: User): Int {

        // Check if username and password are already taken
        transaction {
            val query: Query = Users.select {
                Users.username eq user.username
            }
            if (query.count() > 0) {
                throw UsernameTakenException("Username has already been taken")
            }
        }

        transaction {
            val query: Query = Users.select {
                Users.emailaddress eq user.emailaddress
            }
            if (query.count() > 0) {
                throw EmailAddressTakenException("Email address has already been taken")
            }
        }

        return transaction {
            return@transaction Users.insertAndGetId {
                it[username] = user.username
                it[password] = BCryptPasswordEncoder().encode(user.password)
                it[emailaddress] = user.emailaddress
            }
        }.value
    }
}
class UsernameTakenException(message: String): Exception(message)
class EmailAddressTakenException(message: String): Exception(message)
class NoSuchUserException(message: String): Exception(message)
class WrongPasswordException(message: String): Exception(message)