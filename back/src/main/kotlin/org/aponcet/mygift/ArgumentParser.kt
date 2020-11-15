package org.aponcet.mygift

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.aponcet.mygift.dbmanager.maintenance.AdaptTable
import org.aponcet.mygift.dbmanager.maintenance.CleanDataNotUsed

data class Args(val configurationFile: String, val adaptTable: AdaptTable.STEP?, val cleanData: CleanDataNotUsed.DATA?)

class ArgumentParser {
    companion object {
        fun parse(args: Array<String>): Args {
            val parser = ArgParser("reset-password")
            val configurationFile by parser.option(ArgType.String, shortName = "c", description = "configuration file").default("configuration.json")
            val adaptTable by parser.option(ArgType.String, shortName = "a", description = "Adapt table step X").default("")
            val cleanData by parser.option(ArgType.String, shortName = "d", description = "Clean data X").default("")
            parser.parse(args)
            return Args(
                configurationFile,
                if (adaptTable.isEmpty()) null else AdaptTable.STEP.valueOf(adaptTable),
                if (cleanData.isEmpty()) null else CleanDataNotUsed.DATA.valueOf(cleanData))
        }
    }
}