package org.aponcet.mygift.resetpassword

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

data class Args(val userName: String, val dbPath: String)

class ArgumentParser {
    companion object {
        fun parse(args: Array<String>): Args {
            val parser = ArgParser("reset-password")
            val dbPath by parser.option(ArgType.String, shortName = "d", description = "database path").default("mygift.db")
            val userName by parser.option(ArgType.String, shortName = "u", description = "user name").required()
            parser.parse(args)
            return Args(userName, dbPath)
        }
    }
}