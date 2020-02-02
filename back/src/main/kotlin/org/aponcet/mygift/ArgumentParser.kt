package org.aponcet.mygift

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class ArgumentParser(parser: ArgParser) {
    val port : Int by parser.storing(
        "-p", "--port",
        help = "port used by the server") {
        toInt()
    }.default(8080)

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
}