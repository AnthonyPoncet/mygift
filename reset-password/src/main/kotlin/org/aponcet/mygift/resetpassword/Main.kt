package org.aponcet.mygift.resetpassword

import org.aponcet.mygift.dbmanager.DbConnection
import org.aponcet.mygift.dbmanager.ResetPasswordAccessor
import org.aponcet.mygift.dbmanager.UsersAccessor

class Main {

}

fun main(args: Array<String>) {
    val arguments = ArgumentParser.parse(args)
    println("Create reset password for user ${arguments.userName}")

    val dbConnection = DbConnection("sqlite", arguments.dbPath)

    val userId = UsersAccessor(dbConnection).getUser(arguments.userName)?.id ?: throw Exception("Unknown user ${arguments.userName}")

    val resetPasswordAccessor = ResetPasswordAccessor(dbConnection)
    resetPasswordAccessor.createIfNotExists()
    val entry = resetPasswordAccessor.addEntry(userId)
    println("Request have been created: $entry")
}