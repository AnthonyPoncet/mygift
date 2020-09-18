package org.aponcet.mygift

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class ArgumentParser(parser: ArgParser) {
    val port : Int by parser.storing(
        "-p", "--httpPort",
        help = "http port used by the server") {
        toInt()
    }.default(8080)
    val httpsPort : Int by parser.storing(
        "-w", "--httpsPort",
        help = "https port used by the server") {
        toInt()
    }.default(8081)

    val authServerPort : Int by parser.storing(
        "-s", "--authServerPort",
        help = "port used by the Authentication server") {
        toInt()
    }.default(9876)

    val db : String by parser.storing(
        "-d", "--database",
        help = "path to database")
        .default("mygift.db")

    val resetDB : Boolean by parser.storing(
        "-r", "--resetDB",
        help = "Reset db with some default values") {
            toBoolean()
        }.default(false)

    val adaptTable : String by parser.storing(
        "-a", "--adaptTable",
        help = "Adapt table step X").default("")
    val cleanData : String by parser.storing(
        "-c", "--cleanData",
        help = "Clean data X").default("")

    val debug : Boolean by parser.storing(
        "--debug", help = "Run in debug meaning http instead of https"){
        toBoolean()
    }.default(false)

    /**
     * JKS parameters
     */
    val jks : String by parser.storing(
        "-j", "--jks",
        help = "path to jks file").default("")
    val keyAlias : String by parser.storing(
        "-l", "--keyAlias",
        help = "keyAlias").default("")
    val jksPassword : String by parser.storing(
        "-q", "--jksPassword",
        help = "JKS private password").default("")
    val aliasKey : String by parser.storing(
        "-k", "--aliasPassword",
        help = "Alias password").default("")
}