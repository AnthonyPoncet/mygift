package org.aponcet.authserver

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

data class Args(val configurationFile: String)

class ArgumentParser {
    companion object {
        fun parse(args: Array<String>): Args {
            val parser = ArgParser("auth-server")
            val configurationFile by parser.option(ArgType.String, shortName = "c", description = "configuration file").default("configuration.json")
            parser.parse(args)
            return Args(configurationFile)
        }
    }
}