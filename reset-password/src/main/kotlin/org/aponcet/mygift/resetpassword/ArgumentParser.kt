package org.aponcet.mygift.resetpassword

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

data class Args(val userName: String, val configurationFile: String)

class ArgumentParser {
    companion object {
        fun parse(args: Array<String>): Args {
            val parser = ArgParser("reset-password")
            val configurationFile by parser.option(ArgType.String, shortName = "c", description = "configuration file")
                .default("configuration.json")
            val userName by parser.option(ArgType.String, shortName = "u", description = "user name").required()
            parser.parse(args)
            return Args(userName, configurationFile)
        }
    }
}