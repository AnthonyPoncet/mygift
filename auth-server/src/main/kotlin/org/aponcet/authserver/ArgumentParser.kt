package org.aponcet.authserver

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class ArgumentParser(parser: ArgParser) {
    val port : Int by parser.storing(
        "-p", "--port",
        help = "port used by the server") {
        toInt()
    }.default(9876)

    val db : String by parser.storing(
        "-d", "--database",
        help = "path to database")
        .default("mygift.db")
}