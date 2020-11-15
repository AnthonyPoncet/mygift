package org.aponcet.mygift.resetpassword

import org.aponcet.mygift.dbmanager.DbConnection
import org.aponcet.mygift.dbmanager.ResetPasswordAccessor
import org.aponcet.mygift.dbmanager.UsersAccessor
import org.aponcet.mygift.model.ConfigurationLoader
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("reset-password")

    val arguments = ArgumentParser.parse(args)

    val configuration = ConfigurationLoader.load(arguments.configurationFile)

    logger.info("Create reset password for user ${arguments.userName}")

    val dbConnection = DbConnection("sqlite", configuration.database.path)

    val userId = UsersAccessor(dbConnection).getUser(arguments.userName)?.id ?: throw Exception("Unknown user ${arguments.userName}")

    val resetPasswordAccessor = ResetPasswordAccessor(dbConnection)
    resetPasswordAccessor.createIfNotExists()
    val entry = resetPasswordAccessor.addEntry(userId)
    logger.info("Request have been created: $entry")
}